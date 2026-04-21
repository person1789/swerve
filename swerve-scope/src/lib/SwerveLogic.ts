// SwerveLogic.ts — TypeScript port of the Java swerve brain
// Mirrors SwerveConfig, SwerveKinematics, SwerveAuditor, MotionSmoother

export const SwerveConfig = {
  TRACK_WIDTH_IN: 9.921,
  WHEEL_BASE_IN:  9.927,

  MAX_SPEED_MPS: 1.35,
  MAX_ANGULAR_VELOCITY_RAD_S: 4.0,
  MAX_ACCEL: 3.0,
  MAX_JERK:  10.0,
  LOOP_TIME_SEC: 0.020,

  OBSERVER_LPF_GAIN: 0.15,
  TRANSLATION_LPF_GAIN: 0.20,
  ROTATION_LPF_GAIN: 0.25,

  DRIVE_KS: 1.05,
  DRIVE_KV: 4.2,
  DRIVE_KA: 0.45,

  STEER_P: 0.35,
  HEADING_P: 1.0,
  SNAP_P: 2.5,
  HEADING_THRESHOLD: 0.02,
};

// ────────────────────────────────────────────────────────────────────────────
export function normalizeAngle(a: number): number {
  while (a <= -Math.PI) a += 2 * Math.PI;
  while (a >   Math.PI) a -= 2 * Math.PI;
  return a;
}

export function angleError(current: number, target: number): number {
  return normalizeAngle(target - current);
}

export function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, v));
}

// ────────────────────────────────────────────────────────────────────────────
export class LowPassFilter {
  private last = 0;
  constructor(private alpha: number) {}
  calculate(x: number) {
    this.last = this.alpha * x + (1 - this.alpha) * this.last;
    return this.last;
  }
  reset(v = 0) { this.last = v; }
}

// ────────────────────────────────────────────────────────────────────────────
export interface ModuleState { speedMps: number; angleRad: number; }

export class SwerveKinematics {
  private hw: number;
  private hl: number;
  private offsets: [number, number][];

  constructor() {
    this.hw = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2;
    this.hl = (SwerveConfig.WHEEL_BASE_IN  * 0.0254) / 2;
    this.offsets = [
      [ this.hl,  this.hw],  // FL
      [ this.hl, -this.hw],  // FR
      [-this.hl, -this.hw],  // RR
      [-this.hl,  this.hw],  // RL
    ];
  }

  toModuleStates(vx: number, vy: number, omega: number, dt = SwerveConfig.LOOP_TIME_SEC): ModuleState[] {
    const a = omega * dt;
    let cx = vx, cy = vy;
    if (Math.abs(a) > 1e-6) {
      const s = Math.sin(a) / a;
      const c = (1 - Math.cos(a)) / a;
      cx = vx * s - vy * c;
      cy = vx * c + vy * s;
    }
    return this.offsets.map(([lx, ly]) => {
      const mvx = cx - omega * ly;
      const mvy = cy + omega * lx;
      return { speedMps: Math.hypot(mvx, mvy), angleRad: Math.atan2(mvy, mvx) };
    });
  }

  toChassisSpeeds(states: ModuleState[]): { vx: number; vy: number; omega: number } {
    let vx = 0, vy = 0, omega = 0;
    this.offsets.forEach(([lx, ly], i) => {
      const s = states[i].speedMps;
      const a = states[i].angleRad;
      const mvx = s * Math.cos(a);
      const mvy = s * Math.sin(a);
      vx += mvx; vy += mvy;
      omega += (lx * mvy - ly * mvx) / (lx * lx + ly * ly);
    });
    return { vx: vx / 4, vy: vy / 4, omega: omega / 4 };
  }
}

// ────────────────────────────────────────────────────────────────────────────
export class SwerveAuditor {
  static optimize(
    desired: ModuleState[],
    currentAngles: number[],
    maxSpeed: number,
  ): ModuleState[] {
    const out = desired.map((s, i) => {
      const err = angleError(currentAngles[i], s.angleRad);
      if (Math.abs(err) > Math.PI / 2) {
        return { speedMps: -s.speedMps, angleRad: normalizeAngle(s.angleRad + Math.PI) };
      }
      return { ...s };
    });
    // normalise speeds
    const mx = Math.max(...out.map(s => Math.abs(s.speedMps)));
    if (mx > maxSpeed && mx > 1e-9) {
      const sc = maxSpeed / mx;
      out.forEach(s => (s.speedMps *= sc));
    }
    return out;
  }
}

// ────────────────────────────────────────────────────────────────────────────
export interface Pose3 { vx: number; vy: number; heading: number; }

export class MotionSmoother {
  private vel:  Pose3 = { vx: 0, vy: 0, heading: 0 };
  private accel: Pose3 = { vx: 0, vy: 0, heading: 0 };
  private last:  Pose3 = { vx: 0, vy: 0, heading: 0 };

  calculate(driver: Pose3, limit: Pose3, dt: number): Pose3 {
    if (dt <= 0) return { ...this.vel };
    (['vx', 'vy', 'heading'] as const).forEach(k => {
      const driverDecreased = Math.abs(driver[k]) < Math.abs(this.last[k]) - 1e-4;
      if (driverDecreased) {
        this.accel[k] = 0;
        this.vel[k] = limit[k];
      } else {
        const da = clamp(
          (limit[k] - this.vel[k]) / dt - this.accel[k],
          -SwerveConfig.MAX_JERK * dt,
           SwerveConfig.MAX_JERK * dt,
        );
        this.accel[k] = clamp(this.accel[k] + da, -SwerveConfig.MAX_ACCEL, SwerveConfig.MAX_ACCEL);
        this.vel[k] += this.accel[k] * dt;
      }
    });
    this.last = { ...driver };
    return { ...this.vel };
  }

  getAccel(): Pose3 { return { ...this.accel }; }
  reset() {
    this.vel = { vx: 0, vy: 0, heading: 0 };
    this.accel = { vx: 0, vy: 0, heading: 0 };
    this.last = { vx: 0, vy: 0, heading: 0 };
  }
}

// ────────────────────────────────────────────────────────────────────────────
// Virtual module emulator — 1st-order lag on both speed and angle
export class ModuleEmulator {
  angleRad  = 0;
  speedMps  = 0;
  distM     = 0;
  currentA  = 0;

  update(target: ModuleState, dt: number) {
    // Steering: first-order with ~60ms time-constant
    const aErr = angleError(this.angleRad, target.angleRad);
    this.angleRad = normalizeAngle(this.angleRad + aErr * (dt / (dt + 0.06)));

    // Drive: first-order with ~40ms time-constant
    const dv = target.speedMps - this.speedMps;
    this.speedMps += dv * (dt / (dt + 0.04));
    this.distM += this.speedMps * dt;

    // Current model
    this.currentA = 1.2 + Math.abs(this.speedMps) * 2.5 + Math.abs(dv / dt) * 0.8;
  }

  getState(): ModuleState { return { speedMps: this.speedMps, angleRad: this.angleRad }; }
}

// ────────────────────────────────────────────────────────────────────────────
// Dead-reckoning odometry
export class Odometry {
  x = 0; y = 0; heading = 0;

  update(vel: { vx: number; vy: number; omega: number }, dt: number) {
    const dx = vel.vx * Math.cos(this.heading) - vel.vy * Math.sin(this.heading);
    const dy = vel.vx * Math.sin(this.heading) + vel.vy * Math.cos(this.heading);
    this.x += dx * dt;
    this.y += dy * dt;
    this.heading = normalizeAngle(this.heading + vel.omega * dt);
  }

  reset() { this.x = 0; this.y = 0; this.heading = 0; }
}

// ────────────────────────────────────────────────────────────────────────────
// Heading controller (snap + maintain)
export class HeadingController {
  private target = 0;
  private snapping = false;
  private maintaining = false;
  private integrator = 0;

  update(current: number, turn: number, isMoving: boolean, dt: number): number {
    if (Math.abs(turn) > 0.05) {
      this.snapping = false;
      this.maintaining = false;
      this.integrator = 0;
      return turn * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
    }

    if (this.snapping) {
      const err = angleError(current, this.target);
      if (Math.abs(err) < SwerveConfig.HEADING_THRESHOLD) this.snapping = false;
      return clamp(SwerveConfig.SNAP_P * err, -SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S, SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S);
    }

    if (isMoving) {
      if (!this.maintaining) { this.target = current; this.maintaining = true; this.integrator = 0; }
      const err = angleError(current, this.target);
      return clamp(SwerveConfig.HEADING_P * err, -1.5, 1.5);
    }

    this.maintaining = false;
    return 0;
  }

  snap(targetRad: number) { this.target = targetRad; this.snapping = true; this.maintaining = false; }
  reset(current: number) { this.target = current; this.snapping = false; this.maintaining = false; }

  getState() { return { snapping: this.snapping, maintaining: this.maintaining, target: this.target }; }
}
