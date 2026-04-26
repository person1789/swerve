import { useEffect, useMemo, useRef, useState } from 'react';
import { Crosshair, Ghost, Navigation, Radar } from 'lucide-react';

interface FieldViewProps {
  telemetry: Record<string, unknown>;
}

interface TrailPoint {
  x: number;
  y: number;
}

export function FieldView({ telemetry }: FieldViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const trailRef = useRef<TrailPoint[]>([]);
  const observedTrailRef = useRef<TrailPoint[]>([]);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [hoverInches, setHoverInches] = useState<{ xIn: number; yIn: number } | null>(null);
  const [showCommandedTrail, setShowCommandedTrail] = useState(true);
  const [showObservedTrail, setShowObservedTrail] = useState(true);
  const [showHeadingArrow, setShowHeadingArrow] = useState(true);
  const [showModuleVectors, setShowModuleVectors] = useState(true);

  const pose = telemetry.pose as Record<string, number> | undefined;
  const velocity = telemetry.actualVelocity as Record<string, number> | undefined;
  const modules = Array.isArray(telemetry.modules) ? telemetry.modules as Array<Record<string, number>> : [];

  const diagnostics = useMemo(() => ({
    state: String(telemetry.drivetrainState || 'UNKNOWN'),
    authority: telemetry.translation_authority,
    steerReady: telemetry.steer_ready,
    pinpointUsed: telemetry.pinpointUsed,
  }), [telemetry]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (entry) {
        const { width, height } = entry.contentRect;
        setDimensions(prev => {
          if (Math.abs(prev.width - width) < 1 && Math.abs(prev.height - height) < 1) return prev;
          return { width, height };
        });
      }
    });

    resizeObserver.observe(container);
    return () => resizeObserver.disconnect();
  }, []);

  useEffect(() => {
    if (!pose) return;
    trailRef.current.push({ x: pose.xMeters, y: pose.yMeters });
    if (trailRef.current.length > 500) trailRef.current.shift();

    const observedX = Number(pose.xMeters ?? 0) + Number(velocity?.x ?? 0) * 0.05;
    const observedY = Number(pose.yMeters ?? 0) + Number(velocity?.y ?? 0) * 0.05;
    observedTrailRef.current.push({ x: observedX, y: observedY });
    if (observedTrailRef.current.length > 500) observedTrailRef.current.shift();
  }, [pose, velocity]);

  useEffect(() => {
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
    const fieldSize = Math.min(width, height) - 24;
    const pxPerMeter = fieldSize / 3.6576;
    const centerX = width / 2;
    const centerY = height / 2;

    const toCanvas = (xMeters: number, yMeters: number) => ({
      x: centerX - (yMeters * pxPerMeter),
      y: centerY - (xMeters * pxPerMeter),
    });

    ctx.clearRect(0, 0, width, height);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
    ctx.lineWidth = 1;
    const tileMeters = 0.6096;
    const gridSize = pxPerMeter * tileMeters;

    ctx.beginPath();
    for (let x = centerX % gridSize; x < width; x += gridSize) {
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
    }
    for (let y = centerY % gridSize; y < height; y += gridSize) {
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
    }
    ctx.stroke();

    ctx.strokeStyle = 'rgba(255,255,255,0.12)';
    ctx.strokeRect(centerX - fieldSize / 2, centerY - fieldSize / 2, fieldSize, fieldSize);

    const drawTrail = (trail: TrailPoint[], color: string) => {
      if (trail.length < 2) return;
      ctx.beginPath();
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      const first = toCanvas(trail[0].x, trail[0].y);
      ctx.moveTo(first.x, first.y);
      for (let i = 1; i < trail.length; i++) {
        const point = toCanvas(trail[i].x, trail[i].y);
        ctx.lineTo(point.x, point.y);
      }
      ctx.stroke();
    };

    if (showCommandedTrail) {
      drawTrail(trailRef.current, 'rgba(124, 77, 255, 0.65)');
    }
    if (showObservedTrail) {
      drawTrail(observedTrailRef.current, 'rgba(0, 229, 255, 0.45)');
    }

    if (pose) {
      const robot = toCanvas(Number(pose.xMeters ?? 0), Number(pose.yMeters ?? 0));
      const headingRadians = -Number(pose.headingRadians ?? 0);

      ctx.save();
      ctx.translate(robot.x, robot.y);
      ctx.rotate(headingRadians);

      const robotWidthPx = 0.4064 * pxPerMeter;
      const robotLengthPx = 0.4064 * pxPerMeter;

      ctx.strokeStyle = 'rgba(68, 138, 255, 0.95)';
      ctx.fillStyle = 'rgba(68, 138, 255, 0.14)';
      ctx.lineWidth = 2;
      ctx.fillRect(-robotWidthPx / 2, -robotLengthPx / 2, robotWidthPx, robotLengthPx);
      ctx.strokeRect(-robotWidthPx / 2, -robotLengthPx / 2, robotWidthPx, robotLengthPx);

      ctx.strokeStyle = '#ff5252';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.moveTo(-robotWidthPx / 2, -robotLengthPx / 2);
      ctx.lineTo(robotWidthPx / 2, -robotLengthPx / 2);
      ctx.stroke();

      if (showHeadingArrow) {
        ctx.strokeStyle = 'rgba(255,255,255,0.9)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(0, -robotLengthPx * 0.8);
        ctx.stroke();
      }

      if (showModuleVectors) {
        modules.forEach((mod) => {
          ctx.save();
          const modCx = -Number(mod.yMeters ?? 0) * pxPerMeter;
          const modCy = -Number(mod.xMeters ?? 0) * pxPerMeter;
          ctx.translate(modCx, modCy);
          ctx.rotate(-Number(mod.currentAngleRadians ?? 0));

          ctx.fillStyle = '#ff9100';
          ctx.fillRect(-4, -4, 8, 8);

          ctx.strokeStyle = '#00e676';
          ctx.lineWidth = 2;
          ctx.beginPath();
          ctx.moveTo(0, 0);
          ctx.lineTo(0, -15);
          ctx.stroke();
          ctx.restore();
        });
      }

      ctx.restore();
    }
  }, [dimensions, pose, velocity, modules, showCommandedTrail, showHeadingArrow, showModuleVectors, showObservedTrail]);

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', position: 'relative', overflow: 'hidden', backgroundColor: '#161822', borderRadius: '0.5rem', border: '1px solid rgba(255,255,255,0.05)', boxShadow: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)' }}
      onPointerMove={(event) => {
        const rect = containerRef.current?.getBoundingClientRect();
        if (!rect) return;
        const localX = event.clientX - rect.left;
        const localY = event.clientY - rect.top;
        const fieldSize = Math.min(rect.width, rect.height) - 24;
        const pxPerMeter = fieldSize / 3.6576;
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        const xMeters = (centerY - localY) / pxPerMeter;
        const yMeters = (centerX - localX) / pxPerMeter;
        setHoverInches({ xIn: xMeters / 0.0254, yIn: yMeters / 0.0254 });
      }}
      onPointerLeave={() => setHoverInches(null)}
    >
      <canvas ref={canvasRef} className="absolute inset-0 pointer-events-none" style={{ position: 'absolute', top: 0, left: 0, pointerEvents: 'none', width: '100%', height: '100%' }} />

      <div style={{ position: 'absolute', top: 10, left: 10, display: 'flex', gap: '0.35rem', flexWrap: 'wrap', zIndex: 2 }}>
        <button onClick={() => setShowCommandedTrail(prev => !prev)} style={{ ...overlayBtnStyle, color: showCommandedTrail ? '#d9ccff' : 'var(--text-dim)' }}><Ghost size={11} />Cmd</button>
        <button onClick={() => setShowObservedTrail(prev => !prev)} style={{ ...overlayBtnStyle, color: showObservedTrail ? '#8bf5ff' : 'var(--text-dim)' }}><Radar size={11} />Obs</button>
        <button onClick={() => setShowHeadingArrow(prev => !prev)} style={{ ...overlayBtnStyle, color: showHeadingArrow ? '#ffffff' : 'var(--text-dim)' }}><Navigation size={11} />Heading</button>
        <button onClick={() => setShowModuleVectors(prev => !prev)} style={{ ...overlayBtnStyle, color: showModuleVectors ? '#00e676' : 'var(--text-dim)' }}><Crosshair size={11} />Modules</button>
      </div>

      <div style={{ position: 'absolute', right: 10, top: 10, padding: '0.45rem 0.6rem', borderRadius: 8, background: 'rgba(0,0,0,0.28)', border: '1px solid rgba(255,255,255,0.06)', minWidth: 170 }}>
        <div style={readoutRow}><span>State</span><strong>{diagnostics.state}</strong></div>
        <div style={readoutRow}><span>Authority</span><strong>{diagnostics.authority !== undefined ? Number(diagnostics.authority).toFixed(2) : '--'}</strong></div>
        <div style={readoutRow}><span>Steer Ready</span><strong>{String(diagnostics.steerReady ?? '--')}</strong></div>
        <div style={readoutRow}><span>Pinpoint</span><strong>{String(diagnostics.pinpointUsed ?? '--')}</strong></div>
        {hoverInches && (
          <div style={{ ...readoutRow, borderTop: '1px solid rgba(255,255,255,0.06)', marginTop: 6, paddingTop: 6 }}>
            <span>Cursor</span>
            <strong>{hoverInches.xIn.toFixed(1)}, {hoverInches.yIn.toFixed(1)} in</strong>
          </div>
        )}
      </div>
    </div>
  );
}

const overlayBtnStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  padding: '0.28rem 0.45rem',
  borderRadius: 8,
  border: '1px solid rgba(255,255,255,0.08)',
  background: 'rgba(0,0,0,0.28)',
  cursor: 'pointer',
  fontSize: '0.63rem',
};

const readoutRow: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  gap: '0.8rem',
  fontSize: '0.64rem',
  color: 'var(--text-dim)',
};
