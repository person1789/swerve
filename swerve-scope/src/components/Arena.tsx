import { memo, useEffect, useRef, useCallback, useState } from 'react';
import type { PointerEvent as ReactPointerEvent, WheelEvent as ReactWheelEvent } from 'react';
import type { TelemetryFrame } from '../types/telemetry';
import { SwerveConfig } from '../lib/SwerveLogic';
import { telemetryStore } from '../store/telemetryStore';

const PX_PER_M = 110;
const FIELD_M = 3.66;
const HALF = FIELD_M * PX_PER_M;
const TELEMETRY_DT_SEC = 0.02;
const LABEL = ['FL', 'FR', 'RR', 'RL'];
const MODULE_COLORS = ['#f43f5e', '#fb923c', '#facc15', '#4ade80'];

const DECODE_TAGS = [
  { id: 1, x: -1.5, y: 1.5 }, { id: 2, x: 0.0, y: 1.5 },
  { id: 3, x: 1.5, y: 1.5 }, { id: 4, x: -1.5, y: 0.0 },
  { id: 5, x: 1.5, y: 0.0 }, { id: 6, x: -1.5, y: -1.5 },
  { id: 7, x: 0.0, y: -1.5 }, { id: 8, x: 1.5, y: -1.5 },
];

function arrow(
  ctx: CanvasRenderingContext2D,
  x: number, y: number, len: number, angle: number,
  color: string, width = 1.5,
) {
  if (Math.abs(len) < 2) return;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(angle);
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  ctx.lineWidth = width;
  ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(len, 0); ctx.stroke();
  const head = Math.min(Math.abs(len) * 0.25, 7);
  ctx.beginPath();
  ctx.moveTo(len, 0);
  ctx.lineTo(len - head, head / 2);
  ctx.lineTo(len - head, -head / 2);
  ctx.closePath(); ctx.fill();
  ctx.restore();
}

function normalizeAngle(a: number) {
  while (a <= -Math.PI) a += Math.PI * 2;
  while (a > Math.PI) a -= Math.PI * 2;
  return a;
}

function lerp(a: number, b: number, t: number) { return a + (b - a) * t; }
function lerpAngle(a: number, b: number, t: number) {
  return normalizeAngle(a + normalizeAngle(b - a) * t);
}

function interpolateFrame(
  prev: TelemetryFrame | null,
  cur: TelemetryFrame | null,
  alpha: number,
): TelemetryFrame | null {
  if (!cur) return null;
  if (!prev) return cur;
  return {
    ...cur,
    x: lerp(prev.x, cur.x, alpha),
    y: lerp(prev.y, cur.y, alpha),
    heading: lerpAngle(prev.heading, cur.heading, alpha),
    targets: cur.targets.map((t, i) => [
      lerp(prev.targets[i]?.[0] ?? t[0], t[0], alpha),
      lerpAngle(prev.targets[i]?.[1] ?? t[1], t[1], alpha),
    ] as [number, number]),
    actuals: cur.actuals.map((a, i) => [
      lerp(prev.actuals[i]?.[0] ?? a[0], a[0], alpha),
      lerpAngle(prev.actuals[i]?.[1] ?? a[1], a[1], alpha),
    ] as [number, number]),
  };
}

interface ArenaProps {
  showVectors: boolean;
  showTrail: boolean;
}

function Arena({ showVectors, showTrail }: ArenaProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const animRef = useRef(0);
  const frameReceivedAtRef = useRef(performance.now());
  const showVectorsRef = useRef(showVectors);
  const showTrailRef = useRef(showTrail);

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [hoverField, setHoverField] = useState<{ x: number; y: number } | null>(null);

  const zoomRef = useRef(zoom);
  const panRef = useRef(pan);

  useEffect(() => {
    const unsubscribe = telemetryStore.subscribeToFrames(() => {
      frameReceivedAtRef.current = performance.now();
    });
    return () => {
      unsubscribe();
    };
  }, []);

  useEffect(() => { showVectorsRef.current = showVectors; }, [showVectors]);
  useEffect(() => { showTrailRef.current = showTrail; }, [showTrail]);
  useEffect(() => { zoomRef.current = zoom; }, [zoom]);
  useEffect(() => { panRef.current = pan; }, [pan]);

  const screenToField = useCallback((sx: number, sy: number, w: number, h: number) => ({
    x: (sx - w / 2 - panRef.current.x) / (PX_PER_M * zoomRef.current),
    y: -(sy - h / 2 - panRef.current.y) / (PX_PER_M * zoomRef.current),
  }), []);

  const draw = useCallback((now: number) => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const w = container.clientWidth;
    const h = container.clientHeight;
    if (w < 2 || h < 2) return;

    const dpr = window.devicePixelRatio || 1;
    if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      canvas.style.width = `${w}px`;
      canvas.style.height = `${h}px`;
    }

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const snap = telemetryStore.getSnapshot();
    const curFrame = snap.frame;
    const prevFrame = snap.prevFrame;
    const trail = snap.trail;

    const alpha = Math.max(0, Math.min(1,
      (now - frameReceivedAtRef.current) / (TELEMETRY_DT_SEC * 1000)));
    const frame = interpolateFrame(prevFrame, curFrame, alpha);

    const z = zoomRef.current;
    const p = panRef.current;

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    ctx.save();
    ctx.translate(w / 2 + p.x, h / 2 + p.y);
    ctx.scale(z, z);

    ctx.strokeStyle = '#1a2535';
    ctx.lineWidth = 1 / z;
    const step = 0.6096 * PX_PER_M;
    for (let i = -5; i <= 5; i++) {
      ctx.beginPath(); ctx.moveTo(i * step, -HALF); ctx.lineTo(i * step, HALF); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(-HALF, i * step); ctx.lineTo(HALF, i * step); ctx.stroke();
    }

    ctx.strokeStyle = '#2a3a52';
    ctx.lineWidth = 2 / z;
    ctx.strokeRect(-HALF, -HALF, HALF * 2, HALF * 2);

    DECODE_TAGS.forEach(tag => {
      ctx.save();
      ctx.translate(tag.x * PX_PER_M, -tag.y * PX_PER_M);
      ctx.strokeStyle = '#f59e0b40';
      ctx.lineWidth = 1 / z;
      ctx.strokeRect(-8, -8, 16, 16);
      ctx.fillStyle = '#f59e0bcc';
      ctx.font = `${Math.max(7 / z, 5)}px JetBrains Mono, monospace`;
      ctx.textAlign = 'center';
      ctx.fillText(`T${tag.id}`, 0, 3 / z);
      ctx.restore();
    });

    if (showTrailRef.current && trail.length > 1) {
      for (let i = 1; i < trail.length; i++) {
        const a = i / trail.length;
        ctx.strokeStyle = `rgba(14,165,233,${0.06 + a * 0.3})`;
        ctx.lineWidth = (0.8 + a * 1.2) / z;
        ctx.beginPath();
        ctx.moveTo(trail[i - 1].x * PX_PER_M, -trail[i - 1].y * PX_PER_M);
        ctx.lineTo(trail[i].x * PX_PER_M, -trail[i].y * PX_PER_M);
        ctx.stroke();
      }
      const h2 = trail[trail.length - 1];
      ctx.fillStyle = 'rgba(14,165,233,0.6)';
      ctx.beginPath(); ctx.arc(h2.x * PX_PER_M, -h2.y * PX_PER_M, 3 / z, 0, Math.PI * 2); ctx.fill();
    }

    if (frame) {
      const { x, y, heading, actuals, targets } = frame;
      ctx.save();
      ctx.translate(x * PX_PER_M, -y * PX_PER_M);
      ctx.rotate(-heading);

      const hw = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PX_PER_M;
      const hl = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PX_PER_M;
      const offsets: [number, number][] = [
        [hl, hw], [hl, -hw], [-hl, -hw], [-hl, hw],
      ];

      ctx.shadowBlur = 16 / z; ctx.shadowColor = 'rgba(14,165,233,0.25)';
      ctx.strokeStyle = '#0ea5e9'; ctx.lineWidth = 2 / z;
      ctx.strokeRect(-hl - 4, -hw - 4, (hw + 4) * 2, (hl + 4) * 2);
      ctx.shadowBlur = 0;

      ctx.strokeStyle = '#ef4444'; ctx.lineWidth = 3 / z;
      ctx.beginPath();
      ctx.moveTo(hl + 4, -hw - 4); ctx.lineTo(hl + 4, hw + 4);
      ctx.stroke();

      ctx.strokeStyle = '#1a3a5a'; ctx.lineWidth = 1 / z;
      ctx.beginPath(); ctx.moveTo(-8, 0); ctx.lineTo(8, 0); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(0, -8); ctx.lineTo(0, 8); ctx.stroke();

      offsets.forEach(([mx, my], i) => {
        const actual = actuals?.[i] ?? [0, 0];
        const target = targets?.[i] ?? [0, 0];
        const color = MODULE_COLORS[i];

        ctx.save(); ctx.translate(mx, -my);
        ctx.strokeStyle = 'rgba(255,255,255,0.04)'; ctx.lineWidth = 1 / z;
        ctx.beginPath(); ctx.arc(0, 0, 18, 0, Math.PI * 2); ctx.stroke();

        ctx.save(); ctx.rotate(-actual[1]);
        ctx.fillStyle = '#1a2535'; ctx.strokeStyle = `${color}70`; ctx.lineWidth = 1.5 / z;
        ctx.fillRect(-4, -8, 8, 16); ctx.strokeRect(-4, -8, 8, 16);
        ctx.restore();

        if (showVectorsRef.current) {
          arrow(ctx, 0, 0, target[0] * 35, -target[1], `${color}38`, 2 / z);
          arrow(ctx, 0, 0, actual[0] * 35, -actual[1], color, 1.5 / z);
        }

        ctx.fillStyle = `${color}bb`;
        ctx.font = `${Math.max(7 / z, 5)}px JetBrains Mono, monospace`;
        ctx.textAlign = 'center';
        ctx.fillText(LABEL[i], 0, 26 / z);
        ctx.restore();
      });

      ctx.restore();
    }

    ctx.restore();
  }, []);

  useEffect(() => {
    const animate = (now: number) => {
      draw(now);
      animRef.current = requestAnimationFrame(animate);
    };
    animRef.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animRef.current);
  }, [draw]);

  const dragging = useRef(false);
  const dragOrigin = useRef({ x: 0, y: 0 });
  const panStart = useRef({ x: 0, y: 0 });

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 1) return;
    event.preventDefault();
    dragging.current = true;
    dragOrigin.current = { x: event.clientX, y: event.clientY };
    panStart.current = panRef.current;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (rect) {
      setHoverField(screenToField(
        event.clientX - rect.left, event.clientY - rect.top,
        rect.width, rect.height,
      ));
    }
    if (!dragging.current) return;
    setPan({
      x: panStart.current.x + (event.clientX - dragOrigin.current.x),
      y: panStart.current.y + (event.clientY - dragOrigin.current.y),
    });
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    dragging.current = false;
    event.currentTarget.releasePointerCapture(event.pointerId);
  };

  const handleWheel = (event: ReactWheelEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const mx = event.clientX - rect.left;
    const my = event.clientY - rect.top;
    const before = screenToField(mx, my, rect.width, rect.height);
    const nextZoom = Math.max(0.4, Math.min(5, zoomRef.current * (event.deltaY < 0 ? 1.1 : 0.9)));
    const afterScale = PX_PER_M * nextZoom;
    setZoom(nextZoom);
    setPan({
      x: mx - rect.width / 2 - before.x * afterScale,
      y: my - rect.height / 2 + before.y * afterScale,
    });
  };

  const frameSnap = telemetryStore.getSnapshot().frame;

  return (
    <div
      ref={containerRef}
      className="w-full h-full relative bg-scope-bg select-none"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerLeave={() => setHoverField(null)}
      onWheel={handleWheel}
      onDoubleClick={() => { setZoom(1); setPan({ x: 0, y: 0 }); }}
      style={{ cursor: dragging.current ? 'grabbing' : 'crosshair' }}
    >
      <canvas ref={canvasRef} className="arena-canvas absolute inset-0" />

      <div className="absolute top-2 left-3 text-[9px] text-scope-muted font-mono pointer-events-none">
        FIELD_VIEW // 12x12 ft // 60 FPS
      </div>
      <div className="absolute top-2 right-3 text-[8px] font-mono text-scope-muted pointer-events-none">
        scroll=zoom  mid-drag=pan  dbl=reset
      </div>

      {hoverField && (
        <div className="absolute bottom-7 left-3 text-[9px] font-mono text-scope-text readout pointer-events-none">
          {hoverField.x.toFixed(2)} m, {hoverField.y.toFixed(2)} m
        </div>
      )}
      <div className="absolute bottom-2 right-3 text-[9px] font-mono text-scope-text readout pointer-events-none">
        x{zoom.toFixed(2)}
        {frameSnap && `  |  ${frameSnap.x.toFixed(2)}m, ${frameSnap.y.toFixed(2)}m`}
        {frameSnap && `  ${(frameSnap.heading * 180 / Math.PI).toFixed(1)}deg`}
      </div>
    </div>
  );
}

export default memo(Arena);
