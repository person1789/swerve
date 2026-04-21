import { useEffect, useRef, useState, useCallback } from 'react';
import type { TelemetryFrame, ConnectionStatus, RuntimeMode, StructuredLogEntry } from '../types/telemetry';
import {
  HeadingController,
  ModuleEmulator,
  MotionSmoother,
  Odometry,
  SwerveAuditor,
  SwerveConfig,
  SwerveKinematics,
} from '../lib/SwerveLogic';

export interface SITLHook {
  status: ConnectionStatus;
  runtimeMode: RuntimeMode;
  lastFrame: TelemetryFrame | null;
  schemaMismatch: string | null;
  sendInput: (input: GamepadInput) => void;
  resetSimulation: () => void;
}

export interface GamepadInput {
  drive: number;
  strafe: number;
  turn: number;
  dpad_up?: boolean;
  dpad_down?: boolean;
  dpad_left?: boolean;
  dpad_right?: boolean;
  options?: boolean;
}

interface LocalSimState {
  kinematics: SwerveKinematics;
  smoother: MotionSmoother;
  heading: HeadingController;
  odometry: Odometry;
  modules: ModuleEmulator[];
  timestamp: number;
}

export interface ParsedFrameResult {
  frame: TelemetryFrame;
  warnings: string[];
}

function normalizeModuleStates(value: unknown): [number, number][] {
  if (!Array.isArray(value)) return [[0, 0], [0, 0], [0, 0], [0, 0]];
  return new Array(4).fill(null).map((_, index) => {
    const pair = value[index];
    if (!Array.isArray(pair)) return [0, 0];
    return [
      Number(pair[0] ?? 0),
      Number(pair[1] ?? 0),
    ] as [number, number];
  });
}

function normalizeLogs(value: unknown): StructuredLogEntry[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap(item => {
    if (!item || typeof item !== 'object') return [];
    const candidate = item as Record<string, unknown>;
    const severity = candidate.severity;
    if (severity !== 'info' && severity !== 'warn' && severity !== 'error' && severity !== 'debug') return [];
    return [{
      timestamp: Number(candidate.timestamp ?? 0),
      severity,
      source: String(candidate.source ?? 'SITL'),
      message: String(candidate.message ?? ''),
    }];
  });
}

export function parseTelemetryFrame(raw: unknown): ParsedFrameResult {
  const candidate = (raw && typeof raw === 'object' ? raw : {}) as Record<string, unknown>;
  const warnings: string[] = [];

  if (!Array.isArray(candidate.targets)) warnings.push('targets');
  if (!Array.isArray(candidate.actuals)) warnings.push('actuals');
  if (candidate.currentDraw !== undefined && !Array.isArray(candidate.currentDraw)) warnings.push('currentDraw');
  if (candidate.logs !== undefined && !Array.isArray(candidate.logs)) warnings.push('logs');

  const schemaVersion = Number(candidate.schemaVersion ?? 1);
  const frame: TelemetryFrame = {
    schemaVersion,
    timestamp: Number(candidate.timestamp ?? 0),
    x: Number(candidate.x ?? 0),
    y: Number(candidate.y ?? 0),
    heading: Number(candidate.heading ?? 0),
    targets: normalizeModuleStates(candidate.targets),
    actuals: normalizeModuleStates(candidate.actuals),
    isMaintaining: Boolean(candidate.isMaintaining ?? false),
    isSnapping: Boolean(candidate.isSnapping ?? false),
    snapTargetRad: Number(candidate.snapTargetRad ?? 0),
    controllerMode: candidate.controllerMode === 'manual' || candidate.controllerMode === 'snap' || candidate.controllerMode === 'maintain'
      ? candidate.controllerMode
      : undefined,
    drivetrainState: typeof candidate.drivetrainState === 'string' ? candidate.drivetrainState : undefined,
    driveX: Number(candidate.driveX ?? (candidate.gamepad as any)?.lx ?? 0),
    driveY: Number(candidate.driveY ?? (candidate.gamepad as any)?.ly ?? 0),
    turn: Number(candidate.turn ?? (candidate.gamepad as any)?.rx ?? 0),
    gamepad: candidate.gamepad && typeof candidate.gamepad === 'object'
      ? {
          lx: Number((candidate.gamepad as any).lx ?? 0),
          ly: Number((candidate.gamepad as any).ly ?? 0),
          rx: Number((candidate.gamepad as any).rx ?? 0),
          ry: Number((candidate.gamepad as any).ry ?? 0),
          lt: Number((candidate.gamepad as any).lt ?? 0),
          rt: Number((candidate.gamepad as any).rt ?? 0),
          buttons: Array.isArray((candidate.gamepad as any).buttons)
            ? (candidate.gamepad as any).buttons.map((value: unknown) => Boolean(value))
            : undefined,
          dpad_up: Boolean((candidate.gamepad as any).dpad_up),
          dpad_down: Boolean((candidate.gamepad as any).dpad_down),
          dpad_left: Boolean((candidate.gamepad as any).dpad_left),
          dpad_right: Boolean((candidate.gamepad as any).dpad_right),
        }
      : undefined,
    batteryVoltage: Number(candidate.batteryVoltage ?? 12.0),
    loopTimeMs: Number(candidate.loopTimeMs ?? 20),
    currentDraw: Array.isArray(candidate.currentDraw)
      ? candidate.currentDraw.map(value => Number(value ?? 0))
      : [],
    observerVel: candidate.observerVel && typeof candidate.observerVel === 'object'
      ? {
          vx: Number((candidate.observerVel as any).vx ?? 0),
          vy: Number((candidate.observerVel as any).vy ?? 0),
          omega: Number((candidate.observerVel as any).omega ?? 0),
        }
      : undefined,
    smootherState: candidate.smootherState && typeof candidate.smootherState === 'object'
      ? {
          vx: Number((candidate.smootherState as any).vx ?? 0),
          vy: Number((candidate.smootherState as any).vy ?? 0),
          omega: Number((candidate.smootherState as any).omega ?? 0),
          ax: Number((candidate.smootherState as any).ax ?? 0),
          ay: Number((candidate.smootherState as any).ay ?? 0),
          alpha: Number((candidate.smootherState as any).alpha ?? 0),
        }
      : undefined,
    logs: normalizeLogs(candidate.logs),
  };

  return { frame, warnings };
}

function createLocalSimState(): LocalSimState {
  return {
    kinematics: new SwerveKinematics(),
    smoother: new MotionSmoother(),
    heading: new HeadingController(),
    odometry: new Odometry(),
    modules: [0, 1, 2, 3].map(() => new ModuleEmulator()),
    timestamp: 0,
  };
}

function stepLocalSim(state: LocalSimState, input: GamepadInput, dt: number): TelemetryFrame {
  if (input.dpad_up) state.heading.snap(0);
  if (input.dpad_left) state.heading.snap(Math.PI / 2);
  if (input.dpad_down) state.heading.snap(Math.PI);
  if (input.dpad_right) state.heading.snap(-Math.PI / 2);

  const driveX = input.strafe;
  const driveY = input.drive;
  const rawTurn = input.turn;
  const isMoving = Math.hypot(driveX, driveY) > 0.05;
  const omegaCmd = state.heading.update(state.odometry.heading, rawTurn, isMoving, dt);
  const driverTarget = {
    vx: driveX * SwerveConfig.MAX_SPEED_MPS,
    vy: driveY * SwerveConfig.MAX_SPEED_MPS,
    heading: omegaCmd,
  };
  const smoothed = state.smoother.calculate(driverTarget, driverTarget, dt);
  const desiredStates = state.kinematics.toModuleStates(smoothed.vx, smoothed.vy, smoothed.heading, dt);
  const optimizedStates = SwerveAuditor.optimize(
    desiredStates,
    state.modules.map(module => module.angleRad),
    SwerveConfig.MAX_SPEED_MPS,
  );

  state.modules.forEach((module, index) => module.update(optimizedStates[index], dt));
  const actualStates = state.modules.map(module => module.getState());
  const observerVel = state.kinematics.toChassisSpeeds(actualStates);
  state.odometry.update(observerVel, dt);
  state.timestamp += dt;

  const currentDraw = state.modules.map(module => module.currentA);
  const totalCurrent = currentDraw.reduce((sum, amps) => sum + amps, 1.2);
  const batteryVoltage = Math.max(11.1, 12.8 - totalCurrent * 0.018);
  const headingState = state.heading.getState();
  const smootherAccel = state.smoother.getAccel();
  const logs: StructuredLogEntry[] = [];
  if (batteryVoltage < 11.7) {
    logs.push({
      timestamp: state.timestamp,
      severity: 'warn',
      source: 'LOCAL_SIM',
      message: `Voltage sag ${batteryVoltage.toFixed(2)}V under ${totalCurrent.toFixed(1)}A load`,
    });
  }

  return {
    schemaVersion: 1,
    timestamp: state.timestamp,
    x: state.odometry.x,
    y: state.odometry.y,
    heading: state.odometry.heading,
    targets: optimizedStates.map(module => [module.speedMps, module.angleRad]),
    actuals: actualStates.map(module => [module.speedMps, module.angleRad]),
    isMaintaining: headingState.maintaining,
    isSnapping: headingState.snapping,
    snapTargetRad: headingState.target,
    controllerMode: headingState.snapping ? 'snap' : headingState.maintaining ? 'maintain' : 'manual',
    drivetrainState: headingState.snapping ? 'SNAPPING' : isMoving ? 'DRIVING' : 'IDLE',
    driveX,
    driveY,
    turn: rawTurn,
    gamepad: {
      lx: driveX,
      ly: driveY,
      rx: rawTurn,
      ry: 0,
      lt: 0,
      rt: 0,
      dpad_up: !!input.dpad_up,
      dpad_down: !!input.dpad_down,
      dpad_left: !!input.dpad_left,
      dpad_right: !!input.dpad_right,
    },
    batteryVoltage,
    loopTimeMs: dt * 1000,
    currentDraw,
    observerVel,
    smootherState: {
      vx: smoothed.vx,
      vy: smoothed.vy,
      omega: smoothed.heading,
      ax: smootherAccel.vx,
      ay: smootherAccel.vy,
      alpha: smootherAccel.heading,
    },
    logs,
  };
}

export function useSITL(onFrame: (frame: TelemetryFrame) => void): SITLHook {
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const [runtimeMode, setRuntimeMode] = useState<RuntimeMode>('local');
  const [lastFrame, setLastFrame] = useState<TelemetryFrame | null>(null);
  const [schemaMismatch, setSchemaMismatch] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<GamepadInput>({ drive: 0, strafe: 0, turn: 0 });
  const localSimRef = useRef<LocalSimState>(createLocalSimState());
  const warningRef = useRef<string | null>(null);

  const connect = useCallback(() => {
    if (socketRef.current?.readyState === WebSocket.OPEN || socketRef.current?.readyState === WebSocket.CONNECTING) {
      return;
    }

    setStatus('connecting');
    const ws = new WebSocket('ws://localhost:8080');
    socketRef.current = ws;
    (window as any).sitlSocket = ws;

    ws.onopen = () => {
      setStatus('connected');
      setRuntimeMode('sitl');
      setSchemaMismatch(null);
      warningRef.current = null;
    };

    ws.onmessage = event => {
      try {
        const raw = JSON.parse(event.data);
        const { frame, warnings } = parseTelemetryFrame(raw);
        const mismatch = frame.schemaVersion > 1
          ? `Renderer expects telemetry schema v1 but received v${frame.schemaVersion}.`
          : warnings.length > 0
            ? `Telemetry normalized missing/invalid fields: ${warnings.join(', ')}`
            : null;
        if (mismatch !== warningRef.current) {
          warningRef.current = mismatch;
          setSchemaMismatch(mismatch);
        }
        setLastFrame(frame);
        onFrame(frame);
      } catch {
        setSchemaMismatch('Received malformed telemetry frame JSON.');
      }
    };

    ws.onclose = () => {
      setStatus('disconnected');
      setRuntimeMode('local');
      socketRef.current = null;
      reconnectRef.current = setTimeout(connect, 2000);
    };

    ws.onerror = () => ws.close();
  }, [onFrame]);

  useEffect(() => {
    connect();
    return () => {
      if (reconnectRef.current) clearTimeout(reconnectRef.current);
      socketRef.current?.close();
    };
  }, [connect]);

  useEffect(() => {
    if (runtimeMode !== 'local') return;

    let lastTs = performance.now();
    const timer = window.setInterval(() => {
      const now = performance.now();
      const elapsed = Math.max(SwerveConfig.LOOP_TIME_SEC, (now - lastTs) / 1000);
      lastTs = now;
      const frame = stepLocalSim(localSimRef.current, inputRef.current, Math.min(elapsed, 0.05));
      setLastFrame(frame);
      onFrame(frame);
    }, SwerveConfig.LOOP_TIME_SEC * 1000);
    return () => {
      window.clearInterval(timer);
    };
  }, [runtimeMode, onFrame]);

  useEffect(() => {
    const pressed = new Set<string>();

    const updateFromKeyboard = () => {
      inputRef.current = {
        ...inputRef.current,
        drive: (pressed.has('KeyW') ? 1 : 0) + (pressed.has('KeyS') ? -1 : 0),
        strafe: (pressed.has('KeyD') ? 1 : 0) + (pressed.has('KeyA') ? -1 : 0),
        turn: (pressed.has('KeyE') ? 1 : 0) + (pressed.has('KeyQ') ? -1 : 0),
        dpad_up: pressed.has('ArrowUp'),
        dpad_down: pressed.has('ArrowDown'),
        dpad_left: pressed.has('ArrowLeft'),
        dpad_right: pressed.has('ArrowRight'),
      };
    };

    const keydown = (event: KeyboardEvent) => {
      if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.target instanceof HTMLSelectElement) {
        return;
      }
      const relevant = ['KeyW', 'KeyA', 'KeyS', 'KeyD', 'KeyQ', 'KeyE', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'];
      if (!relevant.includes(event.code)) return;
      pressed.add(event.code);
      updateFromKeyboard();
    };

    const keyup = (event: KeyboardEvent) => {
      if (!pressed.has(event.code)) return;
      pressed.delete(event.code);
      updateFromKeyboard();
    };

    window.addEventListener('keydown', keydown);
    window.addEventListener('keyup', keyup);
    return () => {
      window.removeEventListener('keydown', keydown);
      window.removeEventListener('keyup', keyup);
    };
  }, []);

  const sendInput = useCallback((input: GamepadInput) => {
    inputRef.current = input;
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(input));
    }
  }, []);

  const resetSimulation = useCallback(() => {
    localSimRef.current = createLocalSimState();
    inputRef.current = { drive: 0, strafe: 0, turn: 0 };
    setLastFrame(null);
    if (runtimeMode === 'local') {
      setSchemaMismatch(null);
    }
  }, [runtimeMode]);

  return { status, runtimeMode, lastFrame, schemaMismatch, sendInput, resetSimulation };
}
