// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { TelemetryFrame } from '../types/telemetry';
import ConsolePanel from './ConsolePanel';

let mockLiveFrame: TelemetryFrame | null = null;

vi.mock('../hooks/useTelemetry', () => ({
  useLiveFrame: () => mockLiveFrame,
}));

afterEach(() => {
  cleanup();
});

function makeFrame(overrides: Partial<TelemetryFrame> = {}): TelemetryFrame {
  return {
    schemaVersion: 1,
    timestamp: 1,
    x: 0,
    y: 0,
    heading: 0,
    targets: [[0, 0], [0, 0], [0, 0], [0, 0]],
    actuals: [[0, 0], [0, 0], [0, 0], [0, 0]],
    isMaintaining: false,
    isSnapping: false,
    snapTargetRad: 0,
    driveX: 0,
    driveY: 0,
    turn: 0,
    batteryVoltage: 12.8,
    loopTimeMs: 15,
    logs: [],
    ...overrides,
  };
}

describe('ConsolePanel', () => {
  beforeEach(() => {
    mockLiveFrame = null;
    vi.restoreAllMocks();
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      cb(0);
      return 0;
    });
  });

  it('filters by source and search text for live log entries', () => {
    const view = render(<ConsolePanel />);

    mockLiveFrame = makeFrame({
      timestamp: 2,
      batteryVoltage: 11.3,
      loopTimeMs: 33,
      logs: [
        { timestamp: 2, severity: 'info', source: 'JAVA', message: 'Connected to websocket' },
        { timestamp: 2.1, severity: 'warn', source: 'DRIVE', message: 'Wheel slip detected' },
      ],
    });
    view.rerender(<ConsolePanel />);

    expect(screen.getByText('Low battery: 11.30V')).toBeTruthy();
    expect(screen.getByText('Overrun: 33.0ms')).toBeTruthy();
    expect(screen.getByText('Connected to websocket')).toBeTruthy();
    expect(screen.getByText('Wheel slip detected')).toBeTruthy();

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'DRIVE' } });
    expect(screen.queryByText('Connected to websocket')).toBeNull();
    expect(screen.getByText('Wheel slip detected')).toBeTruthy();

    fireEvent.change(screen.getByPlaceholderText('filter...'), { target: { value: 'slip' } });
    expect(screen.getByText('Wheel slip detected')).toBeTruthy();

    fireEvent.change(screen.getByPlaceholderText('filter...'), { target: { value: 'missing' } });
    expect(screen.getByText('No matches')).toBeTruthy();
  });

  it('supports manual entries, pinning, pinned-only mode, and clearing', () => {
    render(<ConsolePanel isLive={false} />);

    const composer = screen.getByPlaceholderText('Inject log message...');
    fireEvent.change(composer, { target: { value: 'Bookmark-worthy event' } });
    fireEvent.click(screen.getByRole('button', { name: 'send' }));

    expect(screen.getByText('Bookmark-worthy event')).toBeTruthy();
    expect(screen.getAllByText('MANUAL').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '.' }));
    fireEvent.click(screen.getByRole('checkbox', { name: 'pinned' }));

    expect(screen.getByText('Bookmark-worthy event')).toBeTruthy();
    expect(screen.getByText('1 pinned')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'clr' }));
    expect(screen.getByText('No events in session')).toBeTruthy();
  });
});
