import React, { useEffect, useRef, useState, useCallback } from 'react';
import { SwerveKinematics, SwerveAuditor, MotionSmoother, Pose, SwerveConfig } from '../lib/SwerveLogic';

interface SwerveSimProps {
    showModuleVectors: boolean;
    showChassisVector: boolean;
    showRotationVector: boolean;
}

const SwerveSim: React.FC<SwerveSimProps> = ({ showModuleVectors, showChassisVector, showRotationVector }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const requestRef = useRef<number>(0);
    
    // Core Engine Instances
    const kinematics = useRef(new SwerveKinematics());
    const smoother = useRef(new MotionSmoother());
    
    // State
    const [robotPose, setRobotPose] = useState(new Pose(0, 0, 0));
    const [lastTime, setLastTime] = useState(performance.now());
    
    // Virtual Robot State for drawing
    const currentVel = useRef(new Pose(0, 0, 0));
    const moduleStates = useRef([0, 0, 0, 0].map(() => ({ angle: 0, speed: 0 })));

    // Constants for drawing
    const PxPerMeter = 150; // Scaling factor
    const FieldWidthPx = 12 * 0.3048 * PxPerMeter; // 12ft in meters * scaling

    const drawArrow = (ctx: CanvasRenderingContext2D, x: number, y: number, length: number, angle: number, color: string, width: number = 2) => {
        if (Math.abs(length) < 2) return;
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.strokeStyle = color;
        ctx.fillStyle = color;
        ctx.lineWidth = width;
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(length, 0);
        ctx.stroke();
        
        // Arrow head
        const headSize = Math.min(length * 0.3, 10);
        ctx.beginPath();
        ctx.moveTo(length, 0);
        ctx.lineTo(length - headSize, headSize / 2);
        ctx.lineTo(length - headSize, -headSize / 2);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    };

    const animate = useCallback((time: number) => {
        const dt = (time - lastTime) / 1000;
        setLastTime(time);

        // 1. Poll Gamepad
        const gamepads = navigator.getGamepads();
        let drive = 0, strafe = 0, turn = 0;
        
        if (gamepads[0]) {
            const gp = gamepads[0];
            // Deadband
            const deadband = 0.1;
            const applyDeadband = (v: number) => Math.abs(v) < deadband ? 0 : v;
            
            drive = applyDeadband(-gp.axes[1]);  // Left Stick Y (Positive Up)
            strafe = applyDeadband(-gp.axes[0]); // Left Stick X (Positive Left)
            turn = applyDeadband(-gp.axes[2]);   // Right Stick X (Positive Left)
            
            // Normalize Turn (Gamepad API is -1 to 1)
            turn *= SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
        }

        const driveV = drive * SwerveConfig.MAX_SPEED_MPS;
        const strafeV = strafe * SwerveConfig.MAX_SPEED_MPS;
        const turnV = turn; // Already rad/s

        // 2. Swerve Pipeline (Digital Twin)
        const driverTarget = new Pose(driveV, strafeV, turnV);
        
        // Calculate limit (Simulation of Auditor Saturation)
        const rawStates = kinematics.current.toModuleStates(driverTarget.x, driverTarget.y, driverTarget.heading, 0.02);
        let maxFound = 0;
        rawStates.forEach(s => maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond)));
        const scale = maxFound > SwerveConfig.MAX_SPEED_MPS ? SwerveConfig.MAX_SPEED_MPS / maxFound : 1.0;
        const systemLimit = new Pose(driverTarget.x * scale, driverTarget.y * scale, driverTarget.heading * scale);

        const smoothed = smoother.current.calculate(driverTarget, systemLimit, dt);
        currentVel.current = smoothed;

        // Perform IK for individual wheels (To show vectors)
        const curAngles = moduleStates.current.map(s => s.angle);
        const states = kinematics.current.toModuleStates(smoothed.x, smoothed.y, smoothed.heading, dt);
        const optimized = SwerveAuditor.optimize(states, curAngles, SwerveConfig.MAX_SPEED_MPS);
        
        // Update module states (Simulate module feedback)
        optimized.forEach((s, i) => {
            moduleStates.current[i].angle = s.angleRadians;
            moduleStates.current[i].speed = s.speedMetersPerSecond;
        });

        // 3. Update Robot Position (Physics Integration)
        setRobotPose(prev => {
            // Field-Centric Transformation for Sim
            const cos = Math.cos(prev.heading);
            const sin = Math.sin(prev.heading);
            const dxField = (smoothed.x * cos - smoothed.y * sin) * dt;
            const dyField = (smoothed.x * sin + smoothed.y * cos) * dt;
            
            return new Pose(
                prev.x + dxField,
                prev.y + dyField,
                prev.heading + smoothed.heading * dt
            );
        });

        // 4. Render
        const ctx = canvasRef.current?.getContext('2d');
        if (ctx && canvasRef.current) {
            const canvas = canvasRef.current;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            
            ctx.save();
            ctx.translate(canvas.width / 2, canvas.height / 2); // Center of field
            
            // Draw Grid
            ctx.strokeStyle = '#1a1a1a';
            ctx.lineWidth = 1;
            const step = 0.3048 * PxPerMeter; // 1ft steps
            for(let i = -6; i <= 6; i++) {
                ctx.beginPath(); ctx.moveTo(i * step, -FieldWidthPx/2); ctx.lineTo(i * step, FieldWidthPx/2); ctx.stroke();
                ctx.beginPath(); ctx.moveTo(-FieldWidthPx/2, i * step); ctx.lineTo(FieldWidthPx/2, i * step); ctx.stroke();
            }

            // Draw Field Border
            ctx.strokeStyle = '#444';
            ctx.strokeRect(-FieldWidthPx/2, -FieldWidthPx/2, FieldWidthPx, FieldWidthPx);

            // Draw Robot
            ctx.translate(robotPose.x * PxPerMeter, -robotPose.y * PxPerMeter); // Canvas Y is inverted
            ctx.rotate(-robotPose.heading); // Canvas rotation is clockwise

            const robotSize = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 + 0.1) * PxPerMeter; // Chassis size in px
            
            // Chassis Box
            ctx.strokeStyle = '#3b82f6';
            ctx.lineWidth = 3;
            ctx.strokeRect(-robotSize/2, -robotSize/2, robotSize, robotSize);

            // FRONT INDICATOR (Different color line)
            ctx.strokeStyle = '#f43f5e'; // Red-ish Front
            ctx.beginPath();
            ctx.moveTo(robotSize/2, -robotSize/2);
            ctx.lineTo(robotSize/2, robotSize/2);
            ctx.stroke();

            // Draw Modules
            const halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PxPerMeter;
            const halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PxPerMeter;
            const positions = [[halfL, halfW], [halfL, -halfW], [-halfL, -halfW], [-halfL, halfW]];
            
            positions.forEach((pos, i) => {
                const mx = pos[0];
                const my = -pos[1];
                const state = moduleStates.current[i];
                
                ctx.save();
                ctx.translate(mx, my);
                ctx.rotate(-state.angle);
                
                // Wheel
                ctx.fillStyle = '#666';
                ctx.fillRect(-8, -12, 16, 24);
                
                ctx.restore();

                // Module Vectors (Green)
                if (showModuleVectors) {
                    drawArrow(ctx, mx, my, state.speed * 50, -state.angle, '#10b981', 2);
                }
            });

            // Chassis Translation Vector (Blue)
            if (showChassisVector) {
                const chassisSpeed = Math.hypot(smoothed.x, smoothed.y);
                const chassisAngle = Math.atan2(smoothed.y, smoothed.x);
                drawArrow(ctx, 0, 0, chassisSpeed * 60, -chassisAngle, '#3b82f6', 4);
            }

            // Rotation Vector (Purple Arc)
            if (showRotationVector && Math.abs(smoothed.heading) > 0.05) {
                ctx.strokeStyle = '#a855f7';
                ctx.lineWidth = 4;
                ctx.beginPath();
                const radius = robotSize * 0.6;
                const startAngle = -Math.PI/4;
                const endAngle = startAngle + (smoothed.heading * 0.5);
                ctx.arc(0, 0, radius, startAngle, endAngle, smoothed.heading < 0);
                ctx.stroke();
            }

            ctx.restore();
        }

        requestRef.current = requestAnimationFrame(animate);
    }, [lastTime, robotPose, showModuleVectors, showChassisVector, showRotationVector]);

    useEffect(() => {
        requestRef.current = requestAnimationFrame(animate);
        return () => {
            if (requestRef.current) cancelAnimationFrame(requestRef.current);
        };
    }, [animate]);

    return (
        <div className="flex flex-col items-center justify-center h-full bg-background overflow-hidden relative">
            <canvas 
                ref={canvasRef} 
                width={800} 
                height={800} 
                className="rounded-lg shadow-2xl border border-gray-800"
            />
            
            {/* Legend / Overlay */}
            <div className="absolute top-4 left-4 p-4 bg-black/60 backdrop-blur-md rounded-xl border border-white/10 pointer-events-none">
                <div className="text-xs font-mono text-gray-400 mb-2 uppercase tracking-widest">Digital Twin Telemetry</div>
                <div className="grid grid-cols-2 gap-x-6 gap-y-1 font-mono">
                    <span className="text-gray-500">Pose X:</span> <span className="text-blue-400">{robotPose.x.toFixed(2)}m</span>
                    <span className="text-gray-500">Pose Y:</span> <span className="text-blue-400">{robotPose.y.toFixed(2)}m</span>
                    <span className="text-gray-500">Heading:</span> <span className="text-purple-400">{(robotPose.heading * 180 / Math.PI).toFixed(1)}°</span>
                    <span className="text-gray-500">Linear V:</span> <span className="text-green-400">{Math.hypot(currentVel.current.x, currentVel.current.y).toFixed(2)}m/s</span>
                </div>
            </div>
            
            <div className="absolute bottom-4 right-4 text-xs font-mono text-gray-500">
                12' x 12' FTC Field Environment
            </div>
        </div>
    );
};

export default SwerveSim;
