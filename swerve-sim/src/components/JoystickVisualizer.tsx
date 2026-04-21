import React, { useRef, useEffect, useCallback } from 'react';
import type { TelemetryEntry } from '../types/telemetry';

interface JoystickVisualizerProps {
    currentFrame: TelemetryEntry | null;
    history: TelemetryEntry[];
}

const STICK_RADIUS = 60;
const DOT_RADIUS = 6;
const TRAIL_LENGTH = 25; // ~0.5s at 50Hz

const COLORS = {
    background: '#0f172a',
    ring: '#1e293b',
    ringHover: '#334155',
    crosshair: 'rgba(71, 85, 105, 0.3)',
    deadzone: 'rgba(71, 85, 105, 0.15)',
    rawDot: '#f43f5e',
    rawTrail: 'rgba(244, 63, 94, 0.15)',
    filteredDot: '#10b981',
    filteredTrail: 'rgba(16, 185, 129, 0.15)',
    label: '#64748b',
    value: '#cbd5e1',
    dpadActive: '#f59e0b',
    dpadInactive: '#1e293b',
    buttonBorder: '#334155',
};

const JoystickVisualizer: React.FC<JoystickVisualizerProps> = ({ currentFrame, history }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    const drawStick = (
        ctx: CanvasRenderingContext2D,
        cx: number, cy: number,
        rawX: number, rawY: number,
        label: string,
    ) => {
        const r = STICK_RADIUS;

        // Outer ring
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(15, 23, 42, 0.8)';
        ctx.fill();
        ctx.strokeStyle = COLORS.ring;
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Deadzone circle (5%)
        ctx.beginPath();
        ctx.arc(cx, cy, r * 0.05, 0, Math.PI * 2);
        ctx.fillStyle = COLORS.deadzone;
        ctx.fill();

        // Crosshairs
        ctx.strokeStyle = COLORS.crosshair;
        ctx.lineWidth = 0.5;
        ctx.setLineDash([3, 3]);

        ctx.beginPath();
        ctx.moveTo(cx - r, cy);
        ctx.lineTo(cx + r, cy);
        ctx.stroke();

        ctx.beginPath();
        ctx.moveTo(cx, cy - r);
        ctx.lineTo(cx, cy + r);
        ctx.stroke();

        ctx.setLineDash([]);

        // Trail from recent history
        const recent = history.slice(-TRAIL_LENGTH);
        if (recent.length > 1) {
            ctx.beginPath();
            for (let i = 0; i < recent.length; i++) {
                const gp = recent[i].gamepad;
                let tx: number, ty: number;
                if (label === 'TRANSLATION') {
                    tx = gp?.lx ?? 0;
                    ty = gp?.ly ?? 0;
                } else {
                    tx = gp?.rx ?? 0;
                    ty = gp?.ry ?? 0;
                }
                const px = cx + tx * r;
                const py = cy - ty * r;
                if (i === 0) ctx.moveTo(px, py);
                else ctx.lineTo(px, py);
            }
            ctx.strokeStyle = COLORS.rawTrail;
            ctx.lineWidth = 2;
            ctx.stroke();
        }

        // Raw position dot
        const dotX = cx + rawX * r;
        const dotY = cy - rawY * r; // Invert Y for screen coords

        // Connecting line from center
        if (Math.hypot(rawX, rawY) > 0.02) {
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(dotX, dotY);
            ctx.strokeStyle = 'rgba(244, 63, 94, 0.25)';
            ctx.lineWidth = 1;
            ctx.stroke();
        }

        // Dot
        ctx.beginPath();
        ctx.arc(dotX, dotY, DOT_RADIUS, 0, Math.PI * 2);
        ctx.fillStyle = COLORS.rawDot;
        ctx.fill();
        ctx.strokeStyle = 'rgba(244, 63, 94, 0.4)';
        ctx.lineWidth = 1;
        ctx.stroke();

        // Glow
        ctx.shadowBlur = 10;
        ctx.shadowColor = COLORS.rawDot;
        ctx.beginPath();
        ctx.arc(dotX, dotY, 2, 0, Math.PI * 2);
        ctx.fillStyle = COLORS.rawDot;
        ctx.fill();
        ctx.shadowBlur = 0;

        // Label
        ctx.fillStyle = COLORS.label;
        ctx.font = 'bold 8px ui-monospace, monospace';
        ctx.textAlign = 'center';
        ctx.fillText(label, cx, cy - r - 10);

        // Value readout
        ctx.fillStyle = COLORS.value;
        ctx.font = '9px ui-monospace, monospace';
        ctx.fillText(`X:${rawX.toFixed(2)}  Y:${rawY.toFixed(2)}`, cx, cy + r + 16);

        // Magnitude
        const mag = Math.min(Math.hypot(rawX, rawY), 1.0);
        ctx.fillStyle = COLORS.label;
        ctx.font = '8px ui-monospace, monospace';
        ctx.fillText(`MAG: ${(mag * 100).toFixed(0)}%`, cx, cy + r + 28);
    };

    const drawDpad = (
        ctx: CanvasRenderingContext2D,
        cx: number, cy: number,
        frame: TelemetryEntry | null,
    ) => {
        const size = 14;
        const gap = 2;
        const gp = frame?.gamepad;

        const directions: { dx: number; dy: number; key: string; label: string }[] = [
            { dx: 0, dy: -1, key: 'dpad_up', label: '▲' },
            { dx: 0, dy: 1, key: 'dpad_down', label: '▼' },
            { dx: -1, dy: 0, key: 'dpad_left', label: '◄' },
            { dx: 1, dy: 0, key: 'dpad_right', label: '►' },
        ];

        // Center block
        ctx.fillStyle = COLORS.dpadInactive;
        ctx.fillRect(cx - size / 2, cy - size / 2, size, size);

        directions.forEach(({ dx, dy, key, label }) => {
            const px = cx + dx * (size + gap) - size / 2;
            const py = cy + dy * (size + gap) - size / 2;
            const active = gp?.[key] ?? false;

            ctx.fillStyle = active ? COLORS.dpadActive : COLORS.dpadInactive;
            ctx.strokeStyle = active ? COLORS.dpadActive : COLORS.buttonBorder;
            ctx.lineWidth = 1;

            ctx.beginPath();
            ctx.roundRect(px, py, size, size, 2);
            ctx.fill();
            ctx.stroke();

            if (active) {
                ctx.shadowBlur = 8;
                ctx.shadowColor = COLORS.dpadActive;
                ctx.fill();
                ctx.shadowBlur = 0;
            }

            ctx.fillStyle = active ? '#fff' : COLORS.label;
            ctx.font = '8px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(label, px + size / 2, py + size / 2);
        });

        // Label
        ctx.fillStyle = COLORS.label;
        ctx.font = 'bold 8px ui-monospace, monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText('D-PAD', cx, cy + size * 2 + 8);
    };

    const draw = useCallback(() => {
        const canvas = canvasRef.current;
        const container = containerRef.current;
        if (!canvas || !container) return;

        const rect = container.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        canvas.style.width = `${rect.width}px`;
        canvas.style.height = `${rect.height}px`;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;
        ctx.scale(dpr, dpr);

        const w = rect.width;
        const h = rect.height;

        ctx.clearRect(0, 0, w, h);

        const gp = currentFrame?.gamepad;
        const lx = gp?.lx ?? 0;
        const ly = gp?.ly ?? 0;
        const rx = gp?.rx ?? 0;
        const ry = gp?.ry ?? 0;

        // Layout: two sticks side by side, D-pad below center
        const stickY = h * 0.38;
        const leftX = w * 0.28;
        const rightX = w * 0.72;

        drawStick(ctx, leftX, stickY, lx, ly, 'TRANSLATION');
        drawStick(ctx, rightX, stickY, rx, ry, 'ROTATION');

        // D-pad centered below
        drawDpad(ctx, w * 0.5, h * 0.78, currentFrame);

        // Controller mode indicator
        ctx.fillStyle = currentFrame?.isSnapping ? '#f59e0b' : currentFrame?.isMaintaining ? '#3b82f6' : '#475569';
        ctx.font = 'bold 8px ui-monospace, monospace';
        ctx.textAlign = 'center';
        const modeLabel = currentFrame?.isSnapping ? 'SNAP MODE' : currentFrame?.isMaintaining ? 'HEADING HOLD' : 'MANUAL';
        ctx.fillText(modeLabel, w / 2, h * 0.92);

    }, [currentFrame, history]);

    useEffect(() => {
        draw();
    }, [draw]);

    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;
        const observer = new ResizeObserver(() => draw());
        observer.observe(container);
        return () => observer.disconnect();
    }, [draw]);

    return (
        <div ref={containerRef} className="w-full h-full bg-slate-950 relative overflow-hidden">
            <canvas ref={canvasRef} className="block w-full h-full" />
            <div className="absolute top-2 left-3 pointer-events-none">
                <span className="text-[8px] font-mono font-bold text-slate-600 uppercase tracking-widest">
                    Joystick Visualizer // Gamepad Input
                </span>
            </div>
        </div>
    );
};

export default JoystickVisualizer;
