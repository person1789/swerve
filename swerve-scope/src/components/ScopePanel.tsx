import { useEffect, useRef, useCallback, useState, useMemo } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import type { TelemetryFrame, DerivedField } from '../types/telemetry';
import { evalExpr, BUILTIN_EXPRS } from '../lib/ExprEval';

interface ScopePanelProps {
  history: TelemetryFrame[];
  historyLength: number;
  isLive: boolean;
  activeFields: string[];
  customFields: DerivedField[];
  onToggleField: (fieldId: string) => void;
}

const UPLOT_OPTS_BASE: Partial<uPlot.Options> = {
  padding: [8, 8, 0, 0],
  cursor: {
    show: true,
    sync: { key: 'scope' },
  },
  legend: { show: false },
  axes: [
    {
      stroke: '#2a3a52',
      grid: { stroke: '#1a2535', width: 1 },
      ticks: { stroke: '#1a2535', width: 1 },
      font: '9px JetBrains Mono, monospace',
      labelFont: '9px JetBrains Mono, monospace',
    },
    {
      stroke: '#4a6a8a',
      grid: { stroke: '#1a2535', width: 1 },
      ticks: { stroke: '#1a2535', width: 1 },
      font: '9px JetBrains Mono, monospace',
    },
  ],
  scales: {
    x: { time: false },
  },
};

function buildSeries(fields: DerivedField[]): uPlot.Series[] {
  return [
    {},
    ...fields.map(field => ({
      label: field.name,
      stroke: field.color,
      width: 1.5,
      spanGaps: true,
    })),
  ];
}

function FieldToggle({
  field,
  active,
  onToggle,
}: {
  field: DerivedField;
  active: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      onClick={onToggle}
      className={`text-left px-2 py-1 rounded border text-[9px] font-mono transition-colors ${
        active ? 'border-scope-accent/60 bg-scope-surface' : 'border-scope-border hover:border-scope-accent/30'
      }`}
    >
      <div className="flex items-center gap-2">
        <span className="w-2.5 h-2.5 rounded-sm" style={{ background: field.color, opacity: active ? 1 : 0.35 }} />
        <span className="truncate" style={{ color: active ? field.color : '#8ba4c0' }}>{field.name}</span>
      </div>
      <div className="mt-1 text-[8px] text-scope-muted truncate">
        {field.unit || 'scalar'}  {field.expr}
      </div>
    </button>
  );
}

export default function ScopePanel({
  history,
  historyLength,
  isLive,
  activeFields,
  customFields,
  onToggleField,
}: ScopePanelProps) {
  const [windowSec, setWindowSec] = useState(30);
  const containerRef = useRef<HTMLDivElement>(null);
  const plotRef = useRef<uPlot | null>(null);
  const roRef = useRef<ResizeObserver | null>(null);

  const allFields: DerivedField[] = [
    ...BUILTIN_EXPRS,
    ...customFields,
  ].filter(field => activeFields.includes(field.id));

  const buildPlot = useCallback(() => {
    if (!containerRef.current) return;
    plotRef.current?.destroy();

    const w = containerRef.current.clientWidth;
    const h = containerRef.current.clientHeight;
    if (w < 10 || h < 10) return;

    const opts: uPlot.Options = {
      ...UPLOT_OPTS_BASE,
      width: w,
      height: h,
      series: buildSeries(allFields),
    } as uPlot.Options;

    const data: uPlot.AlignedData = [[], ...allFields.map(() => [] as number[])];
    plotRef.current = new uPlot(opts, data, containerRef.current);
  }, [allFields]);

  useEffect(() => {
    buildPlot();
    roRef.current = new ResizeObserver(() => {
      if (!containerRef.current || !plotRef.current) return;
      const w = containerRef.current.clientWidth;
      const h = containerRef.current.clientHeight;
      if (w > 10 && h > 10) plotRef.current.setSize({ width: w, height: h });
    });
    if (containerRef.current) roRef.current.observe(containerRef.current);
    return () => {
      roRef.current?.disconnect();
      plotRef.current?.destroy();
      plotRef.current = null;
    };
  }, [buildPlot]);

  useEffect(() => {
    if (!plotRef.current || allFields.length === 0) return;

    const maxPts = Math.round(windowSec * 50);
    const slice = history.slice(-maxPts);
    const times = slice.map(frame => frame.timestamp);
    const series = allFields.map(field => slice.map(frame => evalExpr(field.expr, frame)));

    plotRef.current.setData([times, ...series] as uPlot.AlignedData);
  }, [historyLength, isLive, allFields, windowSec, history]);

  const visibleHistory = useMemo(
    () => history.slice(-Math.round(windowSec * 50)),
    [historyLength, history, windowSec],
  );

  const stats = useMemo(() => allFields.map(field => {
    const values = visibleHistory
      .map(frame => evalExpr(field.expr, frame))
      .filter(value => Number.isFinite(value));

    if (values.length === 0) {
      return { id: field.id, min: 0, max: 0, mean: 0, last: 0 };
    }

    return {
      id: field.id,
      min: Math.min(...values),
      max: Math.max(...values),
      mean: values.reduce((acc, value) => acc + value, 0) / values.length,
      last: values[values.length - 1],
    };
  }), [allFields, visibleHistory]);

  return (
    <div className="flex w-full h-full bg-scope-bg min-h-0">
      <div className="w-52 border-r border-scope-border flex flex-col min-h-0 flex-shrink-0">
        <div className="flex items-center justify-between px-3 py-2 border-b border-scope-border">
          <span className="text-[9px] font-mono uppercase tracking-wide text-scope-muted">Field Registry</span>
          <span className="text-[9px] font-mono text-scope-text readout">{activeFields.length}</span>
        </div>

        <div className="flex-1 overflow-y-auto">
          <div className="px-3 py-2 border-b border-scope-border">
            <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">Built-in</div>
            <div className="flex flex-col gap-1">
              {BUILTIN_EXPRS.map(field => (
                <FieldToggle
                  key={field.id}
                  field={field}
                  active={activeFields.includes(field.id)}
                  onToggle={() => onToggleField(field.id)}
                />
              ))}
            </div>
          </div>

          <div className="px-3 py-2">
            <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">Custom</div>
            {customFields.length === 0 ? (
              <div className="text-[8px] font-mono text-scope-muted">Add derived fields in the Expressions pane.</div>
            ) : (
              <div className="flex flex-col gap-1">
                {customFields.map(field => (
                  <FieldToggle
                    key={field.id}
                    field={field}
                    active={activeFields.includes(field.id)}
                    onToggle={() => onToggleField(field.id)}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex-1 min-h-0 flex flex-col">
        <div className="flex items-center justify-between px-3 py-1 border-b border-scope-border flex-shrink-0">
          <div className="flex items-center gap-2">
            <label className="text-[9px] font-mono text-scope-muted">window</label>
            <select
              value={windowSec}
              onChange={event => setWindowSec(Number(event.target.value))}
              className="bg-scope-surface border border-scope-border rounded text-[9px] font-mono text-scope-text px-1 py-0.5"
            >
              {[5, 15, 30, 60].map(seconds => (
                <option key={seconds} value={seconds}>{seconds}s</option>
              ))}
            </select>
          </div>
          <span className="text-[9px] font-mono text-scope-muted">{visibleHistory.length} samples visible</span>
        </div>

        {allFields.length > 0 && (
          <div className="flex flex-wrap gap-x-4 gap-y-1 px-3 py-1 border-b border-scope-border flex-shrink-0">
            {allFields.map(field => (
              <span key={field.id} className="flex items-center gap-1 text-[9px] font-mono">
                <span className="w-4 h-0.5 inline-block rounded" style={{ background: field.color }} />
                <span style={{ color: field.color }}>{field.name}</span>
                {field.unit && <span className="text-scope-muted">({field.unit})</span>}
              </span>
            ))}
          </div>
        )}

        {allFields.length === 0 ? (
          <div className="flex-1 flex items-center justify-center text-scope-muted text-[10px] font-mono">
            Select fields from the registry or add new derived fields in Expressions
          </div>
        ) : (
          <>
            <div ref={containerRef} className="flex-1 min-h-0 w-full" />
            <div className="border-t border-scope-border px-3 py-2 flex-shrink-0">
              <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">Visible Window Stats</div>
              <div className="grid grid-cols-2 xl:grid-cols-4 gap-2">
                {allFields.map(field => {
                  const stat = stats.find(item => item.id === field.id);
                  return (
                    <div key={field.id} className="rounded border border-scope-border bg-scope-surface px-2 py-1.5">
                      <div className="text-[9px] font-mono truncate" style={{ color: field.color }}>{field.name}</div>
                      <div className="mt-1 grid grid-cols-2 gap-x-3 gap-y-0.5 text-[8px] font-mono">
                        <span className="text-scope-muted">last</span>
                        <span className="text-right readout text-scope-text">{stat?.last.toFixed(3)}</span>
                        <span className="text-scope-muted">mean</span>
                        <span className="text-right readout text-scope-text">{stat?.mean.toFixed(3)}</span>
                        <span className="text-scope-muted">min</span>
                        <span className="text-right readout text-scope-text">{stat?.min.toFixed(3)}</span>
                        <span className="text-scope-muted">max</span>
                        <span className="text-right readout text-scope-text">{stat?.max.toFixed(3)}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
