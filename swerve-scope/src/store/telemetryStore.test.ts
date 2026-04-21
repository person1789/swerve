import { beforeEach, describe, expect, it } from 'vitest';
import type { TelemetryFrame } from '../types/telemetry';
import { telemetryStore } from './telemetryStore';

function makeFrame(timestamp: number, x = timestamp): TelemetryFrame {
  return {
    schemaVersion: 1,
    timestamp,
    x,
    y: timestamp / 2,
    heading: 0,
    targets: [[0, 0], [0, 0], [0, 0], [0, 0]],
    actuals: [[0, 0], [0, 0], [0, 0], [0, 0]],
    isMaintaining: false,
    isSnapping: false,
    snapTargetRad: 0,
    driveX: 0,
    driveY: 0,
    turn: 0,
    batteryVoltage: 12.5,
    loopTimeMs: 20,
  };
}

describe('telemetryStore', () => {
  beforeEach(() => {
    telemetryStore.clear();
  });

  it('replaces history and restores scrubbed playback state', () => {
    const frames = [makeFrame(0), makeFrame(0.02), makeFrame(0.04)];

    telemetryStore.replaceHistory(frames, { isLive: false, scrubIndex: 1 });

    expect(telemetryStore.getLength()).toBe(3);
    expect(telemetryStore.getIsLive()).toBe(false);
    expect(telemetryStore.getScrubIndex()).toBe(1);
    expect(telemetryStore.getFrame()?.timestamp).toBe(0.02);
    expect(telemetryStore.getPrevFrame()?.timestamp).toBe(0);
  });

  it('snaps imported live sessions to the tail', () => {
    const frames = [makeFrame(0), makeFrame(0.02), makeFrame(0.04)];

    telemetryStore.replaceHistory(frames, { isLive: true, scrubIndex: 0 });

    expect(telemetryStore.getIsLive()).toBe(true);
    expect(telemetryStore.getScrubIndex()).toBe(2);
    expect(telemetryStore.getFrame()?.timestamp).toBe(0.04);
  });

  it('keeps a trail for imported sessions', () => {
    const frames = [makeFrame(0, 0), makeFrame(0.02, 1), makeFrame(0.04, 2)];

    telemetryStore.replaceHistory(frames, { isLive: false, scrubIndex: 2 });

    expect(telemetryStore.getTrail()).toHaveLength(3);
    expect(telemetryStore.getTrail()[2]).toEqual({ x: 2, y: 0.02 });
  });
});
