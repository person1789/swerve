// Arena.tsx — 2D field canvas with odometry trail, robot chassis, module vectors
import { useEffect, useRef, useCallback } from 'react';
import type { TelemetryFrame } from '../types/telemetry';
import { SwerveConfig } from '../lib/SwerveLogic';

interface ArenaProps {
  frame: TelemetryFrame | null;
  trail: { x: number; y: number }[];
  showVectors: boolean;
  showTrail: boolean;
}

const PX_PER_M = 110;
const FIELD_M  = 3.66; // 12ft in meters
const HALF     = FIELD_M * PX_PER_M;

const LABEL = ['FL', 'FR', 'RR', 'RL'];
const MODULE_COLORS = ['#f43f5e', '#fb923c', '#facc15', '#4ade80'];

function arrow(
  ctx: CanvasRenderingContext2D,
  x: number, y: number,
  len: number, angle: number,
  color: string,
  width = 1.5,
) {
  if (Math.abs(len) < 2) return;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(angle);
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  ctx.lineWidth = width;
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.lineTo(len, 0);
  ctx.stroke();
  const h = Math.min(Math.abs(len) * 0.25, 7);
  ctx.beginPath();
  ctx.moveTo(len, 0);
  ctx.lineTo(len - h, h / 2);
  ctx.lineTo(len - h, -h / 2);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

export default function Arena({ frame, trail, showVectors, showTrail }: ArenaProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const rafRef = useRef<number>(0);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const W = container.clientWidth;
    const H = container.clientHeight;
    const dpr = window.devicePixelRatio || 1;
    if (canvas.width !== W * dpr || canvas.height !== H * dpr) {
      canvas.width  = W * dpr;
      canvas.height = H * dpr;
      canvas.style.width  = W + 'px';
      canvas.style.height = H + 'px';
    }

    const ctx = canvas.getContext('2d')!;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    ctx.save();
    ctx.translate(W / 2, H / 2);

    // ── Grid ──────────────────────────────────────────────────────────────
    ctx.strokeStyle = '#1a2535';
    ctx.lineWidth = 1;
    const step = 0.6096 * PX_PER_M; // 2ft
    for (let i = -5; i <= 5; i++) {
      ctx.beginPath(); ctx.moveTo(i * step, -HALF); ctx.lineTo(i * step, HALF); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(-HALF, i * step); ctx.lineTo(HALF, i * step); ctx.stroke();
    }

    // ── Field boundary ────────────────────────────────────────────────────
    ctx.strokeStyle = '#2a3a52';
    ctx.lineWidth = 2;
    ctx.strokeRect(-HALF, -HALF, HALF * 2, HALF * 2);

    // ── Origin cross ──────────────────────────────────────────────────────
    ctx.strokeStyle = '#2a4a6a';
    ctx.lineWidth = 1;
    [-8, 8].forEach(d => {
      ctx.beginPath(); ctx.moveTo(-d, 0); ctx.lineTo(d, 0); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(0, -d); ctx.lineTo(0, d); ctx.stroke();
    });

    // ── Odometry trail ────────────────────────────────────────────────────
    if (showTrail && trail.length > 1) {
      ctx.strokeStyle = 'rgba(14, 165, 233, 0.25)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(trail[0].x * PX_PER_M, -trail[0].y * PX_PER_M);
      for (let i = 1; i < trail.length; i++) {
        ctx.lineTo(trail[i].x * PX_PER_M, -trail[i].y * PX_PER_M);
      }
      ctx.stroke();
      // Trail head dot
      const last = trail[trail.length - 1];
      ctx.fillStyle = 'rgba(14, 165, 233, 0.5)';
      ctx.beginPath();
      ctx.arc(last.x * PX_PER_M, -last.y * PX_PER_M, 3, 0, Math.PI * 2);
      ctx.fill();
    }

    // ── Robot ─────────────────────────────────────────────────────────────
    if (frame) {
      const { x, y, heading, actuals, targets } = frame;

      ctx.save();
      ctx.translate(x * PX_PER_M, -y * PX_PER_M);
      ctx.rotate(-heading);

      const hw = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PX_PER_M;
      const hl = (SwerveConfig.WHEEL_BASE_IN  * 0.0254 / 2) * PX_PER_M;
      const bw = hw * 2 + 8;

      // Shadow
      ctx.shadowBlur = 20;
      ctx.shadowColor = 'rgba(14,165,233,0.3)';

      // Chassis body
      ctx.strokeStyle = '#0ea5e9';
      ctx.lineWidth = 2;
      ctx.strokeRect(-hl - 4, -hw - 4, bw, bw);
      ctx.shadowBlur = 0;

      // Front indicator
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(hl + 4, -hw - 4);
      ctx.lineTo(hl + 4, hw + 4);
      ctx.stroke();

      // Center cross
      ctx.strokeStyle = '#1a3a5a';
      ctx.lineWidth = 1;
      ctx.beginPath(); ctx.moveTo(-8, 0); ctx.lineTo(8, 0); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(0, -8); ctx.lineTo(0, 8); ctx.stroke();

      // ── Modules ──────────────────────────────────────────────────────────
      const mOff: [number, number][] = [
        [ hl,  hw], [ hl, -hw], [-hl, -hw], [-hl,  hw],
      ];
      mOff.forEach(([mx, my], i) => {
        const actual  = actuals?.[i]  ?? [0, 0];
        const target  = targets?.[i]  ?? [0, 0];
        const speed   = actual[0];
        const angle   = actual[1];
        const color   = MODULE_COLORS[i];

        ctx.save();
        ctx.translate(mx, -my);

        // Traction circle (dim)
        ctx.strokeStyle = 'rgba(255,255,255,0.05)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(0, 0, 18, 0, Math.PI * 2);
        ctx.stroke();

        // Wheel rectangle (oriented)
        ctx.save();
        ctx.rotate(-angle);
        ctx.fillStyle = '#1a2535';
        ctx.strokeStyle = color + '80';
        ctx.lineWidth = 1.5;
        ctx.fillRect(-4, -8, 8, 16);
        ctx.strokeRect(-4, -8, 8, 16);
        ctx.restore();

        // Vectors
        if (showVectors) {
          // Target (dim)
          arrow(ctx, 0, 0, target[0] * 35, -target[1], color + '40', 2);
          // Actual
          arrow(ctx, 0, 0, speed * 35, -angle, color, 1.5);
        }

        // Module label
        ctx.fillStyle = color + 'aa';
        ctx.font = '600 7px JetBrains Mono, monospace';
        ctx.textAlign = 'center';
        ctx.fillText(LABEL[i], 0, 26);

        ctx.restore();
      });

      ctx.restore();
    }

    ctx.restore();
  }, [frame, trail, showVectors, showTrail]);

  useEffect(() => {
    const loop = () => { draw(); rafRef.current = requestAnimationFrame(loop); };
    rafRef.current = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(rafRef.current);
  }, [draw]);

  return (
    <div ref={containerRef} className="w-full h-full relative bg-scope-bg">
      <canvas ref={canvasRef} className="arena-canvas w-full h-full" />
      {/* Corner labels */}
      <div className="absolute top-2 left-3 text-[9px] text-scope-muted font-mono">FIELD_VIEW // 12×12ft</div>
      {frame && (
        <div className="absolute bottom-2 right-3 text-[9px] font-mono text-scope-text readout">
          {frame.x.toFixed(2)}m, {frame.y.toFixed(2)}m, {(frame.heading * 180 / Math.PI).toFixed(1)}°
        </div>
      )}
    </div>
  );
}
