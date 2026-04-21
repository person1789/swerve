// ScopePanel.tsx — uPlot oscilloscope with multi-series + expression fields
import { useEffect, useRef, useCallback, useState } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import type { TelemetryFrame, DerivedField } from '../types/telemetry';
import { evalExpr, BUILTIN_EXPRS } from '../lib/ExprEval';

interface ScopePanelProps {
  history: TelemetryFrame[];
  isLive: boolean;
  activeFields: string[];            // field ids from BUILTIN_EXPRS or custom
  customFields: DerivedField[];
  windowSec: number;                 // how many seconds of data to show
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
      grid:   { stroke: '#1a2535', width: 1 },
      ticks:  { stroke: '#1a2535', width: 1 },
      font:   '9px JetBrains Mono, monospace',
      labelFont: '9px JetBrains Mono, monospace',
    },
    {
      stroke: '#4a6a8a',
      grid:   { stroke: '#1a2535', width: 1 },
      ticks:  { stroke: '#1a2535', width: 1 },
      font:   '9px JetBrains Mono, monospace',
    },
  ],
  scales: {
    x: { time: false },
  },
};

function buildSeries(fields: DerivedField[]): uPlot.Series[] {
  return [
    {},  // time axis placeholder
    ...fields.map(f => ({
      label:  f.name,
      stroke: f.color,
      width:  1.5,
      spanGaps: true,
    })),
  ];
}

export default function ScopePanel({
  history,
  isLive,
  activeFields,
  customFields,
  windowSec,
}: ScopePanelProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const plotRef      = useRef<uPlot | null>(null);
  const roRef        = useRef<ResizeObserver | null>(null);

  // Merge builtin + custom fields, filter to active
  const allFields: DerivedField[] = [
    ...BUILTIN_EXPRS,
    ...customFields,
  ].filter(f => activeFields.includes(f.id));

  // Destroy + recreate plot when fields change
  const buildPlot = useCallback(() => {
    if (!containerRef.current) return;
    plotRef.current?.destroy();

    const w = containerRef.current.clientWidth;
    const h = containerRef.current.clientHeight;
    if (w < 10 || h < 10) return;

    const opts: uPlot.Options = {
      ...UPLOT_OPTS_BASE,
      width:  w,
      height: h,
      series: buildSeries(allFields),
    } as uPlot.Options;

    const data: uPlot.AlignedData = [[], ...allFields.map(() => [] as number[])];
    plotRef.current = new uPlot(opts, data, containerRef.current);
  }, [allFields]);

  // Setup resize observer
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

  // Feed data
  useEffect(() => {
    if (!plotRef.current || allFields.length === 0) return;

    const maxPts = Math.round(windowSec * 50); // 50 Hz
    const slice  = history.slice(-maxPts);

    const times = slice.map(f => f.timestamp);
    const series = allFields.map(field =>
      slice.map(frame => evalExpr(field.expr, frame)),
    );

    plotRef.current.setData([times, ...series] as uPlot.AlignedData);
  }, [history, isLive, allFields, windowSec]);

  return (
    <div className="flex flex-col w-full h-full bg-scope-bg">
      {/* Legend strip */}
      {allFields.length > 0 && (
        <div className="flex flex-wrap gap-x-4 gap-y-1 px-3 py-1 border-b border-scope-border flex-shrink-0">
          {allFields.map(f => (
            <span key={f.id} className="flex items-center gap-1 text-[9px] font-mono">
              <span className="w-4 h-0.5 inline-block rounded" style={{ background: f.color }} />
              <span style={{ color: f.color }}>{f.name}</span>
              {f.unit && <span className="text-scope-muted">({f.unit})</span>}
            </span>
          ))}
        </div>
      )}

      {allFields.length === 0 && (
        <div className="flex-1 flex items-center justify-center text-scope-muted text-[10px] font-mono">
          No fields selected — add fields in the Expression Editor
        </div>
      )}

      <div ref={containerRef} className="flex-1 min-h-0 w-full" />
    </div>
  );
}
