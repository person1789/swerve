// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import type { ComponentProps } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { AnalysisRange } from '../types/telemetry';
import ControlBar from './ControlBar';

afterEach(() => {
  cleanup();
});

function renderControlBar(overrides: Partial<ComponentProps<typeof ControlBar>> = {}) {
  const handlers = {
    onSeek: vi.fn(),
    onTogglePlay: vi.fn(),
    onStep: vi.fn(),
    onRateChange: vi.fn(),
    onSnapToLive: vi.fn(),
    onClearHistory: vi.fn(),
    onToggleVectors: vi.fn(),
    onToggleTrail: vi.fn(),
    onResetWorkspace: vi.fn(),
    onResetSimulation: vi.fn(),
    onExportSession: vi.fn(),
    onImportSession: vi.fn(),
    onAddBookmark: vi.fn(),
    onPrevBookmark: vi.fn(),
    onNextBookmark: vi.fn(),
    onClearRange: vi.fn(),
  };

  const analysisRange: AnalysisRange | null = { startIndex: 12, endIndex: 28 };

  render(
    <ControlBar
      status="connected"
      runtimeMode="local"
      schemaMismatch={null}
      isLive={false}
      isPlaying={false}
      playbackRate={1}
      historyLength={120}
      scrubIndex={24}
      durationSec={2.4}
      showVectors={true}
      showTrail={false}
      sessionError={null}
      bookmarkCount={3}
      analysisRange={analysisRange}
      {...handlers}
      {...overrides}
    />,
  );

  return handlers;
}

describe('ControlBar', () => {
  it('wires the main session and playback controls', () => {
    const handlers = renderControlBar();

    fireEvent.click(screen.getByRole('button', { name: 'Vectors' }));
    fireEvent.click(screen.getByRole('button', { name: 'Trail' }));
    fireEvent.click(screen.getByRole('button', { name: 'RESET LAYOUT' }));
    fireEvent.click(screen.getByRole('button', { name: 'RESET SIM' }));
    fireEvent.click(screen.getByRole('button', { name: 'LOAD' }));
    fireEvent.click(screen.getByRole('button', { name: 'SAVE' }));
    fireEvent.click(screen.getByRole('button', { name: 'CLR' }));
    fireEvent.click(screen.getByRole('button', { name: 'PLAY' }));
    fireEvent.click(screen.getByRole('button', { name: 'B-' }));
    fireEvent.click(screen.getByRole('button', { name: 'B+' }));
    fireEvent.click(screen.getByRole('button', { name: 'B>' }));
    fireEvent.click(screen.getByRole('button', { name: 'RANGE CLR' }));
    fireEvent.click(screen.getByRole('button', { name: '2x' }));

    fireEvent.change(screen.getByRole('slider'), { target: { value: '48' } });

    expect(handlers.onToggleVectors).toHaveBeenCalledTimes(1);
    expect(handlers.onToggleTrail).toHaveBeenCalledTimes(1);
    expect(handlers.onResetWorkspace).toHaveBeenCalledTimes(1);
    expect(handlers.onResetSimulation).toHaveBeenCalledTimes(1);
    expect(handlers.onImportSession).toHaveBeenCalledTimes(1);
    expect(handlers.onExportSession).toHaveBeenCalledTimes(1);
    expect(handlers.onClearHistory).toHaveBeenCalledTimes(1);
    expect(handlers.onTogglePlay).toHaveBeenCalledTimes(1);
    expect(handlers.onPrevBookmark).toHaveBeenCalledTimes(1);
    expect(handlers.onAddBookmark).toHaveBeenCalledTimes(1);
    expect(handlers.onNextBookmark).toHaveBeenCalledTimes(1);
    expect(handlers.onClearRange).toHaveBeenCalledTimes(1);
    expect(handlers.onRateChange).toHaveBeenCalledWith(2);
    expect(handlers.onSeek).toHaveBeenCalledWith(48);
  });

  it('adapts its visible controls for live sitl mode', () => {
    renderControlBar({
      runtimeMode: 'sitl',
      isLive: true,
      bookmarkCount: 0,
      analysisRange: null,
      historyLength: 0,
    });

    expect(screen.queryByRole('button', { name: 'RESET SIM' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'PLAY' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'LIVE' })).toBeNull();
    expect(screen.getByText('JAVA // SITL')).toBeTruthy();
    expect(screen.getByText('range none')).toBeTruthy();
    expect((screen.getByRole('button', { name: 'SAVE' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'B-' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'B>' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'RANGE CLR' }) as HTMLButtonElement).disabled).toBe(true);
  });
});
