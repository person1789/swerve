// useHistory.ts — ring-buffer telemetry history with playback control
import { useRef, useState, useCallback } from 'react';
import type { TelemetryFrame } from '../types/telemetry';

const MAX_FRAMES = 36_000; // ~12 min @ 50 Hz
const UI_UPDATE_MS = 50;

export function useHistory() {
  const bufferRef = useRef<TelemetryFrame[]>([]);
  const lastUiUpdateRef = useRef(0);
  const [length, setLength] = useState(0);
  const [scrubIndex, setScrubIndex] = useState(0);
  const [isLive, setIsLive] = useState(true);

  const push = useCallback((frame: TelemetryFrame) => {
    const buf = bufferRef.current;
    buf.push(frame);
    if (buf.length > MAX_FRAMES) buf.shift();
    const now = performance.now();
    const shouldRefreshUi = now - lastUiUpdateRef.current >= UI_UPDATE_MS;

    if (shouldRefreshUi) {
      lastUiUpdateRef.current = now;
      setLength(buf.length);
      if (isLive) setScrubIndex(buf.length - 1);
    }
  }, [isLive]);

  const seek = useCallback((idx: number) => {
    const clamped = Math.max(0, Math.min(idx, bufferRef.current.length - 1));
    setScrubIndex(clamped);
    setIsLive(false);
  }, []);

  const stepBy = useCallback((delta: number) => {
    setScrubIndex(prev => {
      const clamped = Math.max(0, Math.min(prev + delta, bufferRef.current.length - 1));
      return clamped;
    });
    setIsLive(false);
  }, []);

  const snapToLive = useCallback(() => {
    setLength(bufferRef.current.length);
    setScrubIndex(bufferRef.current.length - 1);
    setIsLive(true);
  }, []);

  const clear = useCallback(() => {
    bufferRef.current = [];
    setLength(0);
    setScrubIndex(0);
    setIsLive(true);
  }, []);

  const getFrame = useCallback((idx: number): TelemetryFrame | null => {
    return bufferRef.current[idx] ?? null;
  }, []);

  const getBuffer = useCallback(() => bufferRef.current, []);

  const currentFrame: TelemetryFrame | null = bufferRef.current[scrubIndex] ?? null;

  return {
    push, seek, stepBy, snapToLive, clear, getFrame, getBuffer,
    length, scrubIndex, isLive, currentFrame,
  };
}
