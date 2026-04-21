// useSITL.ts — manages WebSocket connection to Java simulation brain
import { useEffect, useRef, useState, useCallback } from 'react';
import type { TelemetryFrame, ConnectionStatus } from '../types/telemetry';

export interface SITLHook {
  status: ConnectionStatus;
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

export function useSITL(
  onFrame: (frame: TelemetryFrame) => void,
): SITLHook {
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const [lastFrame, setLastFrame] = useState<TelemetryFrame | null>(null);
  const [schemaMismatch, setSchemaMismatch] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const connect = useCallback(() => {
    if (socketRef.current?.readyState === WebSocket.OPEN) return;

    setStatus('connecting');
    const ws = new WebSocket('ws://localhost:8080');
    socketRef.current = ws;
    (window as any).sitlSocket = ws;

    ws.onopen = () => {
      setStatus('connected');
      setSchemaMismatch(null);
    };

    ws.onmessage = (event) => {
      try {
        const raw = JSON.parse(event.data);
        const schemaVersion = Number(raw.schemaVersion ?? 1);
        if (schemaVersion > 1) {
          setSchemaMismatch(`Renderer expects telemetry schema v1 but received v${schemaVersion}.`);
        }
        const frame: TelemetryFrame = {
          schemaVersion,
          timestamp: raw.timestamp ?? 0,
          x: raw.x ?? 0,
          y: raw.y ?? 0,
          heading: raw.heading ?? 0,
          targets: raw.targets ?? [[0,0],[0,0],[0,0],[0,0]],
          actuals: raw.actuals ?? [[0,0],[0,0],[0,0],[0,0]],
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
        setLastFrame(frame);
        onFrame(frame);
      } catch { /* skip malformed */ }
    };

    ws.onclose = () => {
      setStatus('disconnected');
      socketRef.current = null;
      // Auto-reconnect after 2s
      reconnectRef.current = setTimeout(connect, 2000);
    };

    ws.onerror = () => ws.close();
  }, [onFrame]);

  useEffect(() => {
    connect();
    return () => {
      reconnectRef.current && clearTimeout(reconnectRef.current);
      socketRef.current?.close();
    };
  }, [connect]);

  const sendInput = useCallback((input: GamepadInput) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(input));
    }
  }, []);

  return { status, lastFrame, schemaMismatch, sendInput };
}
