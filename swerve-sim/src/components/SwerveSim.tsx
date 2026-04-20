import React, { useEffect, useRef, useState, useCallback } from 'react';
import { SwerveConfig } from '../lib/SwerveLogic';

interface SwerveSimProps {
    showModuleVectors: boolean;
    onTelemetryUpdate: (data: any) => void;
    frameData: any; // The specific frame to render
}

const SwerveSim: React.FC<SwerveSimProps> = ({ showModuleVectors, onTelemetryUpdate, frameData }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const requestRef = useRef<number>(0);
    const socketRef = useRef<WebSocket | null>(null);
    const [isSITL, setIsSITL] = useState(false);
    
    // Virtual Hardware Ref (For rendering the chassis)
    const localPathRef = useRef<{x: number, y: number}[]>([]);
    
    // Scaling constants for drawing
    const PxPerMeter = 140; 
    const FieldWidthPx = 12 * 0.3048 * PxPerMeter; 

    // Handle incoming SITL data in background
    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080');
        socketRef.current = socket;

        socket.onopen = () => {
            console.log('SITL: Connected to Java Server');
            setIsSITL(true);
            (window as any).sitlSocket = socket;
        };

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            
            // Push breadcrumbs to local ref for smooth path rendering
            localPathRef.current.push({ x: data.x, y: data.y });
            if (localPathRef.current.length > 1500) localPathRef.current.shift();

            onTelemetryUpdate({ ...data, mode: 'SITL' });
        };

        socket.onclose = () => {
            console.log('SITL: Disconnected');
            setIsSITL(false);
        };

        return () => socket.close();
    }, [onTelemetryUpdate]);

    const runRobotCycle = useCallback(() => {
        const gamepads = navigator.getGamepads();
        const gp = gamepads[0];
        
        if (gp && socketRef.current?.readyState === WebSocket.OPEN) {
            socketRef.current.send(JSON.stringify({
                drive: -gp.axes[1],
                strafe: -gp.axes[0],
                turn: -gp.axes[2],
                dpad_up: gp.buttons[12].pressed,
                dpad_down: gp.buttons[13].pressed,
                dpad_left: gp.buttons[14].pressed,
                dpad_right: gp.buttons[15].pressed
            }));
        }
    }, []);

    const drawArrow = (ctx: CanvasRenderingContext2D, x: number, y: number, length: number, angle: number, color: string, width: number = 2) => {
        if (Math.abs(length) < 2) return;
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.strokeStyle = color; ctx.fillStyle = color; ctx.lineWidth = width;
        ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(length, 0); ctx.stroke();
        const headSize = Math.min(length * 0.3, 8);
        ctx.beginPath(); ctx.moveTo(length, 0); ctx.lineTo(length - headSize, headSize / 2); ctx.lineTo(length - headSize, headSize / -2);
        ctx.closePath(); ctx.fill(); ctx.restore();
    };

    const animate = useCallback(() => {
        runRobotCycle();

        const ctx = canvasRef.current?.getContext('2d');
        if (ctx && canvasRef.current) {
            const canvas = canvasRef.current;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.save();
            ctx.translate(canvas.width / 2, canvas.height / 2); 
            
            // Grid
            ctx.strokeStyle = '#1e293b'; ctx.lineWidth = 1;
            const step = 0.6096 * PxPerMeter;
            for(let i = -3; i <= 3; i++) {
                ctx.beginPath(); ctx.moveTo(i * step, -FieldWidthPx/2); ctx.lineTo(i * step, FieldWidthPx/2); ctx.stroke();
                ctx.beginPath(); ctx.moveTo(-FieldWidthPx/2, i * step); ctx.lineTo(FieldWidthPx/2, i * step); ctx.stroke();
            }

            // Path Trail (Odometry)
            if (localPathRef.current.length > 2) {
                ctx.strokeStyle = 'rgba(56, 189, 248, 0.2)';
                ctx.lineWidth = 1.5;
                ctx.beginPath();
                ctx.moveTo(localPathRef.current[0].x * PxPerMeter, -localPathRef.current[0].y * PxPerMeter);
                for(let i=1; i<localPathRef.current.length; i++) {
                    ctx.lineTo(localPathRef.current[i].x * PxPerMeter, -localPathRef.current[i].y * PxPerMeter);
                }
                ctx.stroke();
            }

            // Only draw robot if we have data (either live or historical)
            if (frameData) {
                const { x, y, heading, actuals, targets } = frameData;

                ctx.save();
                ctx.translate(x * PxPerMeter, -y * PxPerMeter);
                ctx.rotate(-heading);

                const robotSize = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 + 0.08) * PxPerMeter;
                
                // Chassis Glow
                ctx.shadowBlur = 15;
                ctx.shadowColor = isSITL ? 'rgba(16, 185, 129, 0.4)' : 'rgba(56, 189, 248, 0.4)';
                ctx.strokeStyle = isSITL ? '#10b981' : '#38bdf8'; 
                ctx.lineWidth = 2.5;
                ctx.strokeRect(-robotSize/2, -robotSize/2, robotSize, robotSize);
                ctx.shadowBlur = 0;
                
                // Front indicator
                ctx.strokeStyle = '#f43f5e';
                ctx.beginPath(); ctx.moveTo(robotSize/2, -robotSize/2); ctx.lineTo(robotSize/2, robotSize/2); ctx.stroke();

                // Modules
                const halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PxPerMeter;
                const halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PxPerMeter;
                const offsets = [[halfL, halfW], [halfL, -halfW], [-halfL, -halfW], [-halfL, halfW]];
                
                offsets.forEach((off, i) => {
                    const actual = actuals ? actuals[i] : [0,0];
                    const target = targets ? targets[i] : [0,0];
                    
                    ctx.save();
                    ctx.translate(off[0], -off[1]);

                    // Traction Circle
                    ctx.beginPath();
                    ctx.arc(0, 0, 45, 0, Math.PI * 2);
                    ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
                    ctx.lineWidth = 1;
                    ctx.stroke();
                    
                    // Draw Module Hardware
                    ctx.save();
                    ctx.rotate(-actual[1]);
                    ctx.fillStyle = '#475569';
                    ctx.fillRect(-6, -10, 12, 20);
                    ctx.restore();

                    if (showModuleVectors) {
                        drawArrow(ctx, 0, 0, target[0] * 40, -target[1], 'rgba(244, 63, 94, 0.4)', 3);
                        drawArrow(ctx, 0, 0, actual[0] * 40, -actual[1], '#10b981', 1.5);
                    }
                    ctx.restore();
                });
                ctx.restore();
            }
            ctx.restore();
        }
        requestRef.current = requestAnimationFrame(animate);
    }, [runRobotCycle, showModuleVectors, isSITL, frameData]);

    useEffect(() => {
        requestRef.current = requestAnimationFrame(animate);
        return () => cancelAnimationFrame(requestRef.current);
    }, [animate]);

    return (
        <div className="flex flex-col items-center justify-center h-full bg-slate-950 p-8">
            <div className="relative group">
                <canvas ref={canvasRef} width={700} height={700} className="rounded-2xl shadow-[0_0_80px_rgba(0,0,0,0.6)] border border-slate-800 bg-slate-900/60 backdrop-blur-md transition-all group-hover:border-blue-500/30" />
                
                <div className="absolute top-6 left-6 flex flex-col gap-2 pointer-events-none">
                    <div className="flex items-center gap-2 px-3 py-1 rounded-md bg-slate-900/80 border border-slate-800 backdrop-blur-md">
                        <div className={`w-1.5 h-1.5 rounded-full ${isSITL ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`} />
                        <span className="text-[9px] font-mono font-bold text-slate-300 uppercase tracking-tighter">
                            {isSITL ? 'SITL // LIVE BRIDGE' : 'SITL // SCANNING'}
                        </span>
                    </div>
                </div>
            </div>
            
            <div className="absolute bottom-6 left-6 flex items-center gap-4">
                <div className="text-[10px] font-mono text-slate-500 uppercase tracking-[0.2em] border border-slate-800/30 px-3 py-1 rounded-full bg-slate-900/20">
                    12' x 12' Engineering Sandbox // Metric Units
                </div>
            </div>
        </div>
    );
};

export default SwerveSim;
