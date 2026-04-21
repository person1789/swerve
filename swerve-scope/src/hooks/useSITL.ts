import { useEffect, useRef, useState, useCallback } from 'react';
import type { TelemetryFrame, ConnectionStatus, RuntimeMode } from '../types/telemetry';
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

function parseFrame(raw: any): TelemetryFrame {
  const schemaVersion = Number(raw.schemaVersion ?? 1);
  return {
    schemaVersion,
    timestamp: raw.timestamp ?? 0,
    x: raw.x ?? 0,
    y: raw.y ?? 0,
    heading: raw.heading ?? 0,
    targets: raw.targets ?? [[0, 0], [0, 0], [0, 0], [0, 0]],
    actuals: raw.actuals ?? [[0, 0], [0, 0], [0, 0], [0, 0]],
    isMaintaining: raw.isMaintaining ?? false,
    isSnapping: raw.isSnapping ?? false,
    snapTargetRad: raw.snapTargetRad ?? 0,
    controllerMode: raw.controllerMode,
    drivetrainState: raw.drivetrainState,
    driveX: raw.driveX ?? raw.gamepad?.lx ?? 0,
    driveY: raw.driveY ?? raw.gamepad?.ly ?? 0,
    turn: raw.turn ?? raw.gamepad?.rx ?? 0,
    gamepad: raw.gamepad,
    batteryVoltage: raw.batteryVoltage ?? 12.0,
    loopTimeMs: raw.loopTimeMs ?? 20,
    currentDraw: raw.currentDraw ?? [],
    observerVel: raw.observerVel,
    smootherState: raw.smootherState,
    logs: raw.logs ?? [],
  };
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
    logs: [],
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
    };

    ws.onmessage = event => {
      try {
        const raw = JSON.parse(event.data);
        const schemaVersion = Number(raw.schemaVersion ?? 1);
        if (schemaVersion > 1) {
          setSchemaMismatch(`Renderer expects telemetry schema v1 but received v${schemaVersion}.`);
        }
        const frame = parseFrame(raw);
        setLastFrame(frame);
        onFrame(frame);
      } catch {
        // Ignore malformed frames.
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

  return { status, runtimeMode, lastFrame, schemaMismatch, sendInput };
}
