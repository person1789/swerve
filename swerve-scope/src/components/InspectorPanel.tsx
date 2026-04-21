import { useState, useRef, useEffect, useCallback } from 'react';
import { useHistoryBuffer, useLiveFrame } from '../hooks/useTelemetry';
import type { AnalysisRange, TelemetryFrame } from '../types/telemetry';

interface InspectorProps {
  scrubIndex: number;
  analysisRange: AnalysisRange | null;
  bookmarks: number[];
  onJumpToIndex: (index: number) => void;
  onSetRangeStart: (index: number) => void;
  onSetRangeEnd: (index: number) => void;
  onToggleBookmark: (index: number) => void;
}

type ViewMode = 'live' | 'table' | 'raw';

function fmt(n: number, d = 3) {
  return isNaN(n) ? '---' : n.toFixed(d);
}

function Row({ label, value, unit = '', color }: {
  label: string;
  value: string | number;
  unit?: string;
  color?: string;
}) {
  const v = typeof value === 'number' ? fmt(value) : value;
  return (
    <tr>
      <td className="px-2 py-[3px] text-scope-muted text-[9px] font-mono w-1/2 truncate">{label}</td>
      <td className="px-2 py-[3px] text-[9px] font-mono readout text-right" style={{ color: color || '#c8dff0' }}>
        {v}
        {unit && <span className="text-scope-muted ml-1 text-[8px]">{unit}</span>}
      </td>
    </tr>
  );
}

function Divider({ label }: { label: string }) {
  return (
    <tr>
      <td colSpan={2} className="px-2 pt-3 pb-1 text-[8px] font-mono font-bold text-scope-muted uppercase tracking-widest border-t border-scope-border">
        {label}
      </td>
    </tr>
  );
}

function useRecorder(history: TelemetryFrame[], historyLength: number) {
  const [recording, setRecording] = useState(false);
  const startIdxRef = useRef(0);

  const startRecording = useCallback(() => {
    startIdxRef.current = historyLength;
    setRecording(true);
  }, [historyLength]);

  const stopAndExport = useCallback(() => {
    setRecording(false);
    const frames = history.slice(startIdxRef.current);
    if (frames.length === 0) return;

    const headers = [
      'timestamp', 'x', 'y', 'heading',
      'fl_speed', 'fl_angle', 'fr_speed', 'fr_angle',
      'rr_speed', 'rr_angle', 'rl_speed', 'rl_angle',
      'battery', 'loop_ms', 'is_snapping', 'is_maintaining',
    ];
    const rows = frames.map(frame => [
      frame.timestamp.toFixed(4), frame.x.toFixed(4), frame.y.toFixed(4), frame.heading.toFixed(4),
      ...[0, 1, 2, 3].flatMap(i => [
        (frame.actuals[i]?.[0] ?? 0).toFixed(4),
        (frame.actuals[i]?.[1] ?? 0).toFixed(4),
      ]),
      frame.batteryVoltage.toFixed(3), frame.loopTimeMs.toFixed(2),
      frame.isSnapping ? '1' : '0', frame.isMaintaining ? '1' : '0',
    ].join(','));

    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `swervescope_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }, [history]);

  return { recording, startRecording, stopAndExport };
}

export default function InspectorPanel({
  scrubIndex,
  analysisRange,
  bookmarks,
  onJumpToIndex,
  onSetRangeStart,
  onSetRangeEnd,
  onToggleBookmark,
}: InspectorProps) {
  const frame = useLiveFrame();
  const { buffer: history, length: historyLength } = useHistoryBuffer();
  const [view, setView] = useState<ViewMode>('live');
  const [columns, setColumns] = useState({
    pose: true,
    heading: true,
    modules: true,
    battery: true,
  });
  const tableRef = useRef<HTMLDivElement>(null);
  const { recording, startRecording, stopAndExport } = useRecorder(history, historyLength);
  const visibleRows = history.slice(-250);
  const visibleStartIndex = Math.max(0, history.length - visibleRows.length);
  const isLive = scrubIndex >= historyLength - 1;

  useEffect(() => {
    if (isLive && view === 'table' && tableRef.current) {
      requestAnimationFrame(() => {
        if (tableRef.current) tableRef.current.scrollTop = tableRef.current.scrollHeight;
      });
    }
  }, [historyLength, isLive, view]);

  return (
    <div className="flex flex-col h-full min-h-0 overflow-hidden bg-scope-bg">
      <div className="flex items-center gap-2 px-3 py-1.5 border-b border-scope-border flex-shrink-0">
        <div className="flex bg-scope-surface border border-scope-border rounded overflow-hidden text-[9px] font-mono">
          {(['live', 'table', 'raw'] as ViewMode[]).map(nextView => (
            <button
              key={nextView}
              onClick={() => setView(nextView)}
              className={`px-2 py-1 uppercase tracking-wide transition-colors ${
                view === nextView ? 'bg-scope-accent text-white' : 'text-scope-muted hover:text-scope-text'
              }`}
            >
              {nextView}
            </button>
          ))}
        </div>

        <div className="flex-1" />

        <button onClick={() => onSetRangeStart(scrubIndex)} className="btn btn-ghost text-[9px]">range start</button>
        <button onClick={() => onSetRangeEnd(scrubIndex)} className="btn btn-ghost text-[9px]">range end</button>
        <button onClick={() => onToggleBookmark(scrubIndex)} className={`btn text-[9px] ${bookmarks.includes(scrubIndex) ? 'btn-accent' : 'btn-ghost'}`}>
          {bookmarks.includes(scrubIndex) ? 'unmark' : 'bookmark'}
        </button>
        {recording ? (
          <button onClick={stopAndExport} className="btn btn-danger text-[9px]">
            stop+export
          </button>
        ) : (
          <button onClick={startRecording} className="btn btn-success text-[9px]">
            record
          </button>
        )}
      </div>

      {view === 'table' && (
        <div className="flex items-center gap-3 px-3 py-1 border-b border-scope-border flex-wrap">
          {([
            ['pose', 'POSE'],
            ['heading', 'HDG'],
            ['modules', 'MODULES'],
            ['battery', 'BATT'],
          ] as const).map(([key, label]) => (
            <label key={key} className="flex items-center gap-1 text-[8px] font-mono text-scope-muted cursor-pointer">
              <input
                type="checkbox"
                checked={columns[key]}
                onChange={event => setColumns(prev => ({ ...prev, [key]: event.target.checked }))}
                className="w-3 h-3"
              />
              {label}
            </label>
          ))}
          <span className="text-[8px] font-mono text-scope-muted">
            range: {analysisRange ? `${analysisRange.startIndex}..${analysisRange.endIndex}` : 'none'}
          </span>
          <span className="ml-auto text-[8px] font-mono text-scope-muted">click row to scrub</span>
        </div>
      )}

      {view === 'live' && (
        <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden">
          <table className="w-full border-collapse">
            <tbody>
              <Divider label="Odometry" />
              <Row label="Field X" value={frame?.x ?? 0} unit="m" />
              <Row label="Field Y" value={frame?.y ?? 0} unit="m" />
              <Row label="Heading" value={(frame?.heading ?? 0) * 180 / Math.PI} unit="deg" />

              <Divider label="Controller" />
              <Row
                label="Mode"
                value={frame?.isSnapping ? 'SNAP' : frame?.isMaintaining ? 'HOLD' : 'MANUAL'}
                color={frame?.isSnapping ? '#a855f7' : frame?.isMaintaining ? '#0ea5e9' : '#4a6a8a'}
              />
              <Row label="Snap Target" value={(frame?.snapTargetRad ?? 0) * 180 / Math.PI} unit="deg" />

              <Divider label="Inputs" />
              <Row label="Drive X" value={frame?.driveX ?? 0} />
              <Row label="Drive Y" value={frame?.driveY ?? 0} />
              <Row label="Turn" value={frame?.turn ?? 0} />

              <Divider label="System" />
              <Row label="Battery" value={frame?.batteryVoltage ?? 0} unit="V" color="#f59e0b" />
              <Row
                label="Loop Time"
                value={frame?.loopTimeMs ?? 0}
                unit="ms"
                color={(frame?.loopTimeMs ?? 0) > 25 ? '#ef4444' : '#10b981'}
              />

              <Divider label="Modules (actual speed / angle)" />
              {[0, 1, 2, 3].map(i => {
                const actual = frame?.actuals?.[i] ?? [0, 0];
                return (
                  <Row
                    key={i}
                    label={['FL', 'FR', 'RR', 'RL'][i]}
                    value={`${actual[0].toFixed(2)} m/s  ${(actual[1] * 180 / Math.PI).toFixed(1)} deg`}
                    color={['#f43f5e', '#fb923c', '#facc15', '#4ade80'][i]}
                  />
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {view === 'table' && (
        <div ref={tableRef} className="flex-1 min-h-0 overflow-y-auto overflow-x-auto">
          <table className="telem-table text-[9px]">
            <thead>
              <tr>
                <th>T(s)</th>
                <th>M</th>
                {columns.pose && <th>X</th>}
                {columns.pose && <th>Y</th>}
                {columns.heading && <th>H deg</th>}
                {columns.modules && <th>FL_v</th>}
                {columns.modules && <th>FR_v</th>}
                {columns.modules && <th>RR_v</th>}
                {columns.modules && <th>RL_v</th>}
                {columns.battery && <th>Batt</th>}
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((entry, index) => {
                const absoluteIndex = visibleStartIndex + index;
                const inRange = analysisRange
                  ? absoluteIndex >= analysisRange.startIndex && absoluteIndex <= analysisRange.endIndex
                  : false;
                const marked = bookmarks.includes(absoluteIndex);
                return (
                  <tr
                    key={entry.timestamp + index}
                    onClick={() => onJumpToIndex(absoluteIndex)}
                    className={[
                      'cursor-pointer',
                      absoluteIndex === scrubIndex ? 'bg-scope-surface' : '',
                      inRange ? 'bg-scope-surface/60' : '',
                    ].join(' ')}
                  >
                    <td className="readout">{entry.timestamp.toFixed(2)}</td>
                    <td className="readout">{marked ? '*' : inRange ? 'R' : ''}</td>
                    {columns.pose && <td className="readout">{entry.x.toFixed(2)}</td>}
                    {columns.pose && <td className="readout">{entry.y.toFixed(2)}</td>}
                    {columns.heading && <td className="readout">{(entry.heading * 180 / Math.PI).toFixed(1)}</td>}
                    {columns.modules && [0, 1, 2, 3].map(j => (
                      <td key={j} className="readout">{(entry.actuals[j]?.[0] ?? 0).toFixed(2)}</td>
                    ))}
                    {columns.battery && <td className="readout">{entry.batteryVoltage.toFixed(1)}</td>}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {view === 'raw' && (
        <div className="flex-1 min-h-0 overflow-y-auto overflow-x-auto p-2 font-mono text-[9px] text-scope-text bg-scope-bg">
          {frame ? (
            <pre className="whitespace-pre-wrap break-all">
              {JSON.stringify(frame, null, 2)}
            </pre>
          ) : (
            <span className="text-scope-muted">No frame</span>
          )}
        </div>
      )}
    </div>
  );
}
