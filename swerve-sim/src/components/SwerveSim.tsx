import React, { useEffect, useRef, useCallback } from 'react';
import { SwerveKinematics, SwerveAuditor, MotionSmoother, Pose, SwerveConfig, PinpointEmulator, SwerveModuleEmulator, SwerveVelocityObserver, LowPassFilter } from '../lib/SwerveLogic';

interface SwerveSimProps {
    showModuleVectors: boolean;
    showChassisVector: boolean;
    showRotationVector: boolean;
    onTelemetryUpdate: (data: any) => void;
}

const SwerveSim: React.FC<SwerveSimProps> = ({ showModuleVectors, showChassisVector, showRotationVector, onTelemetryUpdate }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const requestRef = useRef<number>(0);
    
    // Core Engine Instances
    const kinematics = useRef(new SwerveKinematics());
    const smoother = useRef(new MotionSmoother());
    const observer = useRef(new SwerveVelocityObserver());
    
    // Joystick Filters
    const driveFilter  = useRef(new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN));
    const strafeFilter = useRef(new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN));
    const turnFilter   = useRef(new LowPassFilter(SwerveConfig.ROTATION_LPF_GAIN));
    
    // Virtual Hardware Layer
    const pinpoint = useRef(new PinpointEmulator());
    const moduleEmulators = useRef([
        new SwerveModuleEmulator(), // FL
        new SwerveModuleEmulator(), // FR
        new SwerveModuleEmulator(), // RR
        new SwerveModuleEmulator()  // RL
    ]);
    
    const lastTimeRef = useRef(performance.now());
    const accumulatorRef = useRef(0);

    // Scaling constants for drawing
    const PxPerMeter = 140; 
    const FieldWidthPx = 12 * 0.3048 * PxPerMeter; 

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
        const headSize = Math.min(length * 0.3, 8);
        ctx.beginPath();
        ctx.moveTo(length, 0);
        ctx.lineTo(length - headSize, headSize / 2);
        ctx.lineTo(length - headSize, -headSize / 2);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    };

    const runRobotCycle = useCallback((dt: number, timestamp: number) => {
        // 1. Poll Gamepad Input
        const gamepads = navigator.getGamepads();
        let drive = 0, strafe = 0, turn = 0;
        
        if (gamepads[0]) {
            const gp = gamepads[0];
            const deadband = 0.12;
            const applyDeadband = (v: number) => Math.abs(v) < deadband ? 0 : v;
            
            // Filter the inputs
            const rawD = applyDeadband(-gp.axes[1]);
            const rawS = applyDeadband(-gp.axes[0]);
            const rawT = applyDeadband(-gp.axes[2]);

            drive = driveFilter.current.calculate(rawD);
            strafe = strafeFilter.current.calculate(rawS);
            turn = turnFilter.current.calculate(rawT);
            
            turn *= SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
        }

        const driveV = drive * SwerveConfig.MAX_SPEED_MPS;
        const strafeV = strafe * SwerveConfig.MAX_SPEED_MPS;

        // 2. Swerve Logic Pipeline
        const driverTarget = new Pose(driveV, strafeV, turn);
        
        // System Limits (Auditor simulation)
        const rawStates = kinematics.current.toModuleStates(driverTarget.x, driverTarget.y, driverTarget.heading, SwerveConfig.LOOP_TIME_SEC);
        let maxFound = 0;
        rawStates.forEach(s => maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond)));
        const scale = (maxFound > SwerveConfig.MAX_SPEED_MPS) ? (SwerveConfig.MAX_SPEED_MPS / maxFound) : 1.0;
        const systemLimit = new Pose(driverTarget.x * scale, driverTarget.y * scale, driverTarget.heading * scale);

        // Motion Smoothing
        const smoothState = smoother.current.calculate(driverTarget, systemLimit, dt);
        const velocity = smoothState.velocity;

        // Kinematics & Optimization
        const currentAngles = moduleEmulators.current.map(e => e.angleRadians);
        const desiredStates = kinematics.current.toModuleStates(velocity.x, velocity.y, velocity.heading, dt);
        const optimizedStates = SwerveAuditor.optimize(desiredStates, currentAngles, SwerveConfig.MAX_SPEED_MPS);

        // 3. Update Hardware Layer
        pinpoint.current.update(velocity, dt);
        optimizedStates.forEach((os, i) => {
            moduleEmulators.current[i].update(os, dt);
        });

        // 4. Update Observer (Actual Feedback)
        observer.current.update(moduleEmulators.current, kinematics.current);
        const observed = observer.current.observedVelocity;

        // 5. Stream Extensive Telemetry
        onTelemetryUpdate({
            timestamp: timestamp / 1000,
            posX: pinpoint.current.getPose().x,
            posY: pinpoint.current.getPose().y,
            heading: pinpoint.current.getPose().heading,
            velX: velocity.x,
            velY: velocity.y,
            velH: velocity.heading,
            obsX: observed.x,
            obsY: observed.y,
            obsH: observed.heading,
            accelX: smoothState.accel.x,
            accelY: smoothState.accel.y,
            accelH: smoothState.accel.heading,
            jerkX: smoothState.jerk.x,
            jerkY: smoothState.jerk.y,
            jerkH: smoothState.jerk.heading,
            FL_angle: moduleEmulators.current[0].angleRadians * 180 / Math.PI,
            FL_vel: moduleEmulators.current[0].velocityMetersPerSecond,
            FL_dist: moduleEmulators.current[0].distanceMeters,
            FR_angle: moduleEmulators.current[1].angleRadians * 180 / Math.PI,
            FR_vel: moduleEmulators.current[1].velocityMetersPerSecond,
            FR_dist: moduleEmulators.current[1].distanceMeters,
            RR_angle: moduleEmulators.current[2].angleRadians * 180 / Math.PI,
            RR_vel: moduleEmulators.current[2].velocityMetersPerSecond,
            RR_dist: moduleEmulators.current[2].distanceMeters,
            RL_angle: moduleEmulators.current[3].angleRadians * 180 / Math.PI,
            RL_vel: moduleEmulators.current[3].velocityMetersPerSecond,
            RL_dist: moduleEmulators.current[3].distanceMeters
        });

    }, [onTelemetryUpdate]);

    const animate = useCallback((time: number) => {
        const frameDT = (time - lastTimeRef.current) / 1000;
        lastTimeRef.current = time;

        if (SwerveConfig.USE_REALTIME) {
            runRobotCycle(frameDT, time);
        } else {
            // Fixed Step Mode (Accumulator)
            accumulatorRef.current += frameDT;
            while (accumulatorRef.current >= SwerveConfig.LOOP_TIME_SEC) {
                runRobotCycle(SwerveConfig.LOOP_TIME_SEC, time);
                accumulatorRef.current -= SwerveConfig.LOOP_TIME_SEC;
            }
        }

        // Render Frame
        const ctx = canvasRef.current?.getContext('2d');
        if (ctx && canvasRef.current) {
            const canvas = canvasRef.current;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.save();
            ctx.translate(canvas.width / 2, canvas.height / 2); 
            
            // Grid
            ctx.strokeStyle = '#1e293b';
            ctx.lineWidth = 1;
            const step = 0.6096 * PxPerMeter; // 2ft steps
            for(let i = -3; i <= 3; i++) {
                ctx.beginPath(); ctx.moveTo(i * step, -FieldWidthPx/2); ctx.lineTo(i * step, FieldWidthPx/2); ctx.stroke();
                ctx.beginPath(); ctx.moveTo(-FieldWidthPx/2, i * step); ctx.lineTo(FieldWidthPx/2, i * step); ctx.stroke();
            }

            // Robot Pose From Virtual Pinpoint
            const pose = pinpoint.current.getPose();
            ctx.translate(pose.x * PxPerMeter, -pose.y * PxPerMeter);
            ctx.rotate(-pose.heading);

            const robotSize = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 + 0.08) * PxPerMeter;
            ctx.strokeStyle = '#38bdf8';
            ctx.lineWidth = 2.5;
            ctx.strokeRect(-robotSize/2, -robotSize/2, robotSize, robotSize);
            
            // Front indicator
            ctx.strokeStyle = '#f43f5e';
            ctx.beginPath(); ctx.moveTo(robotSize/2, -robotSize/2); ctx.lineTo(robotSize/2, robotSize/2); ctx.stroke();

            // Modules
            const halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254 / 2) * PxPerMeter;
            const halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254 / 2) * PxPerMeter;
            const offsets = [[halfL, halfW], [halfL, -halfW], [-halfL, -halfW], [-halfL, halfW]];
            
            offsets.forEach((off, i) => {
                const emu = moduleEmulators.current[i];
                ctx.save();
                ctx.translate(off[0], -off[1]);
                ctx.rotate(-emu.angleRadians);
                ctx.fillStyle = '#475569';
                ctx.fillRect(-6, -10, 12, 20);
                ctx.restore();

                if (showModuleVectors) {
                    drawArrow(ctx, off[0], -off[1], emu.velocityMetersPerSecond * 40, -emu.angleRadians, '#10b981');
                }
            });

            // Chassis Vectors
            const vel = smoother.current.currentVelocity;
            if (showChassisVector) {
                drawArrow(ctx, 0, 0, Math.hypot(vel.x, vel.y) * 50, -Math.atan2(vel.y, vel.x), '#38bdf8', 3);
            }
            if (showRotationVector && Math.abs(vel.heading) > 0.1) {
                ctx.strokeStyle = '#c084fc'; ctx.lineWidth = 3; ctx.beginPath();
                ctx.arc(0, 0, robotSize * 0.7, -Math.PI/4, -Math.PI/4 + (vel.heading * 0.4), vel.heading < 0);
                ctx.stroke();
            }

            ctx.restore();
        }

        requestRef.current = requestAnimationFrame(animate);
    }, [runRobotCycle, showModuleVectors, showChassisVector, showRotationVector]);

    useEffect(() => {
        requestRef.current = requestAnimationFrame(animate);
        return () => cancelAnimationFrame(requestRef.current);
    }, [animate]);

    return (
        <div className="flex items-center justify-center h-full bg-slate-950 p-8">
            <canvas ref={canvasRef} width={700} height={700} className="rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.5)] border border-slate-800 bg-slate-900/40" />
            
            <div className="absolute bottom-6 left-6 text-[10px] font-mono text-slate-600 uppercase tracking-widest border border-slate-800/50 px-3 py-1 rounded-full bg-slate-900/50 backdrop-blur-sm">
                12' x 12' Engineering Sandbox // Metric Units
            </div>
        </div>
    );
};

export default SwerveSim;
