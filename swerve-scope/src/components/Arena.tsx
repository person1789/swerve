import { memo, useEffect, useRef, useCallback, useState } from 'react';
import type { PointerEvent as ReactPointerEvent, WheelEvent as ReactWheelEvent } from 'react';
import type { TelemetryFrame } from '../types/telemetry';
import { SwerveConfig } from '../lib/SwerveLogic';

interface ArenaProps {
  prevFrame: TelemetryFrame | null;
  frame: TelemetryFrame | null;
  trail: { x: number; y: number }[];
  showVectors: boolean;
  showTrail: boolean;
}

const PX_PER_M = 110;
const FIELD_M = 3.66;
const HALF = FIELD_M * PX_PER_M;
const TELEMETRY_DT_SEC = 0.02;
const LABEL = ['FL', 'FR', 'RR', 'RL'];
const MODULE_COLORS = ['#f43f5e', '#fb923c', '#facc15', '#4ade80'];
const DECODE_TAGS = [
  { id: 1, x: -1.5, y: 1.5 },
  { id: 2, x: 0.0, y: 1.5 },
  { id: 3, x: 1.5, y: 1.5 },
  { id: 4, x: -1.5, y: 0.0 },
  { id: 5, x: 1.5, y: 0.0 },
  { id: 6, x: -1.5, y: -1.5 },
  { id: 7, x: 0.0, y: -1.5 },
  { id: 8, x: 1.5, y: -1.5 },
];

function arrow(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  len: number,
  angle: number,
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
  const head = Math.min(Math.abs(len) * 0.25, 7);
  ctx.beginPath();
  ctx.moveTo(len, 0);
  ctx.lineTo(len - head, head / 2);
  ctx.lineTo(len - head, -head / 2);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

function normalizeAngle(angle: number) {
  let value = angle;
  while (value <= -Math.PI) value += Math.PI * 2;
  while (value > Math.PI) value -= Math.PI * 2;
  return value;
}

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t;
}

function lerpAngle(a: number, b: number, t: number) {
  const delta = normalizeAngle(b - a);
  return normalizeAngle(a + delta * t);
}

function interpolateFrame(prevFrame: TelemetryFrame | null, currentFrame: TelemetryFrame | null, alpha: number): TelemetryFrame | null {
  if (!currentFrame) return null;
  if (!prevFrame) return currentFrame;

  return {
    ...currentFrame,
    x: lerp(prevFrame.x, currentFrame.x, alpha),
    y: lerp(prevFrame.y, currentFrame.y, alpha),
    heading: lerpAngle(prevFrame.heading, currentFrame.heading, alpha),
    targets: currentFrame.targets.map((target, index) => {
      const prevTarget = prevFrame.targets[index] ?? target;
      return [
        lerp(prevTarget[0], target[0], alpha),
        lerpAngle(prevTarget[1], target[1], alpha),
      ] as [number, number];
    }),
    actuals: currentFrame.actuals.map((actual, index) => {
      const prevActual = prevFrame.actuals[index] ?? actual;
      return [
        lerp(prevActual[0], actual[0], alpha),
        lerpAngle(prevActual[1], actual[1], alpha),
      ] as [number, number];
    }),
  };
}

function Arena({ prevFrame, frame, trail, showVectors, showTrail }: ArenaProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<number>(0);
  const draggingRef = useRef(false);
  const dragOriginRef = useRef({ x: 0, y: 0 });
  const panStartRef = useRef({ x: 0, y: 0 });
  const latestFrameRef = useRef<TelemetryFrame | null>(frame);
  const latestPrevFrameRef = useRef<TelemetryFrame | null>(prevFrame);
  const latestTrailRef = useRef(trail);
  const frameReceivedAtRef = useRef(performance.now());
  const showVectorsRef = useRef(showVectors);
  const showTrailRef = useRef(showTrail);

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [hoverField, setHoverField] = useState<{ x: number; y: number } | null>(null);

  useEffect(() => {
    latestPrevFrameRef.current = prevFrame;
    latestFrameRef.current = frame;
    frameReceivedAtRef.current = performance.now();
  }, [prevFrame, frame]);

  useEffect(() => {
    latestTrailRef.current = trail;
  }, [trail]);

  useEffect(() => {
    showVectorsRef.current = showVectors;
  }, [showVectors]);

  useEffect(() => {
    showTrailRef.current = showTrail;
  }, [showTrail]);

  const worldToScreen = useCallback((worldX: number, worldY: number, width: number, height: number) => ({
    x: width / 2 + pan.x + worldX * PX_PER_M * zoom,
    y: height / 2 + pan.y - worldY * PX_PER_M * zoom,
  }), [pan.x, pan.y, zoom]);

  const screenToField = useCallback((screenX: number, screenY: number, width: number, height: number) => ({
    x: (screenX - width / 2 - pan.x) / (PX_PER_M * zoom),
    y: -(screenY - height / 2 - pan.y) / (PX_PER_M * zoom),
  }), [pan.x, pan.y, zoom]);

  const draw = useCallback((now: number) => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const width = container.clientWidth;
    const height = container.clientHeight;
    const dpr = window.devicePixelRatio || 1;
    if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
    }

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const alpha = Math.max(0, Math.min(1, (now - frameReceivedAtRef.current) / (TELEMETRY_DT_SEC * 1000)));
    const renderedFrame = interpolateFrame(latestPrevFrameRef.current, latestFrameRef.current, alpha);
    const renderedTrail = latestTrailRef.current;

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);

    ctx.save();
    ctx.translate(width / 2 + pan.x, height / 2 + pan.y);
    ctx.scale(zoom, zoom);

    ctx.strokeStyle = '#1a2535';
    ctx.lineWidth = 1 / zoom;
    const step = 0.6096 * PX_PER_M;
    for (let i = -5; i <= 5; i++) {
      ctx.beginPath();
      ctx.moveTo(i * step, -HALF);
      ctx.lineTo(i * step, HALF);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(-HALF, i * step);
      ctx.lineTo(HALF, i * step);
      ctx.stroke();
    }

    ctx.strokeStyle = '#2a3a52';
    ctx.lineWidth = 2 / zoom;
    ctx.strokeRect(-HALF, -HALF, HALF * 2, HALF * 2);

    DECODE_TAGS.forEach(tag => {
      ctx.save();
      ctx.translate(tag.x * PX_PER_M, -tag.y * PX_PER_M);
      ctx.strokeStyle = '#f59e0b40';
      ctx.lineWidth = 1 / zoom;
      ctx.strokeRect(-8, -8, 16, 16);
      ctx.fillStyle = '#f59e0bcc';
      ctx.font = `${Math.max(7 / zoom, 6)}px JetBrains Mono, monospace`;
      ctx.textAlign = 'center';
      ctx.fillText(`T${tag.id}`, 0, 3 / zoom);
      ctx.restore();
    });

    ctx.strokeStyle = '#2a4a6a';
    ctx.lineWidth = 1 / zoom;
    [-8, 8].forEach(delta => {
      ctx.beginPath();
      ctx.moveTo(-delta, 0);
      ctx.lineTo(delta, 0);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, -delta);
      ctx.lineTo(0, delta);
      ctx.stroke();
    });

    if (showTrailRef.current && renderedTrail.length > 1) {
      for (let i = 1; i < renderedTrail.length; i++) {
        const trailAlpha = i / renderedTrail.length;
        ctx.strokeStyle = `rgba(14, 165, 233, ${0.08 + trailAlpha * 0.35})`;
        ctx.lineWidth = (1 + trailAlpha) / zoom;
        ctx.beginPath();
        ctx.moveTo(renderedTrail[i - 1].x * PX_PER_M, -renderedTrail[i - 1].y * PX_PER_M);
        ctx.lineTo(renderedTrail[i].x * PX_PER_M, -renderedTrail[i].y * PX_PER_M);
        ctx.stroke();
      }
      const head = renderedTrail[renderedTrail.length - 1];
      ctx.fillStyle = 'rgba(14, 165, 233, 0.65)';
      ctx.beginPath();
      ctx.arc(head.x * PX_PER_M, -head.y * PX_PER_M, 3 / zoom, 0, Math.PI * 2);
      ctx.fill();
    }

    if (renderedFrame) {
      const { x, y, heading, actuals, targets } = renderedFrame;

      ctx.save();
      ctx.translate(x * PX_PER_M, -y * PX_PER_M);
      ctx.rotate(-heading);

      const halfWidth = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PX_PER_M;
      const halfLength = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PX_PER_M;
      const bodySize = halfWidth * 2 + 8;
      const moduleOffsets: [number, number][] = [
        [halfLength, halfWidth],
        [halfLength, -halfWidth],
        [-halfLength, -halfWidth],
        [-halfLength, halfWidth],
      ];

      ctx.shadowBlur = 20 / zoom;
      ctx.shadowColor = 'rgba(14,165,233,0.3)';
      ctx.strokeStyle = '#0ea5e9';
      ctx.lineWidth = 2 / zoom;
      ctx.strokeRect(-halfLength - 4, -halfWidth - 4, bodySize, bodySize);
      ctx.shadowBlur = 0;

      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 3 / zoom;
      ctx.beginPath();
      ctx.moveTo(halfLength + 4, -halfWidth - 4);
      ctx.lineTo(halfLength + 4, halfWidth + 4);
      ctx.stroke();

      ctx.strokeStyle = '#1a3a5a';
      ctx.lineWidth = 1 / zoom;
      ctx.beginPath();
      ctx.moveTo(-8, 0);
      ctx.lineTo(8, 0);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, -8);
      ctx.lineTo(0, 8);
      ctx.stroke();

      moduleOffsets.forEach(([moduleX, moduleY], i) => {
        const actual = actuals?.[i] ?? [0, 0];
        const target = targets?.[i] ?? [0, 0];
        const color = MODULE_COLORS[i];

        ctx.save();
        ctx.translate(moduleX, -moduleY);

        ctx.strokeStyle = 'rgba(255,255,255,0.05)';
        ctx.lineWidth = 1 / zoom;
        ctx.beginPath();
        ctx.arc(0, 0, 18, 0, Math.PI * 2);
        ctx.stroke();

        ctx.save();
        ctx.rotate(-actual[1]);
        ctx.fillStyle = '#1a2535';
        ctx.strokeStyle = `${color}80`;
        ctx.lineWidth = 1.5 / zoom;
        ctx.fillRect(-4, -8, 8, 16);
        ctx.strokeRect(-4, -8, 8, 16);
        ctx.restore();

        if (showVectorsRef.current) {
          arrow(ctx, 0, 0, target[0] * 35, -target[1], `${color}40`, 2 / zoom);
          arrow(ctx, 0, 0, actual[0] * 35, -actual[1], color, 1.5 / zoom);
        }

        ctx.fillStyle = `${color}cc`;
        ctx.font = `${Math.max(7 / zoom, 6)}px JetBrains Mono, monospace`;
        ctx.textAlign = 'center';
        ctx.fillText(LABEL[i], 0, 26 / zoom);
        ctx.restore();
      });

      ctx.restore();
    }

    ctx.restore();

    if (hoverField) {
      const hoverScreen = worldToScreen(hoverField.x, hoverField.y, width, height);
      ctx.strokeStyle = '#8ba4c0';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(hoverScreen.x - 6, hoverScreen.y);
      ctx.lineTo(hoverScreen.x + 6, hoverScreen.y);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(hoverScreen.x, hoverScreen.y - 6);
      ctx.lineTo(hoverScreen.x, hoverScreen.y + 6);
      ctx.stroke();
    }
  }, [hoverField, pan.x, pan.y, worldToScreen, zoom]);

  useEffect(() => {
    const animate = (now: number) => {
      draw(now);
      animationRef.current = requestAnimationFrame(animate);
    };
    animationRef.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationRef.current);
  }, [draw]);

  const updateHover = useCallback((clientX: number, clientY: number) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    setHoverField(screenToField(clientX - rect.left, clientY - rect.top, rect.width, rect.height));
  }, [screenToField]);

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 1) return;
    event.preventDefault();
    draggingRef.current = true;
    dragOriginRef.current = { x: event.clientX, y: event.clientY };
    panStartRef.current = pan;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    updateHover(event.clientX, event.clientY);
    if (!draggingRef.current) return;
    setPan({
      x: panStartRef.current.x + (event.clientX - dragOriginRef.current.x),
      y: panStartRef.current.y + (event.clientY - dragOriginRef.current.y),
    });
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    draggingRef.current = false;
    event.currentTarget.releasePointerCapture(event.pointerId);
  };

  const handleWheel = (event: ReactWheelEvent<HTMLDivElement>) => {
    event.preventDefault();
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;
    const before = screenToField(mouseX, mouseY, rect.width, rect.height);
    const nextZoom = Math.max(0.5, Math.min(4, zoom * (event.deltaY < 0 ? 1.08 : 0.92)));
    const afterScale = PX_PER_M * nextZoom;
    const nextPan = {
      x: mouseX - rect.width / 2 - before.x * afterScale,
      y: mouseY - rect.height / 2 + before.y * afterScale,
    };

    setZoom(nextZoom);
    setPan(nextPan);
  };

  return (
    <div
      ref={containerRef}
      className="w-full h-full relative bg-scope-bg select-none"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerLeave={() => setHoverField(null)}
      onWheel={handleWheel}
      onDoubleClick={() => {
        setZoom(1);
        setPan({ x: 0, y: 0 });
      }}
    >
      <canvas ref={canvasRef} className="arena-canvas w-full h-full" />
      <div className="absolute top-2 left-3 text-[9px] text-scope-muted font-mono">FIELD_VIEW // 12x12ft // 60 FPS</div>
      <div className="absolute top-2 right-3 text-[8px] font-mono text-scope-muted">
        wheel zoom  middle-drag pan  double-click reset
      </div>
      {hoverField && (
        <div className="absolute bottom-7 left-3 text-[9px] font-mono text-scope-text readout">
          hover {hoverField.x.toFixed(2)}m, {hoverField.y.toFixed(2)}m
        </div>
      )}
      <div className="absolute bottom-2 right-3 text-[9px] font-mono text-scope-text readout">
        zoom {zoom.toFixed(2)}x
        {frame && `  |  robot ${frame.x.toFixed(2)}m, ${frame.y.toFixed(2)}m, ${(frame.heading * 180 / Math.PI).toFixed(1)}deg`}
      </div>
    </div>
  );
}

export default memo(Arena);
