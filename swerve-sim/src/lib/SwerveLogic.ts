/**
 * SwerveLogic: A TypeScript port of the robot's Java swerve drivetrain logic.
 * Enhanced with Virtual Hardware emulators for high-fidelity simulation.
 */

export const SwerveConfig = {
    // Robot Physicals
    TRACK_WIDTH_IN: 9.921,
    WHEEL_BASE_IN: 9.927,
    
    // Performance Limits
    MAX_SPEED_MPS: 1.35,
    MAX_ANGULAR_VELOCITY_RAD_S: 4.0,
    MAX_ACCEL: 3.0,
    MAX_JERK: 10.0,
    
    // Control Smoothing
    TRANSLATION_LPF_GAIN: 0.20,
    ROTATION_LPF_GAIN: 0.25,
    OBSERVER_LPF_GAIN: 0.15,

    // Physics & Power
    DRIVE_KS: 1.05,
    DRIVE_KV: 4.2,
    DRIVE_KA: 0.45,
    BATTERY_VOLTAGE: 12.0,
    
    // PID Gains
    STEER_P: 0.35,
    DRIVE_P: 0.1,
    HEADING_P: 1.0,
    SNAP_P: 2.5,
    
    // Simulation Settings
    LOOP_TIME_SEC: 0.020,
    USE_REALTIME: true,
};

/**
 * LowPassFilter: Simple exponential moving average.
 */
export class LowPassFilter {
    private lastOutput: number = 0;
    private alpha: number;

    constructor(alpha: number) { this.alpha = alpha; }
    
    calculate(input: number): number {
        const output = this.alpha * input + (1.0 - this.alpha) * this.lastOutput;
        this.lastOutput = output;
        return output;
    }
    
    reset(val: number = 0) { this.lastOutput = val; }
}

export class MathUtil {
    static normalizeAngle(angle: number): number {
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    static angleError(current: number, target: number): number {
        return this.normalizeAngle(target - current);
    }

    static clamp(value: number, min: number, max: number): number {
        return Math.max(min, Math.min(max, value));
    }
}

export class Pose {
    x: number;
    y: number;
    heading: number;

    constructor(x: number, y: number, heading: number) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }
    
    copy() { return new Pose(this.x, this.y, this.heading); }
}

export class SwerveModuleState {
    speedMetersPerSecond: number;
    angleRadians: number;

    constructor(speedMetersPerSecond: number, angleRadians: number) {
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.angleRadians = angleRadians;
    }

    static fromVector(vx: number, vy: number): SwerveModuleState {
        const speed = Math.hypot(vx, vy);
        const angle = Math.atan2(vy, vx);
        return new SwerveModuleState(speed, angle);
    }
    copy() { return new SwerveModuleState(this.speedMetersPerSecond, this.angleRadians); }
}

/**
 * Virtual Sensor Emulators
 */

export class PinpointEmulator {
    private pose: Pose = new Pose(0, 0, 0);

    update(velocity: Pose, dt: number) {
        const dx = velocity.x * Math.cos(this.pose.heading) - velocity.y * Math.sin(this.pose.heading);
        const dy = velocity.x * Math.sin(this.pose.heading) + velocity.y * Math.cos(this.pose.heading);
        
        this.pose.x += dx * dt;
        this.pose.y += dy * dt;
        this.pose.heading = MathUtil.normalizeAngle(this.pose.heading + velocity.heading * dt);
    }

    getPose(): Pose { return this.pose.copy(); }
    reset() { this.pose = new Pose(0, 0, 0); }
}

export class SwerveModuleEmulator {
    angleRadians: number = 0;
    distanceMeters: number = 0;
    velocityMetersPerSecond: number = 0;
    currentAmps: number = 0;

    update(targetState: SwerveModuleState, dt: number) {
        this.angleRadians = targetState.angleRadians;
        
        // Simulating some inertia/lag and current draw
        const targetV = targetState.speedMetersPerSecond;
        const dv = (targetV - this.velocityMetersPerSecond);
        
        // Very basic "physics" for sim visualization
        this.velocityMetersPerSecond += dv * (dt / (dt + 0.05)); // 50ms lag
        this.distanceMeters += this.velocityMetersPerSecond * dt;
        
        // Sim current: Base draw + draw from acceleration
        this.currentAmps = 1.0 + Math.abs(this.velocityMetersPerSecond) * 2.0 + Math.abs(dv/dt) * 1.5;
    }

    getCurrentState(): SwerveModuleState {
        return new SwerveModuleState(this.velocityMetersPerSecond, this.angleRadians);
    }
}

export class SwerveKinematics {
    private halfW: number;
    private halfL: number;
    private moduleOffsets: {x: number, y: number}[];

    constructor() {
        this.halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2.0;
        this.halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254) / 2.0;
        this.moduleOffsets = [
            {x:  this.halfL, y:  this.halfW}, // FL
            {x:  this.halfL, y: -this.halfW}, // FR
            {x: -this.halfL, y: -this.halfW}, // RR
            {x: -this.halfL, y:  this.halfW}  // RL
        ];
    }

    /**
     * Convert chassis-level velocity into four module states using second-order discretization.
     */
    toModuleStates(vx: number, vy: number, omega: number, dt: number): SwerveModuleState[] {
        const angleRad = omega * dt;
        let vxCorr = vx;
        let vyCorr = vy;
        
        if (Math.abs(angleRad) > 1e-6) {
            const sin = Math.sin(angleRad);
            const cos = Math.cos(angleRad);
            const s = sin / angleRad;
            const c = (1.0 - cos) / angleRad;
            vxCorr = vx * s - vy * c;
            vyCorr = vx * c + vy * s;
        }

        return this.moduleOffsets.map((off) => {
            const moduleVx = vxCorr - omega * off.y;
            const moduleVy = vyCorr + omega * off.x;
            return SwerveModuleState.fromVector(moduleVx, moduleVy);
        });
    }

    toChassisSpeeds(states: SwerveModuleState[]): Pose {
        let vx = 0, vy = 0, omega = 0;
        this.moduleOffsets.forEach((off, i) => {
            const lx = off.x, ly = off.y;
            const s = states[i].speedMetersPerSecond;
            const a = states[i].angleRadians;
            const mvx = s * Math.cos(a);
            const mvy = s * Math.sin(a);
            vx += mvx;
            vy += mvy;
            omega += (lx * mvy - ly * mvx) / (lx * lx + ly * ly);
        });
        return new Pose(vx / 4, vy / 4, omega / 4);
    }
}

export class SwerveVelocityObserver {
    observedVelocity: Pose = new Pose(0, 0, 0);

    update(emulators: SwerveModuleEmulator[], kinematics: SwerveKinematics) {
        const states = emulators.map(e => e.getCurrentState());
        const rawVel = kinematics.toChassisSpeeds(states);
        const alpha = SwerveConfig.OBSERVER_LPF_GAIN;
        this.observedVelocity = new Pose(
            this.observedVelocity.x * (1 - alpha) + rawVel.x * alpha,
            this.observedVelocity.y * (1 - alpha) + rawVel.y * alpha,
            this.observedVelocity.heading * (1 - alpha) + rawVel.heading * alpha
        );
    }
}

export class SwerveAuditor {
    private static FLIP_THRESHOLD = Math.PI / 2.0;

    static optimize(desiredStates: SwerveModuleState[], currentAnglesRad: number[], maxSpeed: number): SwerveModuleState[] {
        const optimized = desiredStates.map((state, i) => this.optimizeSingle(state.copy(), currentAnglesRad[i]));
        this.normalizeByMax(optimized, maxSpeed);
        return optimized;
    }

    static optimizeSingle(state: SwerveModuleState, currentAngle: number): SwerveModuleState {
        const error = MathUtil.angleError(currentAngle, state.angleRadians);
        if (Math.abs(error) > this.FLIP_THRESHOLD) {
            state.speedMetersPerSecond *= -1.0;
            state.angleRadians = MathUtil.normalizeAngle(state.angleRadians + Math.PI);
        }
        return state;
    }

    static normalizeByMax(states: SwerveModuleState[], maxSpeed: number) {
        let maxFound = 0.0;
        for (const s of states) {
            maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond));
        }
        if (maxFound > maxSpeed && maxFound > 1e-9) {
            const scale = maxSpeed / maxFound;
            for (const s of states) s.speedMetersPerSecond *= scale;
        }
    }
}

export interface SmootherState {
    velocity: Pose;
    accel: Pose;
    jerk: Pose;
}

export class MotionSmoother {
    currentVelocity = new Pose(0, 0, 0);
    currentAcceleration = new Pose(0, 0, 0);
    lastDriverIntent = new Pose(0, 0, 0);
    lastJerk = new Pose(0, 0, 0);

    calculate(driverTarget: Pose, systemLimit: Pose, dt: number): SmootherState {
        if (dt <= 0) return { velocity: this.currentVelocity.copy(), accel: this.currentAcceleration.copy(), jerk: new Pose(0, 0, 0) };

        this.currentVelocity.x = this.smoothAxis(this.currentVelocity.x, driverTarget.x, systemLimit.x, this.lastDriverIntent.x, 0, dt);
        this.currentVelocity.y = this.smoothAxis(this.currentVelocity.y, driverTarget.y, systemLimit.y, this.lastDriverIntent.y, 1, dt);
        this.currentVelocity.heading = this.smoothAxis(this.currentVelocity.heading, driverTarget.heading, systemLimit.heading, this.lastDriverIntent.heading, 2, dt);

        this.lastDriverIntent = driverTarget.copy();
        
        return {
            velocity: this.currentVelocity.copy(),
            accel: this.currentAcceleration.copy(),
            jerk: this.lastJerk.copy()
        };
    }

    private smoothAxis(currentV: number, driverTarget: number, systemLimit: number, lastIntent: number, axisIndex: number, dt: number): number {
        const driverDecreased = Math.abs(driverTarget) < Math.abs(lastIntent) - 1e-4;

        if (driverDecreased) {
            if (axisIndex === 0) { this.currentAcceleration.x = 0; this.lastJerk.x = 0; }
            else if (axisIndex === 1) { this.currentAcceleration.y = 0; this.lastJerk.y = 0; }
            else { this.currentAcceleration.heading = 0; this.lastJerk.heading = 0; }
            return systemLimit;
        }

        const desiredAccel = (systemLimit - currentV) / dt;
        let accelError: number;
        if (axisIndex === 0) accelError = desiredAccel - this.currentAcceleration.x;
        else if (axisIndex === 1) accelError = desiredAccel - this.currentAcceleration.y;
        else accelError = desiredAccel - this.currentAcceleration.heading;

        const limitedJerkAccelChange = MathUtil.clamp(accelError, -SwerveConfig.MAX_JERK * dt, SwerveConfig.MAX_JERK * dt);
        let newAccel: number;

        if (axisIndex === 0) {
            this.lastJerk.x = limitedJerkAccelChange / dt;
            this.currentAcceleration.x += limitedJerkAccelChange;
            this.currentAcceleration.x = MathUtil.clamp(this.currentAcceleration.x, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.x;
        } else if (axisIndex === 1) {
            this.lastJerk.y = limitedJerkAccelChange / dt;
            this.currentAcceleration.y += limitedJerkAccelChange;
            this.currentAcceleration.y = MathUtil.clamp(this.currentAcceleration.y, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.y;
        } else {
            this.lastJerk.heading = limitedJerkAccelChange / dt;
            this.currentAcceleration.heading += limitedJerkAccelChange;
            this.currentAcceleration.heading = MathUtil.clamp(this.currentAcceleration.heading, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.heading;
        }

        return currentV + (newAccel * dt);
    }
}
