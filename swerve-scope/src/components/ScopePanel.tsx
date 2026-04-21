import { useEffect, useRef, useCallback, useState, useMemo } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import type { AnalysisRange, DerivedField } from '../types/telemetry';
import { evalExpr, BUILTIN_EXPRS } from '../lib/ExprEval';
import { useHistoryBuffer } from '../hooks/useTelemetry';

interface ScopePanelProps {
  activeFields: string[];
  customFields: DerivedField[];
  scrubIndex: number;
  analysisRange: AnalysisRange | null;
  bookmarks: number[];
  onToggleField: (fieldId: string) => void;
  onSetRange: (range: AnalysisRange | null) => void;
  onClearRange: () => void;
  onJumpToIndex: (index: number) => void;
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
      dash: field.unit ? undefined : [6, 4],
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

function makeRange(startIndex: number, endIndex: number): AnalysisRange {
  return {
    startIndex: Math.min(startIndex, endIndex),
    endIndex: Math.max(startIndex, endIndex),
  };
}

export default function ScopePanel({
  activeFields,
  customFields,
  scrubIndex,
  analysisRange,
  bookmarks,
  onToggleField,
  onSetRange,
  onClearRange,
  onJumpToIndex,
}: ScopePanelProps) {
  const [windowSec, setWindowSec] = useState(30);
  const containerRef = useRef<HTMLDivElement>(null);
  const overlayRef = useRef<HTMLDivElement>(null);
  const plotRef = useRef<uPlot | null>(null);
  const roRef = useRef<ResizeObserver | null>(null);
  const dragStartRef = useRef<number | null>(null);
  const dragMovedRef = useRef(false);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const { buffer: history, length: historyLength } = useHistoryBuffer();

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

  const visibleHistory = useMemo(
    () => history.slice(-Math.round(windowSec * 50)),
    [historyLength, history, windowSec],
  );

  const visibleStartIndex = useMemo(
    () => Math.max(0, historyLength - visibleHistory.length),
    [historyLength, visibleHistory.length],
  );

  const activeRange = useMemo(() => {
    if (!analysisRange || historyLength === 0) return null;
    return makeRange(
      Math.max(0, Math.min(analysisRange.startIndex, historyLength - 1)),
      Math.max(0, Math.min(analysisRange.endIndex, historyLength - 1)),
    );
  }, [analysisRange, historyLength]);

  const analysisSlice = useMemo(() => {
    if (activeRange) {
      return history.slice(activeRange.startIndex, activeRange.endIndex + 1);
    }
    return visibleHistory;
  }, [history, activeRange, visibleHistory]);

  useEffect(() => {
    if (!plotRef.current || allFields.length === 0) return;
    const times = visibleHistory.map(frame => frame.timestamp);
    const series = allFields.map(field => visibleHistory.map(frame => evalExpr(field.expr, frame)));
    plotRef.current.setData([times, ...series] as uPlot.AlignedData);
  }, [visibleHistory, allFields]);

  const stats = useMemo(() => allFields.map(field => {
    const values = analysisSlice
      .map(frame => evalExpr(field.expr, frame))
      .filter(value => Number.isFinite(value));

    if (values.length === 0) {
      return { id: field.id, min: 0, max: 0, mean: 0, last: 0, rms: 0 };
    }

    const sum = values.reduce((acc, value) => acc + value, 0);
    const rms = Math.sqrt(values.reduce((acc, value) => acc + value * value, 0) / values.length);
    return {
      id: field.id,
      min: Math.min(...values),
      max: Math.max(...values),
      mean: sum / values.length,
      last: values[values.length - 1],
      rms,
    };
  }), [allFields, analysisSlice]);

  const unitGroups = useMemo(() => {
    const map = new Map<string, string[]>();
    for (const field of allFields) {
      const key = field.unit || 'unitless';
      const existing = map.get(key) ?? [];
      existing.push(field.name);
      map.set(key, existing);
    }
    return Array.from(map.entries());
  }, [allFields]);

  const getIndexFromClientX = useCallback((clientX: number) => {
    const overlay = overlayRef.current;
    if (!overlay || visibleHistory.length === 0) return null;
    const rect = overlay.getBoundingClientRect();
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width));
    const ratio = rect.width <= 1 ? 0 : x / rect.width;
    const visibleOffset = Math.round(ratio * Math.max(visibleHistory.length - 1, 0));
    return visibleStartIndex + visibleOffset;
  }, [visibleHistory.length, visibleStartIndex]);

  const rangeOverlay = useMemo(() => {
    if (!activeRange || visibleHistory.length === 0) return null;
    const visibleEndIndex = visibleStartIndex + visibleHistory.length - 1;
    if (activeRange.endIndex < visibleStartIndex || activeRange.startIndex > visibleEndIndex) return null;
    const start = Math.max(activeRange.startIndex, visibleStartIndex);
    const end = Math.min(activeRange.endIndex, visibleEndIndex);
    const leftPct = ((start - visibleStartIndex) / Math.max(visibleHistory.length - 1, 1)) * 100;
    const rightPct = ((end - visibleStartIndex) / Math.max(visibleHistory.length - 1, 1)) * 100;
    return {
      leftPct,
      widthPct: Math.max(0.8, rightPct - leftPct),
    };
  }, [activeRange, visibleHistory.length, visibleStartIndex]);

  const scrubPct = visibleHistory.length > 1
    ? ((scrubIndex - visibleStartIndex) / Math.max(visibleHistory.length - 1, 1)) * 100
    : 0;

  const bookmarkPcts = useMemo(
    () => bookmarks
      .filter(index => index >= visibleStartIndex && index < visibleStartIndex + visibleHistory.length)
      .map(index => ({
        index,
        pct: ((index - visibleStartIndex) / Math.max(visibleHistory.length - 1, 1)) * 100,
      })),
    [bookmarks, visibleHistory.length, visibleStartIndex],
  );

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    const index = getIndexFromClientX(event.clientX);
    if (index === null) return;
    dragStartRef.current = index;
    dragMovedRef.current = false;
    setHoverIndex(index);
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const index = getIndexFromClientX(event.clientX);
    if (index === null) return;
    setHoverIndex(index);
    if (dragStartRef.current === null) return;
    if (dragStartRef.current !== index) dragMovedRef.current = true;
    onSetRange(makeRange(dragStartRef.current, index));
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    const index = getIndexFromClientX(event.clientX);
    if (index !== null) {
      if (dragStartRef.current !== null && dragMovedRef.current) {
        onSetRange(makeRange(dragStartRef.current, index));
      } else {
        onJumpToIndex(index);
      }
    }
    dragStartRef.current = null;
    dragMovedRef.current = false;
    event.currentTarget.releasePointerCapture(event.pointerId);
  };

  return (
    <div className="flex w-full h-full bg-scope-bg min-h-0 overflow-hidden">
      <div className="w-56 border-r border-scope-border flex flex-col min-h-0 flex-shrink-0 overflow-hidden">
        <div className="flex items-center justify-between px-3 py-2 border-b border-scope-border">
          <span className="text-[9px] font-mono uppercase tracking-wide text-scope-muted">Field Registry</span>
          <span className="text-[9px] font-mono text-scope-text readout">{activeFields.length}</span>
        </div>

        <div className="px-3 py-2 border-b border-scope-border">
          <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">Analysis</div>
          <div className="flex flex-wrap gap-1">
            <button className="btn btn-ghost text-[8px] py-0.5 px-2" onClick={() => onSetRange(makeRange(Math.max(0, scrubIndex - 50), scrubIndex))}>
              last 1s
            </button>
            <button className="btn btn-ghost text-[8px] py-0.5 px-2" onClick={() => onSetRange(makeRange(Math.max(0, scrubIndex - 250), scrubIndex))}>
              last 5s
            </button>
            <button className="btn btn-ghost text-[8px] py-0.5 px-2" onClick={() => {
              const start = Math.max(0, historyLength - visibleHistory.length);
              const end = Math.max(0, historyLength - 1);
              onSetRange(historyLength > 0 ? makeRange(start, end) : null);
            }}>
              use view
            </button>
            <button className="btn btn-ghost text-[8px] py-0.5 px-2" onClick={() => onSetRange(makeRange(0, scrubIndex))} disabled={historyLength === 0}>
              0..scrub
            </button>
            <button className="btn btn-ghost text-[8px] py-0.5 px-2" onClick={onClearRange} disabled={!activeRange}>
              clear
            </button>
          </div>
          <div className="mt-2 text-[8px] font-mono text-scope-muted">
            range: <span className="text-scope-text readout">{activeRange ? `${activeRange.startIndex}..${activeRange.endIndex}` : 'visible window'}</span>
          </div>
          <div className="mt-1 text-[8px] font-mono text-scope-muted">
            samples: <span className="text-scope-text readout">{analysisSlice.length}</span>
          </div>
          {hoverIndex !== null && (
            <div className="mt-1 text-[8px] font-mono text-scope-muted">
              hover: <span className="text-scope-text readout">#{hoverIndex}</span>
            </div>
          )}
        </div>

        <div className="flex-1 overflow-y-auto min-h-0">
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

          <div className="px-3 py-2 border-b border-scope-border">
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

          <div className="px-3 py-2">
            <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">Bookmarks</div>
            {bookmarks.length === 0 ? (
              <div className="text-[8px] font-mono text-scope-muted">No bookmarks yet.</div>
            ) : (
              <div className="flex flex-wrap gap-1">
                {bookmarks.map(index => (
                  <button
                    key={index}
                    onClick={() => onJumpToIndex(index)}
                    className={`btn text-[8px] py-0.5 px-2 ${index === scrubIndex ? 'btn-accent' : 'btn-ghost'}`}
                  >
                    #{index}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex-1 min-h-0 min-w-0 flex flex-col overflow-hidden">
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

        {unitGroups.length > 0 && (
          <div className="px-3 py-1 border-b border-scope-border flex-shrink-0 text-[8px] font-mono text-scope-muted flex flex-wrap gap-x-3 gap-y-1">
            {unitGroups.map(([unit, names]) => (
              <span key={unit}>
                axis[{unit}] = {names.join(', ')}
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
            <div className="relative flex-1 min-h-0 min-w-0 w-full overflow-hidden">
              <div ref={containerRef} className="absolute inset-0" />
              <div
                ref={overlayRef}
                className="absolute inset-0 cursor-crosshair"
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerLeave={() => setHoverIndex(null)}
              >
                {rangeOverlay && (
                  <div
                    className="absolute top-0 bottom-0 bg-scope-accent/10 border-x border-scope-accent/40 pointer-events-none"
                    style={{ left: `${rangeOverlay.leftPct}%`, width: `${rangeOverlay.widthPct}%` }}
                  />
                )}
                <div
                  className="absolute top-0 bottom-0 w-px bg-white/30 pointer-events-none"
                  style={{ left: `${Math.max(0, Math.min(100, scrubPct))}%` }}
                />
                {bookmarkPcts.map(bookmark => (
                  <div
                    key={bookmark.index}
                    className="absolute top-0 bottom-0 w-px bg-amber-300/70 pointer-events-none"
                    style={{ left: `${bookmark.pct}%` }}
                  />
                ))}
              </div>
            </div>
            <div className="border-t border-scope-border px-3 py-2 flex-shrink-0 overflow-x-auto">
              <div className="text-[8px] font-mono uppercase tracking-widest text-scope-muted mb-2">
                {activeRange ? 'Selected Range Stats' : 'Visible Window Stats'}
              </div>
              <div className="grid grid-cols-2 xl:grid-cols-4 gap-2 min-w-max">
                {allFields.map(field => {
                  const stat = stats.find(item => item.id === field.id);
                  return (
                    <div key={field.id} className="rounded border border-scope-border bg-scope-surface px-2 py-1.5 min-w-[140px]">
                      <div className="text-[9px] font-mono truncate" style={{ color: field.color }}>{field.name}</div>
                      <div className="mt-1 grid grid-cols-2 gap-x-3 gap-y-0.5 text-[8px] font-mono">
                        <span className="text-scope-muted">last</span>
                        <span className="text-right readout text-scope-text">{stat?.last.toFixed(3)}</span>
                        <span className="text-scope-muted">mean</span>
                        <span className="text-right readout text-scope-text">{stat?.mean.toFixed(3)}</span>
                        <span className="text-scope-muted">rms</span>
                        <span className="text-right readout text-scope-text">{stat?.rms.toFixed(3)}</span>
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
