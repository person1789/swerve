import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import type { LogSeverity, StructuredLogEntry, TelemetryFrame } from '../types/telemetry';
import { useLiveFrame } from '../hooks/useTelemetry';

type Severity = LogSeverity;

interface LogEntry {
  id: number;
  ts: number;
  severity: Severity;
  source: string;
  msg: string;
  pinned?: boolean;
}

let idCounter = 0;

function mkEntry(ts: number, severity: Severity, source: string, msg: string): LogEntry {
  return { id: idCounter++, ts, severity, source, msg, pinned: false };
}

function frameToLogs(frame: TelemetryFrame, prev: TelemetryFrame | null): LogEntry[] {
  const entries: LogEntry[] = [];

  if (frame.batteryVoltage < 11.5 && (!prev || prev.batteryVoltage >= 11.5)) {
    entries.push(mkEntry(frame.timestamp, 'error', 'POWER', `Low battery: ${frame.batteryVoltage.toFixed(2)}V`));
  } else if (frame.batteryVoltage < 12.3 && (!prev || prev.batteryVoltage >= 12.3)) {
    entries.push(mkEntry(frame.timestamp, 'warn', 'POWER', `Battery ${frame.batteryVoltage.toFixed(2)}V`));
  }

  if (frame.loopTimeMs > 30 && (!prev || prev.loopTimeMs <= 30)) {
    entries.push(mkEntry(frame.timestamp, 'warn', 'LOOP', `Overrun: ${frame.loopTimeMs.toFixed(1)}ms`));
  } else if (frame.loopTimeMs <= 25 && prev && prev.loopTimeMs > 30) {
    entries.push(mkEntry(frame.timestamp, 'info', 'LOOP', 'Loop time normalized'));
  }

  if (prev && !prev.isSnapping && frame.isSnapping) {
    entries.push(mkEntry(frame.timestamp, 'info', 'HDG_CTRL', `Snap -> ${(frame.snapTargetRad * 180 / Math.PI).toFixed(1)}deg`));
  }
  if (prev && prev.isSnapping && !frame.isSnapping) {
    entries.push(mkEntry(frame.timestamp, 'info', 'HDG_CTRL', 'Snap complete'));
  }
  if (prev && !prev.isMaintaining && frame.isMaintaining) {
    entries.push(mkEntry(frame.timestamp, 'debug', 'HDG_CTRL', 'Heading lock engaged'));
  }
  if (prev && prev.isMaintaining && !frame.isMaintaining) {
    entries.push(mkEntry(frame.timestamp, 'debug', 'HDG_CTRL', 'Heading lock released'));
  }

  (frame.logs ?? []).forEach((log: StructuredLogEntry) => {
    entries.push(mkEntry(log.timestamp, log.severity, log.source, log.message));
  });

  return entries;
}

const SEV_COLOR: Record<Severity, string> = {
  error: '#ef4444',
  warn: '#f59e0b',
  info: '#0ea5e9',
  debug: '#4a6a8a',
};

const SEV_BG: Record<Severity, string> = {
  error: 'rgba(239,68,68,0.06)',
  warn: 'rgba(245,158,11,0.06)',
  info: 'transparent',
  debug: 'transparent',
};

const MAX_LOG = 600;

interface ConsolePanelProps {
  isLive?: boolean;
}

export default function ConsolePanel({ isLive: isLiveProp }: ConsolePanelProps) {
  const liveFrame = useLiveFrame();
  const [entries, setEntries] = useState<LogEntry[]>([]);
  const [filter, setFilter] = useState<Severity | 'all'>('all');
  const [autoScroll, setAutoScroll] = useState(true);
  const [search, setSearch] = useState('');
  const [draft, setDraft] = useState('');
  const [selectedSource, setSelectedSource] = useState<string>('all');
  const [pinnedOnly, setPinnedOnly] = useState(false);

  const listRef = useRef<HTMLDivElement>(null);
  const prevFrameRef = useRef<TelemetryFrame | null>(null);
  const userScrolledRef = useRef(false);

  useEffect(() => {
    if (!liveFrame) return;
    const newLogs = frameToLogs(liveFrame, prevFrameRef.current);
    prevFrameRef.current = liveFrame;
    if (newLogs.length === 0) return;
    setEntries(prev => {
      const next = [...prev, ...newLogs];
      const pinned = next.filter(entry => entry.pinned);
      const unpinned = next.filter(entry => !entry.pinned);
      const trimmed = unpinned.length > MAX_LOG ? unpinned.slice(-MAX_LOG) : unpinned;
      return [...pinned, ...trimmed];
    });
  }, [liveFrame]);

  useEffect(() => {
    if (!autoScroll || userScrolledRef.current) return;
    const el = listRef.current;
    if (!el) return;
    requestAnimationFrame(() => {
      if (el) el.scrollTop = el.scrollHeight;
    });
  }, [entries, autoScroll]);

  const handleScroll = useCallback(() => {
    const el = listRef.current;
    if (!el) return;
    const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 20;
    userScrolledRef.current = !atBottom;
    if (atBottom) setAutoScroll(true);
  }, []);

  const clear = useCallback(() => {
    setEntries([]);
    prevFrameRef.current = null;
  }, []);

  const isLive = isLiveProp ?? true;

  const sources = useMemo(
    () => Array.from(new Set(entries.map(entry => entry.source))).sort(),
    [entries],
  );

  const filtered = entries.filter(entry => {
    if (filter !== 'all' && entry.severity !== filter) return false;
    if (selectedSource !== 'all' && entry.source !== selectedSource) return false;
    if (pinnedOnly && !entry.pinned) return false;
    if (search) {
      const q = search.toLowerCase();
      if (!entry.msg.toLowerCase().includes(q) && !entry.source.toLowerCase().includes(q)) return false;
    }
    return true;
  });

  const counts = { error: 0, warn: 0, info: 0, debug: 0 };
  entries.forEach(entry => counts[entry.severity]++);
  const pinnedCount = entries.filter(entry => entry.pinned).length;

  const togglePin = (id: number) => {
    setEntries(prev => prev.map(entry => entry.id === id ? { ...entry, pinned: !entry.pinned } : entry));
  };

  const exportLogs = () => {
    const blob = new Blob([JSON.stringify(filtered, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `swervescope-logs-${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col h-full w-full min-h-0 overflow-hidden bg-scope-bg">
      <div className="flex items-center gap-1.5 px-2 py-1.5 border-b border-scope-border flex-shrink-0 flex-wrap min-w-0">
        {(['all', 'error', 'warn', 'info', 'debug'] as const).map(level => (
          <button
            key={level}
            onClick={() => setFilter(level)}
            className={`px-2 py-0.5 rounded text-[9px] font-mono border transition-colors flex-shrink-0 ${
              filter === level ? 'border-current opacity-100' : 'border-scope-border opacity-50 hover:opacity-75'
            }`}
            style={{ color: level === 'all' ? '#8ba4c0' : SEV_COLOR[level as Severity] }}
          >
            {level.toUpperCase()}
            {level !== 'all' && (
              <span className="ml-1 opacity-70">{counts[level as Severity]}</span>
            )}
          </button>
        ))}

        <select
          value={selectedSource}
          onChange={event => setSelectedSource(event.target.value)}
          className="workspace-select h-5 text-[9px]"
        >
          <option value="all">all sources</option>
          {sources.map(source => (
            <option key={source} value={source}>{source}</option>
          ))}
        </select>

        <input
          type="text"
          placeholder="filter..."
          value={search}
          onChange={event => setSearch(event.target.value)}
          className="expr-input py-0.5 text-[9px] h-5 flex-1 min-w-[60px]"
        />

        <label className="flex items-center gap-1 text-[9px] font-mono text-scope-muted cursor-pointer select-none flex-shrink-0">
          <input
            type="checkbox"
            checked={pinnedOnly}
            onChange={event => setPinnedOnly(event.target.checked)}
            className="w-3 h-3"
          />
          pinned
        </label>

        <label className="flex items-center gap-1 text-[9px] font-mono text-scope-muted cursor-pointer select-none flex-shrink-0">
          <input
            type="checkbox"
            checked={autoScroll}
            onChange={event => {
              setAutoScroll(event.target.checked);
              userScrolledRef.current = false;
            }}
            className="w-3 h-3"
          />
          auto
        </label>

        <button onClick={exportLogs} className="btn btn-ghost text-[9px] py-0.5 flex-shrink-0">export</button>
        <button onClick={clear} className="btn btn-ghost text-[9px] py-0.5 flex-shrink-0">clr</button>
      </div>

      <div
        ref={listRef}
        onScroll={handleScroll}
        className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden"
        style={{ overscrollBehavior: 'contain' }}
      >
        {filtered.length === 0 ? (
          <div className="p-4 text-[9px] font-mono text-scope-muted">
            {entries.length === 0
              ? isLive ? 'Waiting for events...' : 'No events in session'
              : 'No matches'}
          </div>
        ) : (
          filtered.map(entry => (
            <div
              key={entry.id}
              className="flex gap-2 px-2 py-[2px] border-b border-scope-border/40 hover:bg-scope-surface/50 transition-colors"
              style={{ background: SEV_BG[entry.severity] }}
            >
              <button
                className={`text-[8px] font-mono w-4 flex-shrink-0 ${entry.pinned ? 'text-amber-300' : 'text-scope-muted'}`}
                onClick={() => togglePin(entry.id)}
                title={entry.pinned ? 'Unpin' : 'Pin'}
              >
                {entry.pinned ? '!' : '.'}
              </button>
              <span className="text-scope-muted flex-shrink-0 w-[52px] text-[8px] font-mono readout pt-[1px]">
                {entry.ts.toFixed(2)}s
              </span>
              <span className="flex-shrink-0 w-8 text-right font-bold text-[8px] font-mono pt-[1px]"
                style={{ color: SEV_COLOR[entry.severity] }}>
                {entry.severity.slice(0, 3).toUpperCase()}
              </span>
              <span className="text-scope-muted flex-shrink-0 w-16 truncate text-[8px] font-mono pt-[1px]">
                {entry.source}
              </span>
              <span className="text-scope-text flex-1 min-w-0 text-[9px] font-mono break-words leading-relaxed">
                {entry.msg}
              </span>
            </div>
          ))
        )}
        <div style={{ height: 1 }} />
      </div>

      <div className="flex items-center justify-between px-3 py-1 border-t border-scope-border flex-shrink-0">
        <span className="text-[8px] font-mono text-scope-muted">
          {filtered.length} / {entries.length} entries
        </span>
        <span className="text-[8px] font-mono text-scope-muted">
          {pinnedCount} pinned
        </span>
        {counts.error > 0 && (
          <span className="text-[8px] font-mono text-red-400 animate-pulse">
            {counts.error} error{counts.error > 1 ? 's' : ''}
          </span>
        )}
      </div>

      <div className="flex gap-2 px-2 py-1.5 border-t border-scope-border flex-shrink-0">
        <input
          className="expr-input flex-1 text-[9px] py-1"
          placeholder="Inject log message..."
          value={draft}
          onChange={event => setDraft(event.target.value)}
          onKeyDown={event => {
            if (event.key === 'Enter' && draft.trim()) {
              setEntries(prev => [
                ...prev,
                mkEntry(performance.now() / 1000, 'info', 'MANUAL', draft.trim()),
              ]);
              setDraft('');
              userScrolledRef.current = false;
            }
          }}
        />
        <button
          onClick={() => {
            if (!draft.trim()) return;
            setEntries(prev => [
              ...prev,
              mkEntry(performance.now() / 1000, 'info', 'MANUAL', draft.trim()),
            ]);
            setDraft('');
            userScrolledRef.current = false;
          }}
          className="btn btn-accent text-[9px] py-1 px-2 flex-shrink-0"
        >
          send
        </button>
      </div>
    </div>
  );
}
