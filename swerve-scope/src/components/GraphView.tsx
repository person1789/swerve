import { useEffect, useRef, useState, useMemo } from 'react';
import { Settings2, X, Plus } from 'lucide-react';

interface GraphViewProps {
  telemetry: any;
}

interface Plot {
  key: string;
  color: string;
  maxY: number;
  autoScale: boolean;
}

const COLORS = ['#00e676', '#7c4dff', '#ff9100', '#00e5ff', '#ff5252', '#ffd740'];

export function GraphView({ telemetry }: GraphViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const dataRef = useRef<Record<string, number>[]>([]);
  const startTime = useRef(Date.now());
  
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [plots, setPlots] = useState<Plot[]>([
    { key: 'actualVelocity.linearSpeed', color: '#00e676', maxY: 3.0, autoScale: true },
    { key: 'actualVelocity.omega', color: '#ff9100', maxY: 6.0, autoScale: true }
  ]);
  const [showSettings, setShowSettings] = useState(false);

  // Flatten telemetry to get all available numerical keys
  const availableKeys = useMemo(() => {
    const keys: string[] = [];
    const flatten = (obj: any, prefix = '') => {
      for (const [k, v] of Object.entries(obj)) {
        const fullKey = prefix ? `${prefix}.${k}` : k;
        if (typeof v === 'number') {
          keys.add(fullKey);
        } else if (v && typeof v === 'object' && !Array.isArray(v)) {
          flatten(v, fullKey);
        }
      }
    };
    
    const keySet = new Set<string>();
    const flattenToSet = (obj: any, prefix = '') => {
      for (const [k, v] of Object.entries(obj)) {
        const fullKey = prefix ? `${prefix}.${k}` : k;
        if (typeof v === 'number') {
          keySet.add(fullKey);
        } else if (v && typeof v === 'object' && !Array.isArray(v)) {
          flattenToSet(v, fullKey);
        }
      }
    };
    flattenToSet(telemetry);
    return Array.from(keySet).sort();
  }, [telemetry]);

  // Record history
  useEffect(() => {
    const frame: Record<string, number> = { _time: Date.now() - startTime.current };
    
    const getVal = (path: string, obj: any): number => {
      return path.split('.').reduce((o, i) => (o ? o[i] : 0), obj) || 0;
    };

    // Store all numerical values found in telemetry to allow switching keys without losing history
    const flattenToFrame = (obj: any, prefix = '') => {
      for (const [k, v] of Object.entries(obj)) {
        const fullKey = prefix ? `${prefix}.${k}` : k;
        if (typeof v === 'number') frame[fullKey] = v;
        else if (v && typeof v === 'object' && !Array.isArray(v)) flattenToFrame(v, fullKey);
      }
    };
    flattenToFrame(telemetry);

    dataRef.current.push(frame);
    if (dataRef.current.length > 1000) dataRef.current.shift();
  }, [telemetry]);

  // Resize handling
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

  // Draw loop
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

      const { width, height } = canvas;
      ctx.clearRect(0, 0, width, height);

      const data = dataRef.current;
      if (data.length < 2) {
        animationId = requestAnimationFrame(draw);
        return;
      }

      const now = Date.now() - startTime.current;
      const timeWindow = 10000;
      const minTime = now - timeWindow;

      // Draw Grid
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.03)';
      ctx.lineWidth = 1;
      for (let i = 1; i < 4; i++) {
        const y = (height / 4) * i;
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(width, y); ctx.stroke();
      }

      // Draw Plots
      plots.forEach((plot, plotIdx) => {
        const history = data.filter(d => d._time >= minTime);
        if (history.length < 2) return;

        // Auto-scaling
        let currentMax = plot.maxY;
        if (plot.autoScale) {
          const values = history.map(d => Math.abs(d[plot.key] || 0));
          const maxVal = Math.max(...values);
          if (maxVal > 0) currentMax = maxVal * 1.2;
        }

        ctx.beginPath();
        ctx.strokeStyle = plot.color;
        ctx.lineWidth = 2 * dpr;
        ctx.lineJoin = 'round';

        history.forEach((point, i) => {
          const x = ((point._time - minTime) / timeWindow) * width;
          const val = point[plot.key] || 0;
          
          // Speed style (0-max) or Omega style (-max to max)
          // We'll treat all as center-relative if they have negative values, otherwise bottom-relative
          const hasNegatives = history.some(d => (d[plot.key] || 0) < 0);
          let y = 0;
          if (hasNegatives) {
            y = (height / 2) - (val / currentMax) * (height / 2);
          } else {
            y = height - (val / currentMax) * height;
          }

          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();

        // Legend entry
        ctx.fillStyle = plot.color;
        ctx.font = `600 ${10 * dpr}px var(--font-sans)`;
        ctx.fillText(`${plot.key.split('.').pop()}: ${(data[data.length-1][plot.key] || 0).toFixed(2)}`, 10 * dpr, (20 + plotIdx * 18) * dpr);
      });

      animationId = requestAnimationFrame(draw);
    };

    draw();
    return () => cancelAnimationFrame(animationId);
  }, [dimensions, plots]);

  return (
    <div ref={containerRef} style={{ width: '100%', height: '100%', position: 'relative', overflow: 'hidden' }}>
      <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />
      
      {/* Settings Button */}
      <button 
        onClick={() => setShowSettings(!showSettings)}
        style={{
          position: 'absolute', top: '0.5rem', right: '0.5rem',
          background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
          color: 'white', padding: '4px', borderRadius: '4px', cursor: 'pointer'
        }}
      >
        <Settings2 size={14} />
      </button>

      {/* Settings Modal */}
      {showSettings && (
        <div style={{
          position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(15,16,25,0.95)', backdropFilter: 'blur(10px)',
          padding: '1rem', overflow: 'auto', zIndex: 10
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--accent)' }}>Graph Configuration</span>
            <X size={16} onClick={() => setShowSettings(false)} style={{ cursor: 'pointer' }} />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {plots.map((plot, i) => (
              <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.03)', padding: '0.5rem', borderRadius: '6px' }}>
                <select 
                  value={plot.key}
                  onChange={e => setPlots(prev => prev.map((p, j) => j === i ? { ...p, key: e.target.value } : p))}
                  style={selectStyle}
                >
                  {availableKeys.map(k => <option key={k} value={k}>{k}</option>)}
                </select>
                <input 
                  type="color" value={plot.color} 
                  onChange={e => setPlots(prev => prev.map((p, j) => j === i ? { ...p, color: e.target.value } : p))}
                  style={{ width: '24px', height: '24px', border: 'none', background: 'none', cursor: 'pointer' }}
                />
                <button onClick={() => setPlots(prev => prev.filter((_, j) => j !== i))} style={iconBtn}><X size={12} /></button>
              </div>
            ))}
            <button 
              onClick={() => setPlots(prev => [...prev, { key: availableKeys[0] || '', color: COLORS[prev.length % COLORS.length], maxY: 1.0, autoScale: true }])}
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
  color: 'white', fontSize: '0.7rem', padding: '0.25rem', borderRadius: '4px', outline: 'none'
};

const iconBtn: React.CSSProperties = {
  background: 'none', border: 'none', color: 'rgba(255,255,255,0.3)', cursor: 'pointer', padding: '4px'
};

const modeBtn: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: '4px',
  padding: '0.3rem 0.6rem', fontSize: '0.7rem', fontWeight: 600,
  border: '1px solid rgba(255,255,255,0.1)', borderRadius: '6px', cursor: 'pointer',
  background: 'rgba(124,77,255,0.1)', color: '#b388ff',
};

