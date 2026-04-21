// InspectorPanel.tsx — telemetry inspection table with recording & export
import { useState, useRef, useEffect, useCallback } from 'react';
import type { TelemetryFrame } from '../types/telemetry';

interface InspectorProps {
  history: TelemetryFrame[];
  historyLength: number;
  currentFrame: TelemetryFrame | null;
  isLive: boolean;
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

// ── Recording ──────────────────────────────────────────────────────────────
function useRecorder(history: TelemetryFrame[]) {
  const [recording, setRecording] = useState(false);
  const startIdxRef = useRef(0);

  const startRecording = useCallback(() => {
    startIdxRef.current = history.length;
    setRecording(true);
  }, [history.length]);

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
    const rows = frames.map(f => [
      f.timestamp.toFixed(4), f.x.toFixed(4), f.y.toFixed(4), f.heading.toFixed(4),
      ...[0,1,2,3].flatMap(i => [
        (f.actuals[i]?.[0] ?? 0).toFixed(4),
        (f.actuals[i]?.[1] ?? 0).toFixed(4),
      ]),
      f.batteryVoltage.toFixed(3), f.loopTimeMs.toFixed(2),
      f.isSnapping ? '1' : '0', f.isMaintaining ? '1' : '0',
    ].join(','));

    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url;
    a.download = `swervescope_${new Date().toISOString().slice(0,19).replace(/:/g,'-')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [history]);

  return { recording, startRecording, stopAndExport };
}

export default function InspectorPanel({ history, historyLength, currentFrame, isLive }: InspectorProps) {
  const [view, setView] = useState<ViewMode>('live');
  const tableRef = useRef<HTMLDivElement>(null);
  const { recording, startRecording, stopAndExport } = useRecorder(history);

  const f = currentFrame;

  // Auto-scroll raw table to bottom when live
  useEffect(() => {
    if (isLive && view === 'table' && tableRef.current) {
      tableRef.current.scrollTop = tableRef.current.scrollHeight;
    }
  }, [historyLength, isLive, view]);

  return (
    <div className="flex flex-col h-full bg-scope-bg">
      {/* Toolbar */}
      <div className="flex items-center gap-2 px-3 py-1.5 border-b border-scope-border flex-shrink-0">
        {/* View toggle */}
        <div className="flex bg-scope-surface border border-scope-border rounded overflow-hidden text-[9px] font-mono">
          {(['live', 'table', 'raw'] as ViewMode[]).map(v => (
            <button
              key={v}
              onClick={() => setView(v)}
              className={`px-2 py-1 uppercase tracking-wide transition-colors ${
                view === v ? 'bg-scope-accent text-white' : 'text-scope-muted hover:text-scope-text'
              }`}
            >
              {v}
            </button>
          ))}
        </div>

        <div className="flex-1" />

        {/* Record / export */}
        {recording ? (
          <button onClick={stopAndExport} className="btn btn-danger text-[9px]">
            ■ stop+export
          </button>
        ) : (
          <button onClick={startRecording} className="btn btn-success text-[9px]">
            ● record
          </button>
        )}
        {recording && (
          <span className="text-[9px] font-mono text-red-400 animate-pulse">
            {history.length} frames
          </span>
        )}
      </div>

      {/* Live key-value view */}
      {view === 'live' && (
        <div className="flex-1 overflow-y-auto">
          <table className="w-full border-collapse">
            <tbody>
              <Divider label="Odometry" />
              <Row label="Field X"   value={f?.x ?? 0}              unit="m" />
              <Row label="Field Y"   value={f?.y ?? 0}              unit="m" />
              <Row label="Heading"   value={(f?.heading ?? 0) * 180 / Math.PI} unit="°" />

              <Divider label="Controller" />
              <Row label="Mode"
                value={f?.isSnapping ? 'SNAP' : f?.isMaintaining ? 'HOLD' : 'MANUAL'}
                color={f?.isSnapping ? '#a855f7' : f?.isMaintaining ? '#0ea5e9' : '#4a6a8a'}
              />
              <Row label="Snap Target" value={(f?.snapTargetRad ?? 0) * 180 / Math.PI} unit="°" />

              <Divider label="Inputs" />
              <Row label="Drive X"   value={f?.driveX ?? 0} />
              <Row label="Drive Y"   value={f?.driveY ?? 0} />
              <Row label="Turn"      value={f?.turn ?? 0} />

              <Divider label="System" />
              <Row label="Battery"   value={f?.batteryVoltage ?? 0} unit="V"  color="#f59e0b" />
              <Row label="Loop Time" value={f?.loopTimeMs ?? 0}     unit="ms" color={
                (f?.loopTimeMs ?? 0) > 25 ? '#ef4444' : '#10b981'
              } />

              <Divider label="Modules (actual speed / angle)" />
              {[0,1,2,3].map(i => {
                const a = f?.actuals?.[i] ?? [0,0];
                const t = f?.targets?.[i] ?? [0,0];
                return (
                  <Row
                    key={i}
                    label={['FL','FR','RR','RL'][i]}
                    value={`${a[0].toFixed(2)} m/s  ${(a[1]*180/Math.PI).toFixed(1)}°`}
                    color={['#f43f5e','#fb923c','#facc15','#4ade80'][i]}
                  />
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Rolling table view */}
      {view === 'table' && (
        <div ref={tableRef} className="flex-1 overflow-auto">
          <table className="telem-table text-[9px]">
            <thead>
              <tr>
                <th>T(s)</th>
                <th>X</th><th>Y</th><th>H°</th>
                <th>FL_v</th><th>FR_v</th><th>RR_v</th><th>RL_v</th>
                <th>Batt</th>
              </tr>
            </thead>
            <tbody>
              {history.slice(-200).map((fr, i) => (
                <tr key={i}>
                  <td className="readout">{fr.timestamp.toFixed(2)}</td>
                  <td className="readout">{fr.x.toFixed(2)}</td>
                  <td className="readout">{fr.y.toFixed(2)}</td>
                  <td className="readout">{(fr.heading * 180 / Math.PI).toFixed(1)}</td>
                  {[0,1,2,3].map(j => (
                    <td key={j} className="readout">{(fr.actuals[j]?.[0] ?? 0).toFixed(2)}</td>
                  ))}
                  <td className="readout">{fr.batteryVoltage.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Raw JSON */}
      {view === 'raw' && (
        <div className="flex-1 overflow-auto p-2 font-mono text-[9px] text-scope-text bg-scope-bg">
          {f ? (
            <pre className="whitespace-pre-wrap break-all">
              {JSON.stringify(f, null, 2)}
            </pre>
          ) : (
            <span className="text-scope-muted">No frame</span>
          )}
        </div>
      )}
    </div>
  );
}
