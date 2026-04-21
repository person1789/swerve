import React, { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { SwerveConfig } from '../lib/SwerveLogic';
import { Maximize2, MousePointer2 } from 'lucide-react';

interface SwerveSimProps {
    showModuleVectors: boolean;
    onTelemetryUpdate: (data: any) => void;
    frameData: any; // The specific frame to render
}

interface Viewport {
    x: number;
    y: number;
    zoom: number;
}

const SwerveSim: React.FC<SwerveSimProps> = ({ showModuleVectors, onTelemetryUpdate, frameData }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);
    const requestRef = useRef<number>(0);
    const socketRef = useRef<WebSocket | null>(null);
    const [isSITL, setIsSITL] = useState(false);
    
    // Viewport State
    const [viewport, setViewport] = useState<Viewport>({ x: 0, y: 0, zoom: 1 });
    const [isDragging, setIsDragging] = useState(false);
    const [lastMousePos, setLastMousePos] = useState({ x: 0, y: 0 });
    const [mouseWorldPos, setMouseWorldPos] = useState({ x: 0, y: 0 });
    
    // Virtual Hardware Ref (For rendering path)
    const localPathRef = useRef<{x: number, y: number}[]>([]);
    
    const PxPerMeter = 140; 
    const FieldWidthPx = 12 * 0.3048 * PxPerMeter; 

    // Handle SITL connection
    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080');
        socketRef.current = socket;
        socket.onopen = () => { setIsSITL(true); (window as any).sitlSocket = socket; };
        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            localPathRef.current.push({ x: data.x, y: data.y });
            if (localPathRef.current.length > 3000) localPathRef.current.shift();
            onTelemetryUpdate({ ...data, mode: 'SITL' });
        };
        socket.onclose = () => setIsSITL(false);
        return () => socket.close();
    }, [onTelemetryUpdate]);

    const runRobotCycle = useCallback(() => {
        const gamepads = navigator.getGamepads();
        const gp = gamepads[0];
        if (gp && socketRef.current?.readyState === WebSocket.OPEN) {
            socketRef.current.send(JSON.stringify({
                drive: -gp.axes[1], strafe: -gp.axes[0], turn: -gp.axes[2],
                dpad_up: gp.buttons[12].pressed, dpad_down: gp.buttons[13].pressed,
                dpad_left: gp.buttons[14].pressed, dpad_right: gp.buttons[15].pressed
            }));
        }
    }, []);

    // Conversion Helpers (Meters -> Pixels)
    const worldToScreen = useCallback((x: number, y: number, view: Viewport) => {
        if (!canvasRef.current) return { x: 0, y: 0 };
        const midX = canvasRef.current.width / 2;
        const midY = canvasRef.current.height / 2;
        return {
            x: midX + view.x + (x * PxPerMeter * view.zoom),
            y: midY + view.y - (y * PxPerMeter * view.zoom)
        };
    }, []);

    const screenToWorld = useCallback((sx: number, sy: number, view: Viewport) => {
        if (!canvasRef.current) return { x: 0, y: 0 };
        const midX = canvasRef.current.width / 2;
        const midY = canvasRef.current.height / 2;
        return {
            x: (sx - midX - view.x) / (PxPerMeter * view.zoom),
            y: -(sy - midY - view.y) / (PxPerMeter * view.zoom)
        };
    }, []);

    // Interaction Handlers
    const handleMouseDown = (e: React.MouseEvent) => {
        if (e.button === 0 || e.button === 1 || e.button === 2) { // Allow all buttons to drag for accessibility
            setIsDragging(true);
            setLastMousePos({ x: e.clientX, y: e.clientY });
        }
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (!canvasRef.current) return;
        const rect = canvasRef.current.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        setMouseWorldPos(screenToWorld(sx, sy, viewport));

        if (isDragging) {
            const dx = e.clientX - lastMousePos.x;
            const dy = e.clientY - lastMousePos.y;
            setViewport(v => ({ ...v, x: v.x + dx, y: v.y + dy }));
            setLastMousePos({ x: e.clientX, y: e.clientY });
        }
    };

    const handleMouseUp = () => setIsDragging(false);

    const handleWheel = (e: React.WheelEvent) => {
        const delta = -e.deltaY;
        const factor = delta > 0 ? 1.1 : 0.9;
        setViewport(v => {
            const newZoom = Math.min(Math.max(v.zoom * factor, 0.1), 10);
            return { ...v, zoom: newZoom };
        });
    };

    const resetView = () => setViewport({ x: 0, y: 0, zoom: 1 });

    const drawArrow = (ctx: CanvasRenderingContext2D, x: number, y: number, length: number, angle: number, color: string, width: number = 2) => {
        if (Math.abs(length) < 2) return;
        ctx.save(); ctx.translate(x, y); ctx.rotate(angle);
        ctx.strokeStyle = color; ctx.fillStyle = color; ctx.lineWidth = width;
        ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(length, 0); ctx.stroke();
        const headSize = Math.min(length * 0.3, 8);
        ctx.beginPath(); ctx.moveTo(length, 0); ctx.lineTo(length - headSize, headSize/2); ctx.lineTo(length - headSize, -headSize/2);
        ctx.closePath(); ctx.fill(); ctx.restore();
    };

    const animate = useCallback(() => {
        runRobotCycle();
        const ctx = canvasRef.current?.getContext('2d');
        if (!ctx || !canvasRef.current) { requestRef.current = requestAnimationFrame(animate); return; }
        
        const { width, height } = canvasRef.current;
        ctx.clearRect(0, 0, width, height);

        ctx.save();
        ctx.translate(width/2 + viewport.x, height/2 + viewport.y);
        ctx.scale(viewport.zoom, viewport.zoom);

        // 1. Draw Grid
        ctx.strokeStyle = '#1e293b'; ctx.lineWidth = 1/viewport.zoom;
        const step = 0.6096 * PxPerMeter; // 2ft grid
        for(let i = -6; i <= 6; i++) {
            ctx.beginPath(); ctx.moveTo(i * step, -FieldWidthPx); ctx.lineTo(i * step, FieldWidthPx); ctx.stroke();
            ctx.beginPath(); ctx.moveTo(-FieldWidthPx, i * step); ctx.lineTo(FieldWidthPx, i * step); ctx.stroke();
        }

        // 2. Path Trail
        if (localPathRef.current.length > 2) {
            ctx.strokeStyle = 'rgba(56, 189, 248, 0.2)'; ctx.lineWidth = 1.5/viewport.zoom;
            ctx.beginPath();
            ctx.moveTo(localPathRef.current[0].x * PxPerMeter, -localPathRef.current[0].y * PxPerMeter);
            for(let i=1; i<localPathRef.current.length; i++) {
                ctx.lineTo(localPathRef.current[i].x * PxPerMeter, -localPathRef.current[i].y * PxPerMeter);
            }
            ctx.stroke();
        }

        // 3. Render Robot
        if (frameData) {
            const { x, y, heading, actuals, targets } = frameData;
            const robotSize = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 + 0.08) * PxPerMeter;
            const halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PxPerMeter;
            const halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PxPerMeter;
            const moduleOffsets = [[halfL, halfW], [halfL, -halfW], [-halfL, -halfW], [-halfL, halfW]];

            ctx.save();
            ctx.translate(x * PxPerMeter, -y * PxPerMeter);
            ctx.rotate(-heading);

            // A. Ghost Robot (Targets)
            if (targets && showModuleVectors) {
                ctx.save();
                ctx.globalAlpha = 0.2;
                ctx.setLineDash([5, 5]);
                ctx.strokeStyle = '#f43f5e'; ctx.lineWidth = 1;
                ctx.strokeRect(-robotSize/2, -robotSize/2, robotSize, robotSize);
                ctx.setLineDash([]);
                ctx.globalAlpha = 1;
                ctx.restore();
            }

            // B. Actual Chassis
            ctx.shadowBlur = 15/viewport.zoom;
            ctx.shadowColor = isSITL ? 'rgba(16, 185, 129, 0.4)' : 'rgba(56, 189, 248, 0.4)';
            ctx.strokeStyle = isSITL ? '#10b981' : '#38bdf8'; 
            ctx.lineWidth = 2.5/viewport.zoom;
            ctx.strokeRect(-robotSize/2, -robotSize/2, robotSize, robotSize);
            ctx.shadowBlur = 0;
            
            // C. Modules
            moduleOffsets.forEach((off, i) => {
                const actual = actuals?.[i] || [0,0];
                const target = targets?.[i] || [0,0];
                
                ctx.save();
                ctx.translate(off[0], -off[1]);

                // Target Ghost Wheel
                if (showModuleVectors) {
                    ctx.save(); ctx.rotate(-target[1]); ctx.globalAlpha = 0.2;
                    ctx.fillStyle = '#f43f5e'; ctx.fillRect(-4, -8, 8, 16);
                    ctx.restore();
                }

                // Actual Wheel
                ctx.save(); ctx.rotate(-actual[1]);
                ctx.fillStyle = '#475569'; ctx.fillRect(-6, -10, 12, 20);
                ctx.restore();

                if (showModuleVectors) {
                    drawArrow(ctx, 0, 0, target[0] * 40, -target[1], 'rgba(244, 63, 94, 0.5)', 2/viewport.zoom);
                    drawArrow(ctx, 0, 0, actual[0] * 40, -actual[1], '#10b981', 1.5/viewport.zoom);
                }
                ctx.restore();
            });
            ctx.restore();
        }

        ctx.restore();
        requestRef.current = requestAnimationFrame(animate);
    }, [runRobotCycle, showModuleVectors, isSITL, frameData, viewport]);

    useEffect(() => {
        requestRef.current = requestAnimationFrame(animate);
        return () => cancelAnimationFrame(requestRef.current);
    }, [animate]);

    return (
        <div 
            ref={containerRef}
            className="flex flex-col h-full bg-slate-950 overflow-hidden relative cursor-crosshair select-none"
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onWheel={handleWheel}
            onContextMenu={e => e.preventDefault()}
        >
            <canvas 
                ref={canvasRef} 
                width={700} height={700} 
                className="w-full h-full"
            />
            
            {/* HUD: Connection Status */}
            <div className="absolute top-4 left-4 flex flex-col gap-2 pointer-events-none">
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-slate-900/90 border border-slate-800 backdrop-blur-sm">
                    <div className={`w-1.5 h-1.5 rounded-full ${isSITL ? 'bg-emerald-500 animate-pulse shadow-[0_0_8px_#10b981]' : 'bg-red-500 shadow-[0_0_8px_#ef4444]'}`} />
                    <span className="text-[9px] font-mono font-black text-slate-300 uppercase tracking-widest">
                        {isSITL ? 'SITL_PRO_ENGINE' : 'ENGINE_DISCONNECT'}
                    </span>
                    {isSITL && <span className="text-[8px] text-slate-500 ml-2">50HZ 1:1 SYNC</span>}
                </div>
            </div>

            {/* HUD: Interaction Controls */}
            <div className="absolute top-4 right-4 flex items-center gap-2">
                <button 
                    onClick={resetView}
                    className="p-2 rounded-md bg-slate-900/90 border border-slate-800 text-slate-400 hover:text-white hover:bg-slate-800 transition-all flex items-center gap-2 text-[9px] uppercase font-bold tracking-widest"
                >
                    <Maximize2 className="w-3.5 h-3.5" /> Reset View
                </button>
            </div>

            {/* HUD: Coordinates & Units */}
            <div className="absolute bottom-4 left-4 flex items-end gap-6 pointer-events-none">
                <div className="flex flex-col gap-0.5">
                    <div className="flex items-center gap-2 px-3 py-1 bg-slate-900/80 border border-slate-800/60 rounded text-[9px] font-mono text-slate-400">
                        <MousePointer2 className="w-3 h-3 text-blue-500" />
                        <span className="text-slate-200">X: {mouseWorldPos.x.toFixed(3)}m</span>
                        <span className="text-slate-500">/</span>
                        <span className="text-slate-200">Y: {mouseWorldPos.y.toFixed(3)}m</span>
                    </div>
                    <div className="text-[8px] font-mono text-slate-600 px-3 uppercase tracking-tighter">
                        Converted: {(mouseWorldPos.x * 39.37).toFixed(1)}" x {(mouseWorldPos.y * 39.37).toFixed(1)}"
                    </div>
                </div>
                
                <div className="flex items-center gap-4 h-6 px-3 bg-slate-950/40 rounded-full border border-slate-800/30">
                    <div className="text-[8px] font-mono text-slate-600 uppercase tracking-widest">Engineering Sandbox // 12ft Grid</div>
                </div>
            </div>
        </div>
    );
};

export default SwerveSim;

