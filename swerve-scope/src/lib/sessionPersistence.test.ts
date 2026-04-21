import { describe, expect, it } from 'vitest';
import { createSessionFile, parseSessionFile, serializeSessionFile } from './sessionPersistence';
import type { TelemetryFrame, WorkspaceLayout } from '../types/telemetry';

function makeFrame(timestamp: number): TelemetryFrame {
  return {
    schemaVersion: 1,
    timestamp,
    x: timestamp,
    y: timestamp + 1,
    heading: 0,
    targets: [[0, 0], [0, 0], [0, 0], [0, 0]],
    actuals: [[0, 0], [0, 0], [0, 0], [0, 0]],
    isMaintaining: false,
    isSnapping: false,
    snapTargetRad: 0,
    driveX: 0,
    driveY: 0,
    turn: 0,
    batteryVoltage: 12.4,
    loopTimeMs: 20,
  };
}

const workspace: WorkspaceLayout = {
  leftPct: 62,
  slotHeights: {
    primary: 58,
    secondary: 42,
    sidebarTop: 34,
    sidebarMiddle: 33,
    sidebarBottom: 33,
  },
  slots: {
    primary: 'arena',
    secondary: 'graph',
    sidebarTop: 'detailer',
    sidebarMiddle: 'inspector',
    sidebarBottom: 'console',
  },
};

describe('sessionPersistence', () => {
  it('serializes and parses a valid session file', () => {
    const json = serializeSessionFile({
      frames: [makeFrame(0), makeFrame(0.02)],
      activeFields: ['speed'],
      customFields: [],
      workspace,
      playback: { isLive: false, scrubIndex: 1 },
      ui: { showVectors: true, showTrail: false },
      analysis: { range: { startIndex: 0, endIndex: 1 }, bookmarks: [1] },
    });

    const parsed = parseSessionFile(json);

    expect(parsed.app).toBe('SwerveScope');
    expect(parsed.version).toBe(1);
    expect(parsed.frames).toHaveLength(2);
    expect(parsed.playback.scrubIndex).toBe(1);
    expect(parsed.ui.showTrail).toBe(false);
    expect(parsed.analysis?.bookmarks).toEqual([1]);
    expect(parsed.analysis?.range).toEqual({ startIndex: 0, endIndex: 1 });
  });

  it('rejects unsupported session payloads', () => {
    const invalid = JSON.stringify({
      app: 'OtherApp',
      version: 1,
      exportedAt: new Date().toISOString(),
      frames: [],
      activeFields: [],
      customFields: [],
      workspace,
      playback: { isLive: true, scrubIndex: 0 },
      ui: { showVectors: true, showTrail: true },
      analysis: { range: null, bookmarks: [] },
    });

    expect(() => parseSessionFile(invalid)).toThrow('Unsupported session file format.');
  });

  it('builds the canonical session envelope', () => {
    const session = createSessionFile({
      frames: [makeFrame(0)],
      activeFields: ['speed'],
      customFields: [],
      workspace,
      playback: { isLive: true, scrubIndex: 0 },
      ui: { showVectors: true, showTrail: true },
      analysis: { range: null, bookmarks: [] },
    });

    expect(session.app).toBe('SwerveScope');
    expect(session.version).toBe(1);
    expect(typeof session.exportedAt).toBe('string');
  });

  it('defaults missing analysis metadata for older sessions', () => {
    const parsed = parseSessionFile(JSON.stringify({
      app: 'SwerveScope',
      version: 1,
      exportedAt: new Date().toISOString(),
      frames: [makeFrame(0)],
      activeFields: ['speed'],
      customFields: [],
      workspace,
      playback: { isLive: true, scrubIndex: 0 },
      ui: { showVectors: true, showTrail: true },
    }));

    expect(parsed.analysis).toEqual({ range: null, bookmarks: [] });
  });
});
