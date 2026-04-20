/**
 * SwerveLogic: A TypeScript port of the robot's Java swerve drivetrain logic.
 * This ensures the simulation behaves identically to the real hardware.
 */

export const SwerveConfig = {
    TRACK_WIDTH_IN: 9.921,
    WHEEL_BASE_IN: 9.927,
    MAX_SPEED_MPS: 1.35,
    MAX_ANGULAR_VELOCITY_RAD_S: 4.0,
    MAX_ACCEL: 3.0,
    MAX_JERK: 10.0,
    LOOP_TIME_SEC: 0.020,
    // Note: We don't need offsets and inversions in the sim as we start from 0
};

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

export class SwerveKinematics {
    private halfW: number;
    private halfL: number;
    private moduleOffsets: number[][];

    constructor() {
        this.halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2.0;
        this.halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254) / 2.0;
        this.moduleOffsets = [
            [this.halfL, this.halfW],   // FL
            [this.halfL, -this.halfW],  // FR
            [-this.halfL, -this.halfW], // RR
            [-this.halfL, this.halfW]   // RL
        ];
    }

    toModuleStates(vx: number, vy: number, omega: number, dt: number): SwerveModuleState[] {
        // Skew correction
        const halfAngle = (omega * dt) / 2.0;
        const cosH = Math.cos(halfAngle);
        const sinH = Math.sin(halfAngle);
        const vxCorr = vx * cosH - vy * sinH;
        const vyCorr = vx * sinH + vy * cosH;

        return this.moduleOffsets.map(([lx, ly]) => {
            const moduleVx = vxCorr - omega * ly;
            const moduleVy = vyCorr + omega * lx;
            return SwerveModuleState.fromVector(moduleVx, moduleVy);
        });
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

export class MotionSmoother {
    currentVelocity = new Pose(0, 0, 0);
    currentAcceleration = new Pose(0, 0, 0);
    lastDriverIntent = new Pose(0, 0, 0);

    calculate(driverTarget: Pose, systemLimit: Pose, dt: number): Pose {
        if (dt <= 0) return this.currentVelocity;

        this.currentVelocity.x = this.smoothAxis(this.currentVelocity.x, driverTarget.x, systemLimit.x, this.lastDriverIntent.x, 0, dt);
        this.currentVelocity.y = this.smoothAxis(this.currentVelocity.y, driverTarget.y, systemLimit.y, this.lastDriverIntent.y, 1, dt);
        this.currentVelocity.heading = this.smoothAxis(this.currentVelocity.heading, driverTarget.heading, systemLimit.heading, this.lastDriverIntent.heading, 2, dt);

        this.lastDriverIntent = driverTarget.copy();
        return this.currentVelocity.copy();
    }

    private smoothAxis(currentV: number, driverTarget: number, systemLimit: number, lastIntent: number, axisIndex: number, dt: number): number {
        const driverDecreased = Math.abs(driverTarget) < Math.abs(lastIntent) - 1e-4;

        if (driverDecreased) {
            if (axisIndex === 0) this.currentAcceleration.x = 0;
            else if (axisIndex === 1) this.currentAcceleration.y = 0;
            else this.currentAcceleration.heading = 0;
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
            this.currentAcceleration.x += limitedJerkAccelChange;
            this.currentAcceleration.x = MathUtil.clamp(this.currentAcceleration.x, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.x;
        } else if (axisIndex === 1) {
            this.currentAcceleration.y += limitedJerkAccelChange;
            this.currentAcceleration.y = MathUtil.clamp(this.currentAcceleration.y, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.y;
        } else {
            this.currentAcceleration.heading += limitedJerkAccelChange;
            this.currentAcceleration.heading = MathUtil.clamp(this.currentAcceleration.heading, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
            newAccel = this.currentAcceleration.heading;
        }

        return currentV + (newAccel * dt);
    }
}
