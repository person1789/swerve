import { useEffect, useRef, useState, useMemo } from 'react';
import { Settings2, X, Plus } from 'lucide-react';
import { flattenTelemetryNumbers, normalizeTelemetryFrame } from '../lib/schemas';

interface GraphViewProps {
  telemetry: Record<string, unknown>;
}

interface Plot {
  key: string;
  color: string;
  maxY: number;
  autoScale: boolean;
  axis: 'left' | 'right';
}

interface HistoryFrame {
  _time: number;
  _markers?: string[];
  [key: string]: number | string[] | undefined;
}

const COLORS = ['#00e676', '#7c4dff', '#ff9100', '#00e5ff', '#ff5252', '#ffd740'];
const DERIVED_OPTIONS = ['derived.linearVelocityMagnitude', 'derived.translationDirectionDeg'];

export function GraphView({ telemetry }: GraphViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const dataRef = useRef<HistoryFrame[]>([]);
  const startTime = useRef(Date.now());

  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [plots, setPlots] = useState<Plot[]>([
    { key: 'actualVelocity.linearSpeed', color: '#00e676', maxY: 3.0, autoScale: true, axis: 'left' },
    { key: 'actualVelocity.omega', color: '#ff9100', maxY: 6.0, autoScale: true, axis: 'right' },
  ]);
  const [showSettings, setShowSettings] = useState(false);

  const availableKeys = useMemo(() => {
    const telemetryKeys = Object.keys(flattenTelemetryNumbers(telemetry));
    return Array.from(new Set([...telemetryKeys, ...DERIVED_OPTIONS])).sort();
  }, [telemetry]);

  useEffect(() => {
    const frame: HistoryFrame = { _time: Date.now() - startTime.current };
    Object.assign(frame, flattenTelemetryNumbers(telemetry));

    const velocity = telemetry.actualVelocity as Record<string, number> | undefined;
    const vx = Number(velocity?.x ?? 0);
    const vy = Number(velocity?.y ?? 0);
    frame['derived.linearVelocityMagnitude'] = Math.hypot(vx, vy);
    frame['derived.translationDirectionDeg'] = Math.atan2(vy, vx) * 180 / Math.PI;
    frame._markers = normalizeTelemetryFrame(telemetry, 'sim', frame._time).markers;

    dataRef.current.push(frame);
    if (dataRef.current.length > 1000) dataRef.current.shift();
  }, [telemetry]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const ro = new ResizeObserver(entries => {
      if (entries[0]) {
        const { width, height } = entries[0].contentRect;
        setDimensions({ width, height });
      }
    });
    ro.observe(container);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    let animationId: number;
    const draw = () => {
      const canvas = canvasRef.current;
      if (!canvas || dimensions.width === 0) return;

      const dpr = window.devicePixelRatio || 1;
      canvas.width = dimensions.width * dpr;
      canvas.height = dimensions.height * dpr;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.scale(dpr, dpr);

      const width = dimensions.width;
      const height = dimensions.height;
      ctx.clearRect(0, 0, width, height);

      const data = dataRef.current;
      if (data.length < 2) {
        animationId = requestAnimationFrame(draw);
        return;
      }

      const now = Date.now() - startTime.current;
      const timeWindow = 10000;
      const minTime = now - timeWindow;
      const history = data.filter(d => d._time >= minTime);
      if (history.length < 2) {
        animationId = requestAnimationFrame(draw);
        return;
      }

      ctx.strokeStyle = 'rgba(255, 255, 255, 0.03)';
      ctx.lineWidth = 1;
      for (let i = 1; i < 4; i++) {
        const y = (height / 4) * i;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
      }

      const leftPlots = plots.filter(plot => plot.axis === 'left');
      const rightPlots = plots.filter(plot => plot.axis === 'right');

      const axisMax = (axisPlots: Plot[]) => {
        if (axisPlots.length === 0) return 1;
        const values = axisPlots.flatMap(plot => history.map(point => Math.abs(Number(point[plot.key] || 0))));
        const rawMax = Math.max(1, ...values);
        return rawMax * 1.15;
      };

      const leftMax = axisMax(leftPlots);
      const rightMax = axisMax(rightPlots);

      const drawPlot = (plot: Plot, plotIdx: number) => {
        const currentMax = plot.autoScale ? (plot.axis === 'left' ? leftMax : rightMax) : plot.maxY;
        const hasNegatives = history.some(d => Number(d[plot.key] || 0) < 0);

        ctx.beginPath();
        ctx.strokeStyle = plot.color;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';

        history.forEach((point, i) => {
          const x = ((point._time - minTime) / timeWindow) * width;
          const val = Number(point[plot.key] || 0);
          const y = hasNegatives
            ? (height / 2) - (val / currentMax) * (height / 2)
            : height - (val / currentMax) * height;
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();

        ctx.fillStyle = plot.color;
        ctx.font = `600 10px var(--font-sans)`;
        ctx.fillText(`${plot.axis.toUpperCase()} ${plot.key.split('.').pop()}: ${Number(history[history.length - 1][plot.key] || 0).toFixed(2)}`, 10, 18 + plotIdx * 16);
      };

      plots.forEach(drawPlot);

      history.forEach((point) => {
        if (!point._markers || point._markers.length === 0) return;
        const x = ((point._time - minTime) / timeWindow) * width;
        ctx.strokeStyle = 'rgba(255,255,255,0.15)';
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();

        ctx.fillStyle = 'rgba(255,255,255,0.65)';
        ctx.font = '9px var(--font-sans)';
        ctx.fillText(point._markers[0], x + 4, height - 8);
      });

      ctx.fillStyle = 'rgba(255,255,255,0.3)';
      ctx.font = '10px var(--font-sans)';
      ctx.fillText(`L ${leftMax.toFixed(2)}`, width - 48, 16);
      ctx.fillText(`R ${rightMax.toFixed(2)}`, width - 48, 30);

      animationId = requestAnimationFrame(draw);
    };

    draw();
    return () => cancelAnimationFrame(animationId);
  }, [dimensions, plots]);

  return (
    <div ref={containerRef} style={{ width: '100%', height: '100%', position: 'relative', overflow: 'hidden' }}>
      <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />

      <button
        onClick={() => setShowSettings(!showSettings)}
        style={{
          position: 'absolute', top: '0.5rem', right: '0.5rem',
          background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
          color: 'white', padding: '4px', borderRadius: '4px', cursor: 'pointer',
        }}
      >
        <Settings2 size={14} />
      </button>

      {showSettings && (
        <div style={{
          position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(15,16,25,0.95)', backdropFilter: 'blur(10px)',
          padding: '1rem', overflow: 'auto', zIndex: 10,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--accent)' }}>Graph Configuration</span>
            <X size={16} onClick={() => setShowSettings(false)} style={{ cursor: 'pointer' }} />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {plots.map((plot, i) => (
              <div key={`${plot.key}-${i}`} style={{ display: 'grid', gridTemplateColumns: '1.8fr auto auto auto', gap: '0.5rem', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.03)', padding: '0.5rem', borderRadius: '6px' }}>
                <select
                  value={plot.key}
                  onChange={e => setPlots(prev => prev.map((p, j) => j === i ? { ...p, key: e.target.value } : p))}
                  style={selectStyle}
                >
                  {availableKeys.map(k => <option key={k} value={k}>{k}</option>)}
                </select>
                <select
                  value={plot.axis}
                  onChange={e => setPlots(prev => prev.map((p, j) => j === i ? { ...p, axis: e.target.value as 'left' | 'right' } : p))}
                  style={smallSelectStyle}
                >
                  <option value="left">Left</option>
                  <option value="right">Right</option>
                </select>
                <input
                  type="color"
                  value={plot.color}
                  onChange={e => setPlots(prev => prev.map((p, j) => j === i ? { ...p, color: e.target.value } : p))}
                  style={{ width: '24px', height: '24px', border: 'none', background: 'none', cursor: 'pointer' }}
                />
                <button onClick={() => setPlots(prev => prev.filter((_, j) => j !== i))} style={iconBtn}><X size={12} /></button>
              </div>
            ))}
            <button
              onClick={() => setPlots(prev => [...prev, { key: availableKeys[0] || 'actualVelocity.linearSpeed', color: COLORS[prev.length % COLORS.length], maxY: 1.0, autoScale: true, axis: prev.length % 2 === 0 ? 'left' : 'right' }])}
              style={{ ...modeBtn, alignSelf: 'flex-start', marginTop: '0.5rem' }}
            >
              <Plus size={14} /> Add Plot
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

const selectStyle: React.CSSProperties = {
  flex: 1, backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
  color: 'white', fontSize: '0.7rem', padding: '0.25rem', borderRadius: '4px', outline: 'none',
};

const smallSelectStyle: React.CSSProperties = {
  ...selectStyle,
  width: 72,
  flex: 'unset',
};

const iconBtn: React.CSSProperties = {
  background: 'none', border: 'none', color: 'rgba(255,255,255,0.3)', cursor: 'pointer', padding: '4px',
};

const modeBtn: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: '4px',
  padding: '0.3rem 0.6rem', fontSize: '0.7rem', fontWeight: 600,
  border: '1px solid rgba(255,255,255,0.1)', borderRadius: '6px', cursor: 'pointer',
  background: 'rgba(124,77,255,0.1)', color: '#b388ff',
};
