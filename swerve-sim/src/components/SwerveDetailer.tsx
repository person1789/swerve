import React, { useRef, useEffect, useCallback } from 'react';
import type { TelemetryEntry } from '../types/telemetry';

interface SwerveDetailerProps {
    currentFrame: TelemetryEntry | null;
}

const MODULE_NAMES = ['FL', 'FR', 'RR', 'RL'];
const MAX_SPEED = 1.35; // m/s — for vector scaling

// Colors
const TARGET_COLOR = 'rgba(244, 63, 94, 0.55)';     // rose-500 translucent
const TARGET_STROKE = 'rgba(244, 63, 94, 0.9)';
const ACTUAL_COLOR = '#10b981';                       // emerald-500
const ACTUAL_STROKE = '#10b981';
const TRACTION_CIRCLE_COLOR = 'rgba(148, 163, 184, 0.12)';
const TRACTION_FILL_OK = 'rgba(16, 185, 129, 0.06)';
const TRACTION_FILL_WARN = 'rgba(245, 158, 11, 0.10)';
const TRACTION_FILL_CRIT = 'rgba(239, 68, 68, 0.12)';
const CHASSIS_BORDER = '#334155';
const CHASSIS_FILL = 'rgba(30, 41, 59, 0.6)';
const FRONT_INDICATOR = '#f43f5e';
const LABEL_COLOR = '#94a3b8';
const VALUE_COLOR = '#e2e8f0';
const ANGULAR_ARC_COLOR = '#a78bfa';

const SwerveDetailer: React.FC<SwerveDetailerProps> = ({ currentFrame }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    const drawArrow = (
        ctx: CanvasRenderingContext2D,
        x: number, y: number,
        length: number, angle: number,
        strokeColor: string, fillColor: string,
        lineWidth: number = 2.5,
    ) => {
        if (Math.abs(length) < 1.5) return;
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.strokeStyle = strokeColor;
        ctx.fillStyle = fillColor;
        ctx.lineWidth = lineWidth;
        ctx.lineCap = 'round';

        // Shaft
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(length, 0);
        ctx.stroke();

        // Arrowhead
        const headSize = Math.min(Math.abs(length) * 0.3, 10);
        ctx.beginPath();
        ctx.moveTo(length, 0);
        ctx.lineTo(length - headSize, headSize / 2);
        ctx.lineTo(length - headSize, -headSize / 2);
        ctx.closePath();
        ctx.fill();

        ctx.restore();
    };

    const drawModule = (
        ctx: CanvasRenderingContext2D,
        cx: number, cy: number,
        radius: number,
        target: number[],
        actual: number[],
        name: string,
    ) => {
        const [tSpeed, tAngle] = target;
        const [aSpeed, aAngle] = actual;

        // Traction circle
        const utilization = Math.abs(aSpeed) / MAX_SPEED;
        let tractionFill = TRACTION_FILL_OK;
        if (utilization > 0.9) tractionFill = TRACTION_FILL_CRIT;
        else if (utilization > 0.7) tractionFill = TRACTION_FILL_WARN;

        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fillStyle = tractionFill;
        ctx.fill();
        ctx.strokeStyle = TRACTION_CIRCLE_COLOR;
        ctx.lineWidth = 1;
        ctx.stroke();

        // Utilization ring (filled arc)
        if (utilization > 0.01) {
            ctx.beginPath();
            ctx.arc(cx, cy, radius, -Math.PI / 2, -Math.PI / 2 + utilization * Math.PI * 2);
            ctx.strokeStyle = utilization > 0.9
                ? 'rgba(239, 68, 68, 0.4)'
                : utilization > 0.7
                    ? 'rgba(245, 158, 11, 0.4)'
                    : 'rgba(16, 185, 129, 0.25)';
            ctx.lineWidth = 3;
            ctx.stroke();
        }

        // Target arrow (behind, translucent)
        const tLen = (Math.abs(tSpeed) / MAX_SPEED) * radius * 0.85;
        drawArrow(ctx, cx, cy, tLen, -tAngle, TARGET_STROKE, TARGET_COLOR, 3.5);

        // Actual arrow (solid, on top)
        const aLen = (Math.abs(aSpeed) / MAX_SPEED) * radius * 0.85;
        drawArrow(ctx, cx, cy, aLen, -aAngle, ACTUAL_STROKE, ACTUAL_COLOR, 2);

        // Module wheel indicator (small rectangle showing actual steering angle)
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(-aAngle);
        ctx.fillStyle = '#475569';
        ctx.fillRect(-4, -8, 8, 16);
        ctx.strokeStyle = '#64748b';
        ctx.lineWidth = 0.5;
        ctx.strokeRect(-4, -8, 8, 16);
        ctx.restore();

        // Labels
        ctx.fillStyle = LABEL_COLOR;
        ctx.font = 'bold 9px ui-monospace, "Cascadia Code", monospace';
        ctx.textAlign = 'center';
        ctx.fillText(name, cx, cy - radius - 8);

        // Speed labels
        ctx.font = '8px ui-monospace, "Cascadia Code", monospace';
        ctx.fillStyle = VALUE_COLOR;
        ctx.fillText(`${aSpeed.toFixed(2)} m/s`, cx, cy + radius + 14);

        ctx.fillStyle = '#64748b';
        ctx.fillText(`${(aAngle * 180 / Math.PI).toFixed(1)}°`, cx, cy + radius + 25);
    };

    const drawChassisSpeeds = (
        ctx: CanvasRenderingContext2D,
        cx: number, cy: number,
        frame: TelemetryEntry,
    ) => {
        // Calculate chassis velocity from module actuals (avg)
        let vxSum = 0, vySum = 0, omegaSum = 0;
        if (frame.actuals) {
            for (let i = 0; i < 4; i++) {
                const [speed, angle] = frame.actuals[i] || [0, 0];
                vxSum += speed * Math.cos(angle);
                vySum += speed * Math.sin(angle);
            }
            vxSum /= 4;
            vySum /= 4;
        }

        // Linear velocity arrow
        const linearSpeed = Math.hypot(vxSum, vySum);
        const linearAngle = Math.atan2(vySum, vxSum);
        const linearLen = (linearSpeed / MAX_SPEED) * 50;

        if (linearLen > 2) {
            drawArrow(ctx, cx, cy, linearLen, -linearAngle, '#38bdf8', '#38bdf8', 3);
        }

        // Angular velocity arc
        // Use heading delta approximation from observer or compute from module geometry
        const omega = frame.observerVel?.omega ?? 0;
        if (Math.abs(omega) > 0.05) {
            const arcRadius = 20;
            const arcLength = Math.min(Math.abs(omega) / 4.0, 1.0) * Math.PI;
            const startAngle = -Math.PI / 2;
            const endAngle = startAngle + (omega > 0 ? -arcLength : arcLength);

            ctx.beginPath();
            ctx.arc(cx, cy, arcRadius, startAngle, endAngle, omega > 0);
            ctx.strokeStyle = ANGULAR_ARC_COLOR;
            ctx.lineWidth = 2.5;
            ctx.stroke();

            // Arrowhead on arc end
            const tipX = cx + arcRadius * Math.cos(endAngle);
            const tipY = cy + arcRadius * Math.sin(endAngle);
            ctx.beginPath();
            ctx.arc(tipX, tipY, 3, 0, Math.PI * 2);
            ctx.fillStyle = ANGULAR_ARC_COLOR;
            ctx.fill();
        }

        // Speed readout
        ctx.fillStyle = '#64748b';
        ctx.font = '8px ui-monospace, "Cascadia Code", monospace';
        ctx.textAlign = 'center';
        ctx.fillText(`${linearSpeed.toFixed(2)} m/s`, cx, cy + 40);
    };

    const draw = useCallback(() => {
        const canvas = canvasRef.current;
        const container = containerRef.current;
        if (!canvas || !container) return;

        // Responsive canvas sizing
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

        // Clear
        ctx.clearRect(0, 0, w, h);

        // Robot chassis outline (centered)
        const chassisSize = Math.min(w, h) * 0.55;
        const chassisX = (w - chassisSize) / 2;
        const chassisY = (h - chassisSize) / 2;

        // Chassis fill
        ctx.fillStyle = CHASSIS_FILL;
        ctx.strokeStyle = CHASSIS_BORDER;
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.roundRect(chassisX, chassisY, chassisSize, chassisSize, 6);
        ctx.fill();
        ctx.stroke();

        // Front indicator (right side = forward in robot frame)
        ctx.strokeStyle = FRONT_INDICATOR;
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(chassisX + chassisSize, chassisY + 4);
        ctx.lineTo(chassisX + chassisSize, chassisY + chassisSize - 4);
        ctx.stroke();

        // "FRONT" label
        ctx.save();
        ctx.translate(chassisX + chassisSize + 12, chassisY + chassisSize / 2);
        ctx.rotate(-Math.PI / 2);
        ctx.fillStyle = 'rgba(244, 63, 94, 0.4)';
        ctx.font = 'bold 8px ui-monospace, monospace';
        ctx.textAlign = 'center';
        ctx.fillText('FRONT', 0, 0);
        ctx.restore();

        // Module positions (corners of chassis)
        const moduleRadius = chassisSize * 0.22;
        const inset = chassisSize * 0.22;
        const modulePositions = [
            { x: chassisX + chassisSize - inset, y: chassisY + inset,                    name: 'FL' },  // Front-Left
            { x: chassisX + chassisSize - inset, y: chassisY + chassisSize - inset,      name: 'FR' },  // Front-Right
            { x: chassisX + inset,               y: chassisY + chassisSize - inset,      name: 'RR' },  // Rear-Right
            { x: chassisX + inset,               y: chassisY + inset,                    name: 'RL' },  // Rear-Left
        ];

        if (!currentFrame) {
            // No data state
            ctx.fillStyle = '#475569';
            ctx.font = '11px ui-monospace, monospace';
            ctx.textAlign = 'center';
            ctx.fillText('Waiting for telemetry...', w / 2, h / 2);
            return;
        }

        // Draw each module
        modulePositions.forEach((pos, i) => {
            const target = currentFrame.targets?.[i] || [0, 0];
            const actual = currentFrame.actuals?.[i] || [0, 0];
            drawModule(ctx, pos.x, pos.y, moduleRadius, target, actual, pos.name);
        });

        // Draw chassis speeds in center
        drawChassisSpeeds(ctx, w / 2, h / 2, currentFrame);

        // Legend
        const legendX = 12;
        const legendY = h - 40;

        ctx.fillStyle = TARGET_STROKE;
        ctx.fillRect(legendX, legendY, 12, 3);
        ctx.fillStyle = LABEL_COLOR;
        ctx.font = '8px ui-monospace, monospace';
        ctx.textAlign = 'left';
        ctx.fillText('TARGET', legendX + 16, legendY + 4);

        ctx.fillStyle = ACTUAL_COLOR;
        ctx.fillRect(legendX, legendY + 12, 12, 3);
        ctx.fillStyle = LABEL_COLOR;
        ctx.fillText('ACTUAL', legendX + 16, legendY + 16);

        ctx.fillStyle = '#38bdf8';
        ctx.fillRect(legendX, legendY + 24, 12, 3);
        ctx.fillStyle = LABEL_COLOR;
        ctx.fillText('CHASSIS', legendX + 16, legendY + 28);

        // State indicators (top-right)
        const stateX = w - 12;
        ctx.textAlign = 'right';
        ctx.font = 'bold 8px ui-monospace, monospace';

        if (currentFrame.isSnapping) {
            ctx.fillStyle = '#f59e0b';
            ctx.fillText('● SNAP', stateX, 16);
        } else if (currentFrame.isMaintaining) {
            ctx.fillStyle = '#3b82f6';
            ctx.fillText('● HOLD', stateX, 16);
        } else {
            ctx.fillStyle = '#475569';
            ctx.fillText('○ MANUAL', stateX, 16);
        }
    }, [currentFrame]);

    useEffect(() => {
        draw();
    }, [draw]);

    // Resize observer
    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;

        const resizeObserver = new ResizeObserver(() => {
            draw();
        });
        resizeObserver.observe(container);
        return () => resizeObserver.disconnect();
    }, [draw]);

    return (
        <div ref={containerRef} className="w-full h-full bg-slate-950 relative overflow-hidden">
            <canvas
                ref={canvasRef}
                className="block w-full h-full"
            />
            <div className="absolute top-2 left-3 pointer-events-none">
                <span className="text-[8px] font-mono font-bold text-slate-600 uppercase tracking-widest">
                    Swerve Detailer // Module Vectors
                </span>
            </div>
        </div>
    );
};

export default SwerveDetailer;
