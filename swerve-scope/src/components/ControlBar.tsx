import type { AnalysisRange, ConnectionStatus, RuntimeMode } from '../types/telemetry';

interface ControlBarProps {
  status: ConnectionStatus;
  runtimeMode: RuntimeMode;
  schemaMismatch: string | null;
  isLive: boolean;
  isPlaying: boolean;
  playbackRate: number;
  historyLength: number;
  scrubIndex: number;
  durationSec: number;
  onSeek: (idx: number) => void;
  onTogglePlay: () => void;
  onStep: (delta: number) => void;
  onRateChange: (rate: number) => void;
  onSnapToLive: () => void;
  onClearHistory: () => void;
  showVectors: boolean;
  showTrail: boolean;
  onToggleVectors: () => void;
  onToggleTrail: () => void;
  onResetWorkspace: () => void;
  onResetSimulation: () => void;
  onExportSession: () => void;
  onImportSession: () => void;
  sessionError: string | null;
  bookmarkCount: number;
  analysisRange: AnalysisRange | null;
  onAddBookmark: () => void;
  onPrevBookmark: () => void;
  onNextBookmark: () => void;
  onClearRange: () => void;
}

const STATUS_COLORS: Record<ConnectionStatus, string> = {
  connected: 'dot-live',
  connecting: 'dot-conn',
  disconnected: 'dot-dead',
};

const STATUS_TEXT: Record<ConnectionStatus, string> = {
  connected: 'SITL LIVE',
  connecting: 'CONNECTING',
  disconnected: 'OFFLINE',
};

function fmtTime(sec: number) {
  const minutes = Math.floor(sec / 60);
  const seconds = (sec % 60).toFixed(1);
  return `${minutes}:${seconds.padStart(4, '0')}`;
}

export default function ControlBar({
  status,
  runtimeMode,
  schemaMismatch,
  isLive,
  isPlaying,
  playbackRate,
  historyLength,
  scrubIndex,
  durationSec,
  onSeek,
  onTogglePlay,
  onStep,
  onRateChange,
  onSnapToLive,
  onClearHistory,
  showVectors,
  showTrail,
  onToggleVectors,
  onToggleTrail,
  onResetWorkspace,
  onResetSimulation,
  onExportSession,
  onImportSession,
  sessionError,
  bookmarkCount,
  analysisRange,
  onAddBookmark,
  onPrevBookmark,
  onNextBookmark,
  onClearRange,
}: ControlBarProps) {
  const currentSec = scrubIndex * 0.02;
  const rangeLabel = analysisRange
    ? `${analysisRange.startIndex}..${analysisRange.endIndex}`
    : 'none';

  return (
    <div className="flex-shrink-0 bg-scope-surface border-b border-scope-border">
      <div className="flex items-center h-9 px-3 gap-4">
        <div className="flex items-center gap-2 flex-shrink-0">
          <span className="text-[11px] font-mono font-black tracking-tight text-scope-accent">
            SWERVE<span className="text-scope-text">SCOPE</span>
          </span>
          <span className="text-[8px] font-mono text-scope-muted">canonical</span>
        </div>

        <div className="flex items-center gap-1.5 flex-shrink-0">
          <div className={`w-1.5 h-1.5 rounded-full ${STATUS_COLORS[status]}`} />
          <span className={`text-[9px] font-mono font-bold ${
            status === 'connected' ? 'text-scope-green' :
            status === 'connecting' ? 'text-amber-400' :
            'text-scope-muted'
          }`}>
            {STATUS_TEXT[status]}
          </span>
        </div>

        <div className={`flex items-center gap-1.5 border rounded px-2 py-1 ${
          runtimeMode === 'sitl' ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-scope-border bg-scope-bg'
        }`}>
          <span className="text-[8px] font-mono uppercase tracking-wide text-scope-muted">mode</span>
          <span className={`text-[9px] font-mono font-bold ${runtimeMode === 'sitl' ? 'text-emerald-300' : 'text-scope-accent'}`}>
            {runtimeMode === 'sitl' ? 'JAVA // SITL' : 'LOCAL // TS'}
          </span>
        </div>

        <div className="flex items-center gap-2 border border-scope-border rounded px-2 py-1">
          <span className="text-[9px] font-mono uppercase tracking-wide text-scope-muted">
            Docked Workspace
          </span>
          <span className="text-[8px] font-mono text-scope-muted">
            range {rangeLabel}
          </span>
        </div>

        <div className="flex-1" />

        <div className="flex items-center gap-2">
          <button onClick={onToggleVectors} className={`btn ${showVectors ? 'btn-accent' : 'btn-ghost'} text-[9px]`}>
            Vectors
          </button>
          <button onClick={onToggleTrail} className={`btn ${showTrail ? 'btn-accent' : 'btn-ghost'} text-[9px]`}>
            Trail
          </button>
        </div>

        <span className="text-[9px] font-mono text-scope-muted readout">
          {historyLength} frames
        </span>

        {!isLive && (
          <button onClick={onSnapToLive} className="btn btn-success text-[9px] animate-pulse">
            LIVE
          </button>
        )}

        <button onClick={onResetWorkspace} className="btn btn-ghost text-[9px]">
          RESET LAYOUT
        </button>
        {runtimeMode === 'local' && (
          <button onClick={onResetSimulation} className="btn btn-ghost text-[9px]">
            RESET SIM
          </button>
        )}
        <button onClick={onImportSession} className="btn btn-ghost text-[9px]">
          LOAD
        </button>
        <button onClick={onExportSession} className="btn btn-ghost text-[9px]" disabled={historyLength === 0}>
          SAVE
        </button>
        <button onClick={onClearHistory} className="btn btn-ghost text-[9px]">
          CLR
        </button>
      </div>

      {schemaMismatch && (
        <div className="px-3 pb-2 text-[9px] font-mono text-amber-300">
          {schemaMismatch}
        </div>
      )}

      {sessionError && (
        <div className="px-3 pb-2 text-[9px] font-mono text-red-300">
          {sessionError}
        </div>
      )}

      {runtimeMode === 'local' && (
        <div className="px-3 pb-2 text-[9px] font-mono text-scope-muted">
          Local sim ready: use a gamepad or `WASD` for translation, `Q/E` for turn, arrows for snap headings.
        </div>
      )}

      <div className="flex items-center gap-3 px-3 pb-2">
        {!isLive && (
          <div className="flex items-center gap-1 flex-shrink-0">
            <button onClick={() => onStep(-1)} className="btn btn-ghost text-[9px] py-0.5 px-2" title="Step back">
              {'<'}
            </button>
            <button onClick={onTogglePlay} className={`btn text-[9px] py-0.5 px-2 ${isPlaying ? 'btn-accent' : 'btn-ghost'}`}>
              {isPlaying ? 'PAUSE' : 'PLAY'}
            </button>
            <button onClick={() => onStep(1)} className="btn btn-ghost text-[9px] py-0.5 px-2" title="Step forward">
              {'>'}
            </button>
          </div>
        )}

        <span className="text-[9px] font-mono text-scope-muted w-12 text-right readout flex-shrink-0">
          {fmtTime(currentSec)}
        </span>

        <div className="relative flex-1 group">
          <input
            type="range"
            min={0}
            max={Math.max(0, historyLength - 1)}
            value={scrubIndex}
            onChange={event => onSeek(parseInt(event.target.value))}
            className="w-full h-1 rounded-full appearance-none cursor-pointer"
            style={{
              background: `linear-gradient(to right, #0ea5e9 ${
                historyLength > 1 ? (scrubIndex / (historyLength - 1)) * 100 : 0
              }%, #1a2535 0%)`,
            }}
          />
        </div>

        <span className="text-[9px] font-mono text-scope-muted w-12 readout flex-shrink-0">
          {fmtTime(durationSec)}
        </span>

        {!isLive && (
          <div className="flex items-center gap-1 flex-shrink-0">
            {[0.25, 0.5, 1, 2, 4].map(rate => (
              <button
                key={rate}
                onClick={() => onRateChange(rate)}
                className={`btn text-[9px] py-0.5 px-1.5 ${playbackRate === rate ? 'btn-accent' : 'btn-ghost'}`}
              >
                {rate}x
              </button>
            ))}
          </div>
        )}

        <div className="flex items-center gap-1.5 flex-shrink-0">
          <button onClick={onPrevBookmark} className="btn btn-ghost text-[9px] py-0.5 px-1.5" disabled={bookmarkCount === 0}>
            B-
          </button>
          <button onClick={onAddBookmark} className="btn btn-ghost text-[9px] py-0.5 px-1.5">
            B+
          </button>
          <button onClick={onNextBookmark} className="btn btn-ghost text-[9px] py-0.5 px-1.5" disabled={bookmarkCount === 0}>
            B&gt;
          </button>
          <button onClick={onClearRange} className="btn btn-ghost text-[9px] py-0.5 px-1.5" disabled={!analysisRange}>
            RANGE CLR
          </button>
          <span className="text-[8px] font-mono text-scope-muted">{bookmarkCount} bookmarks</span>
        </div>

        <div className={`w-2 h-2 rounded-full flex-shrink-0 ${isLive ? 'dot-live' : 'dot-conn'}`} />
      </div>
    </div>
  );
}
