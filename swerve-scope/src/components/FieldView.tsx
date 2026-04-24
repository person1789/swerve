import { useEffect, useRef, useState } from 'react';

interface FieldViewProps {
  telemetry: any;
}

export function FieldView({ telemetry }: FieldViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const trailRef = useRef<{x: number, y: number}[]>([]);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (entry) {
        const { width, height } = entry.contentRect;
        // Only update if dimensions actually changed significantly
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
    const canvas = canvasRef.current;
    if (!canvas || dimensions.width === 0) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = dimensions.width * dpr;
    canvas.height = dimensions.height * dpr;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    const fieldSize = Math.min(width, height);
    const pxPerMeter = fieldSize / 3.65;
    
    const centerX = width / 2;
    const centerY = height / 2;

    ctx.clearRect(0, 0, width, height);

    // Grid lines
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
    ctx.lineWidth = 1;
    const gridSize = pxPerMeter * 0.6096;
    
    ctx.beginPath();
    for (let x = centerX % gridSize; x < width; x += gridSize) {
      ctx.moveTo(x, 0); ctx.lineTo(x, height);
    }
    for (let y = centerY % gridSize; y < height; y += gridSize) {
      ctx.moveTo(0, y); ctx.lineTo(width, y);
    }
    ctx.stroke();

    const pose = telemetry.pose;
    if (pose) {
      const canvasX = centerX - (pose.yMeters * pxPerMeter);
      const canvasY = centerY - (pose.xMeters * pxPerMeter);
      
      trailRef.current.push({x: canvasX, y: canvasY});
      if (trailRef.current.length > 200) trailRef.current.shift();

      if (trailRef.current.length > 1) {
        ctx.beginPath();
        ctx.strokeStyle = 'rgba(124, 77, 255, 0.5)';
        ctx.lineWidth = 2 * dpr;
        ctx.moveTo(trailRef.current[0].x, trailRef.current[0].y);
        for (let i = 1; i < trailRef.current.length; i++) {
          ctx.lineTo(trailRef.current[i].x, trailRef.current[i].y);
        }
        ctx.stroke();
      }

      ctx.save();
      ctx.translate(canvasX, canvasY);
      ctx.rotate(-pose.headingRadians);

      const robotWidthPx = (16 * 0.0254) * pxPerMeter;
      const robotLengthPx = (16 * 0.0254) * pxPerMeter;
      
      ctx.strokeStyle = 'rgba(68, 138, 255, 0.8)';
      ctx.fillStyle = 'rgba(68, 138, 255, 0.1)';
      ctx.lineWidth = 2 * dpr;
      ctx.fillRect(-robotWidthPx/2, -robotLengthPx/2, robotWidthPx, robotLengthPx);
      ctx.strokeRect(-robotWidthPx/2, -robotLengthPx/2, robotWidthPx, robotLengthPx);

      ctx.strokeStyle = '#ff5252';
      ctx.lineWidth = 4 * dpr;
      ctx.beginPath();
      ctx.moveTo(-robotWidthPx/2, -robotLengthPx/2);
      ctx.lineTo(robotWidthPx/2, -robotLengthPx/2);
      ctx.stroke();

      if (telemetry.modules) {
        telemetry.modules.forEach((mod: any) => {
          ctx.save();
          const modCx = -mod.yMeters * pxPerMeter;
          const modCy = -mod.xMeters * pxPerMeter;
          ctx.translate(modCx, modCy);
          ctx.rotate(-mod.currentAngleRadians);
          
          ctx.fillStyle = '#ff9100';
          ctx.fillRect(-4 * dpr, -4 * dpr, 8 * dpr, 8 * dpr);
          
          ctx.strokeStyle = '#00e676';
          ctx.lineWidth = 2 * dpr;
          ctx.beginPath();
          ctx.moveTo(0, 0);
          ctx.lineTo(0, -15 * dpr);
          ctx.stroke();
          
          ctx.restore();
        });
      }
      ctx.restore();
    }
  }, [telemetry, dimensions]);

  return (
    <div ref={containerRef} style={{ width: '100%', height: '100%', position: 'relative', overflow: 'hidden', backgroundColor: '#161822', borderRadius: '0.5rem', border: '1px solid rgba(255,255,255,0.05)', boxShadow: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)' }}>
      <canvas ref={canvasRef} className="absolute inset-0 pointer-events-none" style={{ position: 'absolute', top: 0, left: 0, pointerEvents: 'none', width: '100%', height: '100%' }} />
    </div>
  );
}
