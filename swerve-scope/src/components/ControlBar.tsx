// ControlBar.tsx — top header: connection, playback controls, timeline scrubber
import type { ConnectionStatus } from '../types/telemetry';

interface ControlBarProps {
  status:          ConnectionStatus;
  isLive:          boolean;
  isPlaying:       boolean;
  playbackRate:    number;
  historyLength:   number;
  scrubIndex:      number;
  durationSec:     number;
  onSeek:          (idx: number) => void;
  onTogglePlay:    () => void;
  onStep:          (delta: number) => void;
  onRateChange:    (rate: number) => void;
  onSnapToLive:    () => void;
  onClearHistory:  () => void;
  showVectors:     boolean;
  showTrail:       boolean;
  onToggleVectors: () => void;
  onToggleTrail:   () => void;
  activeTab:       string;
  onTabChange:     (tab: string) => void;
  tabs:            { id: string; label: string }[];
}

const STATUS_COLORS: Record<ConnectionStatus, string> = {
  connected:    'dot-live',
  connecting:   'dot-conn',
  disconnected: 'dot-dead',
};
const STATUS_TEXT: Record<ConnectionStatus, string> = {
  connected:    'SITL LIVE',
  connecting:   'CONNECTING',
  disconnected: 'OFFLINE',
};

function fmtTime(sec: number) {
  const m = Math.floor(sec / 60);
  const s = (sec % 60).toFixed(1);
  return `${m}:${s.padStart(4, '0')}`;
}

export default function ControlBar({
  status, isLive, isPlaying, playbackRate, historyLength, scrubIndex, durationSec,
  onSeek, onTogglePlay, onStep, onRateChange, onSnapToLive, onClearHistory,
  showVectors, showTrail, onToggleVectors, onToggleTrail,
  activeTab, onTabChange, tabs,
}: ControlBarProps) {
  const currentSec = scrubIndex * 0.02; // 50Hz

  return (
    <div className="flex-shrink-0 bg-scope-surface border-b border-scope-border">
      {/* ── Top row ─────────────────────────────────────────────────────── */}
      <div className="flex items-center h-9 px-3 gap-4">
        {/* Logo */}
        <div className="flex items-center gap-2 flex-shrink-0">
          <span className="text-[11px] font-mono font-black tracking-tight text-scope-accent">
            SWERVE<span className="text-scope-text">SCOPE</span>
          </span>
          <span className="text-[8px] font-mono text-scope-muted">v2.0</span>
        </div>

        {/* Connection badge */}
        <div className="flex items-center gap-1.5 flex-shrink-0">
          <div className={`w-1.5 h-1.5 rounded-full ${STATUS_COLORS[status]}`} />
          <span className={`text-[9px] font-mono font-bold ${
            status === 'connected'    ? 'text-scope-green' :
            status === 'connecting'   ? 'text-amber-400'   : 'text-scope-muted'
          }`}>
            {STATUS_TEXT[status]}
          </span>
        </div>

        {/* Tab navigation */}
        <div className="flex items-center gap-0.5 border border-scope-border rounded overflow-hidden">
          {tabs.map(t => (
            <button
              key={t.id}
              onClick={() => onTabChange(t.id)}
              className={`px-3 py-1 text-[9px] font-mono uppercase tracking-wide transition-colors ${
                activeTab === t.id
                  ? 'bg-scope-accent text-white'
                  : 'text-scope-muted hover:text-scope-text hover:bg-scope-surface'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="flex-1" />

        {/* Overlay toggles */}
        <div className="flex items-center gap-2">
          <button
            onClick={onToggleVectors}
            className={`btn ${showVectors ? 'btn-accent' : 'btn-ghost'} text-[9px]`}
          >
            Vectors
          </button>
          <button
            onClick={onToggleTrail}
            className={`btn ${showTrail ? 'btn-accent' : 'btn-ghost'} text-[9px]`}
          >
            Trail
          </button>
        </div>

        {/* History stats */}
        <span className="text-[9px] font-mono text-scope-muted readout">
          {historyLength} frames
        </span>

        {/* Live snap */}
        {!isLive && (
          <button onClick={onSnapToLive} className="btn btn-success text-[9px] animate-pulse">
            ▶ LIVE
          </button>
        )}

        {/* Clear */}
        <button onClick={onClearHistory} className="btn btn-ghost text-[9px]">
          CLR
        </button>
      </div>

      {/* ── Scrubber row ─────────────────────────────────────────────────── */}
      <div className="flex items-center gap-3 px-3 pb-2">
        {!isLive && (
          <div className="flex items-center gap-1 flex-shrink-0">
            <button onClick={() => onStep(-1)} className="btn btn-ghost text-[9px] py-0.5 px-2" title="Step back">
              ◀
            </button>
            <button onClick={onTogglePlay} className={`btn text-[9px] py-0.5 px-2 ${isPlaying ? 'btn-accent' : 'btn-ghost'}`}>
              {isPlaying ? '⏸' : '▶'}
            </button>
            <button onClick={() => onStep(1)} className="btn btn-ghost text-[9px] py-0.5 px-2" title="Step forward">
              ▶
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
            onChange={e => onSeek(parseInt(e.target.value))}
            className="w-full h-1 rounded-full appearance-none cursor-pointer"
            style={{
              background: `linear-gradient(to right, #0ea5e9 ${
                historyLength > 1 ? (scrubIndex / (historyLength - 1)) * 100 : 0
              }%, #1a2535 0%)`,
              // accent-color fallback for thumb
            }}
          />
        </div>

        <span className="text-[9px] font-mono text-scope-muted w-12 readout flex-shrink-0">
          {fmtTime(durationSec)}
        </span>

        {!isLive && (
          <div className="flex items-center gap-1 flex-shrink-0">
            {[0.25, 0.5, 1, 2, 4].map(r => (
              <button
                key={r}
                onClick={() => onRateChange(r)}
                className={`btn text-[9px] py-0.5 px-1.5 ${playbackRate === r ? 'btn-accent' : 'btn-ghost'}`}
              >
                {r}×
              </button>
            ))}
          </div>
        )}

        {/* Playback indicator */}
        <div
          className={`w-2 h-2 rounded-full flex-shrink-0 ${isLive ? 'dot-live' : 'dot-conn'}`}
          title={isLive ? 'Live' : 'Scrubbing history'}
        />
      </div>
    </div>
  );
}
