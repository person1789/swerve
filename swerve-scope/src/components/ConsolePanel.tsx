// ConsolePanel.tsx — scrolling system log with severity filter
import { useEffect, useRef, useState, useCallback } from 'react';
import type { TelemetryFrame } from '../types/telemetry';

type Severity = 'info' | 'warn' | 'error' | 'debug';

interface LogEntry {
  id:       number;
  ts:       number;
  severity: Severity;
  source:   string;
  msg:      string;
}

let _id = 0;
function mkEntry(ts: number, severity: Severity, source: string, msg: string): LogEntry {
  return { id: _id++, ts, severity, source, msg };
}

// Generate log events from telemetry frames
function frameToLogs(frame: TelemetryFrame, prev: TelemetryFrame | null): LogEntry[] {
  const entries: LogEntry[] = [];

  // Battery warnings
  if (frame.batteryVoltage < 11.5) {
    entries.push(mkEntry(frame.timestamp, 'error', 'POWER', `Low battery: ${frame.batteryVoltage.toFixed(2)}V`));
  } else if (frame.batteryVoltage < 12.3) {
    entries.push(mkEntry(frame.timestamp, 'warn', 'POWER', `Battery ${frame.batteryVoltage.toFixed(2)}V — consider swap`));
  }

  // Loop time spikes
  if (frame.loopTimeMs > 30) {
    entries.push(mkEntry(frame.timestamp, 'warn', 'LOOP', `Overrun: ${frame.loopTimeMs.toFixed(1)}ms`));
  }

  // Snap transitions
  if (prev && !prev.isSnapping && frame.isSnapping) {
    entries.push(mkEntry(frame.timestamp, 'info', 'HDG_CTRL', `Snap activated → ${(frame.snapTargetRad * 180 / Math.PI).toFixed(1)}°`));
  }
  if (prev && prev.isSnapping && !frame.isSnapping) {
    entries.push(mkEntry(frame.timestamp, 'info', 'HDG_CTRL', 'Snap complete'));
  }

  // Maintain transitions
  if (prev && !prev.isMaintaining && frame.isMaintaining) {
    entries.push(mkEntry(frame.timestamp, 'debug', 'HDG_CTRL', 'Heading lock engaged'));
  }

  return entries;
}

const SEV_COLOR: Record<Severity, string> = {
  error: '#ef4444',
  warn:  '#f59e0b',
  info:  '#0ea5e9',
  debug: '#4a6a8a',
};

const MAX_LOG = 2000;

interface ConsolePanelProps {
  frame:     TelemetryFrame | null;
  prevFrame: TelemetryFrame | null;
  isLive:    boolean;
}

export default function ConsolePanel({ frame, prevFrame, isLive }: ConsolePanelProps) {
  const [entries, setEntries]       = useState<LogEntry[]>([]);
  const [filter, setFilter]         = useState<Severity | 'all'>('all');
  const [autoScroll, setAutoScroll] = useState(true);
  const [search, setSearch]         = useState('');
  const listRef = useRef<HTMLDivElement>(null);
  const prevFrameRef = useRef<TelemetryFrame | null>(null);

  // Ingest new frame
  useEffect(() => {
    if (!frame) return;
    const newLogs = frameToLogs(frame, prevFrameRef.current);
    prevFrameRef.current = frame;
    if (newLogs.length === 0) return;
    setEntries(prev => {
      const next = [...prev, ...newLogs];
      return next.length > MAX_LOG ? next.slice(-MAX_LOG) : next;
    });
  }, [frame]);

  // Auto-scroll
  useEffect(() => {
    if (autoScroll && isLive && listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [entries, autoScroll, isLive]);

  const clear = useCallback(() => setEntries([]), []);

  const filtered = entries.filter(e => {
    if (filter !== 'all' && e.severity !== filter) return false;
    if (search && !e.msg.toLowerCase().includes(search.toLowerCase()) &&
        !e.source.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const counts = { error: 0, warn: 0, info: 0, debug: 0 };
  entries.forEach(e => counts[e.severity]++);

  return (
    <div className="flex flex-col h-full bg-scope-bg">
      {/* Toolbar */}
      <div className="flex items-center gap-2 px-2 py-1.5 border-b border-scope-border flex-shrink-0 flex-wrap">
        {/* Severity filter chips */}
        {(['all', 'error', 'warn', 'info', 'debug'] as const).map(s => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`px-2 py-0.5 rounded text-[9px] font-mono border transition-colors ${
              filter === s ? 'border-current opacity-100' : 'border-scope-border opacity-50 hover:opacity-75'
            }`}
            style={{ color: s === 'all' ? '#8ba4c0' : SEV_COLOR[s as Severity] }}
          >
            {s.toUpperCase()}
            {s !== 'all' && <span className="ml-1 opacity-70">{counts[s as Severity]}</span>}
          </button>
        ))}

        <div className="flex-1 min-w-[80px]">
          <input
            type="text"
            placeholder="search…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="expr-input py-0.5 text-[9px] h-5"
          />
        </div>

        <label className="flex items-center gap-1 text-[9px] font-mono text-scope-muted cursor-pointer select-none">
          <input
            type="checkbox"
            checked={autoScroll}
            onChange={e => setAutoScroll(e.target.checked)}
            className="w-3 h-3"
          />
          auto
        </label>

        <button onClick={clear} className="btn btn-ghost text-[9px] py-0.5">clr</button>
      </div>

      {/* Log entries */}
      <div ref={listRef} className="flex-1 overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="p-4 text-[9px] font-mono text-scope-muted">
            {entries.length === 0 ? 'Waiting for events…' : 'No matches'}
          </div>
        ) : (
          filtered.map(e => (
            <div key={e.id} className="log-line flex gap-2">
              <span className="text-scope-muted flex-shrink-0 w-14">
                {e.ts.toFixed(2)}s
              </span>
              <span
                className="flex-shrink-0 w-10 text-right font-bold"
                style={{ color: SEV_COLOR[e.severity] }}
              >
                {e.severity.toUpperCase().slice(0,3)}
              </span>
              <span className="text-scope-muted flex-shrink-0 w-20 truncate">
                {e.source}
              </span>
              <span className="text-scope-text flex-1 truncate">{e.msg}</span>
            </div>
          ))
        )}
      </div>

      {/* Status bar */}
      <div className="flex items-center justify-between px-3 py-1 border-t border-scope-border flex-shrink-0">
        <span className="text-[8px] font-mono text-scope-muted">
          {filtered.length} / {entries.length} entries
        </span>
        {counts.error > 0 && (
          <span className="text-[8px] font-mono text-red-400">
            {counts.error} error{counts.error > 1 ? 's' : ''}
          </span>
        )}
      </div>
    </div>
  );
}
