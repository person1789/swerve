@depreciatted
# FTC Swerve Drivetrain — Elite Implementation Proposal
## Executive Summary

This document provides a complete critical analysis and implementation roadmap for the current swerve drivetrain codebase. The existing system has a strong architectural vision — the layered Input → Brain → Pipeline → Hardware separation is correct — but contains critical bugs, mismatched abstractions, and missing implementations that would prevent it from performing at an elite level on a competition field.

The analysis is organized into four tiers: **Critical Blockers** (things that are broken or will cause match failures), **Performance Gaps** (things that compile and run but produce suboptimal behavior), **Architectural Improvements** (structural changes that make the system more maintainable and tunable), and **Missing Implementations** (things documented or referenced but not yet built).

---

## Tier 1 — Critical Blockers

These issues will cause immediate, observable failures during a match.

### 1.1 `SwerveModule` References Non-Existent Constants

**File:** `SwerveModule.java`

```java
// These constants do not exist anywhere in SwerveConfig.java
double wheelRps = (tps / SwerveConfig.DRIVE_TICKS_PER_REV) / SwerveConfig.DRIVE_GEAR_RATIO;
return wheelRps * (2 * Math.PI * SwerveConfig.WHEEL_RADIUS_METERS);

// Also missing from config:
getCurrentAmps() > SwerveConfig.DRIVE_CURRENT_THRESHOLD
```

`SwerveConfig.java` defines `MAX_SPEED_MPS`, `TRACK_WIDTH_IN`, and PID gains, but has no `DRIVE_TICKS_PER_REV`, `DRIVE_GEAR_RATIO`, `WHEEL_RADIUS_METERS`, or `DRIVE_CURRENT_THRESHOLD`. The module will not compile. Additionally, the `setMotorScaling()` method is called in `SwerveDrivetrain` but is not defined in `SwerveModule`.

**Fix — add to `SwerveConfig.java`:**

```java
// ─── Drive Motor Physical Constants ───────────────────────────────────────────
/** Encoder ticks per revolution of the drive motor shaft. */
public static double DRIVE_TICKS_PER_REV = 28; // GoBILDA 6000 RPM Yellow Jacket

/** Gear reduction between drive motor and wheel. */
public static double DRIVE_GEAR_REDUCTION = 7.43; //Reduction

/** Wheel radius in meters. Measure your actual wheel for accuracy. */
public static double WHEEL_RADIUS_METERS = 0.049; // 49mm diameter swerve wheel

/** Current draw threshold in amps above which a stall is declared. */
public static double DRIVE_CURRENT_THRESHOLD = 7.0;
```

**Fix — add to `SwerveModule.java`:**

```java
private double motorScaling = 1.0;

public void setMotorScaling(double scalar) {
    this.motorScaling = MathUtil.clamp(scalar, 0.0, 1.0);
}
```

Then multiply `drivePower` by `motorScaling` before the `setPower` call.

---

### 1.2 `SwerveVelocityObserver` Imports Wrong Package

**File:** `SwerveVelocityObserver.java`

```java
// WRONG — this package does not exist
import org.firstinspires.ftc.hardware.SwerveModule;

// CORRECT
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
```

This file will not compile. It is a copy-paste error in the import statement.

---

### 1.3 `HWMap` References `SwerveConfig` Before Its Own Package

**File:** `HWMap.java`

```java
// Missing import — SwerveConfig is in a different package
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
```

This import is missing. The file uses `SwerveConfig.HUB_LOGO_DIR` and `SwerveConfig.HUB_USB_DIR` for IMU initialization but doesn't import the class.

---

### 1.4 `SwerveKinematics` Has Two Incompatible Public APIs

**File:** `SwerveKinematics.java` and its callers

`SwerveKinematics` defines `inverseKinematics(Vector chassisSpeeds)` as its public method. However, the test files (`SwerveKinematicsTest`, `AdvancedSystemTest`, `SwerveSimServer`) all call `toModuleStates(vx, vy, omega)` and `toChassisSpeeds(states)`, which don't exist.

The production code (`SwerveDrivetrain`, `MainTeleOp`) correctly calls `inverseKinematics`. The test suite calls a completely different API. This means either the tests are wrong, or the production code is using the wrong method names.

**Fix — add aliases to `SwerveKinematics.java` for the test API:**

```java
/** Alias for inverseKinematics for test compatibility. */
public SwerveModuleState[] toModuleStates(double vx, double vy, double omega) {
    return inverseKinematics(new Vector(vx, vy, omega));
}

/** Alias for forwardKinematics returning a Pose-like object for test compatibility. */
public Pose toChassisSpeeds(SwerveModuleState[] states) {
    Vector v = forwardKinematics(states);
    return new Pose(v.x(), v.y(), v.omega());
}
```

Or, better — fix the test files to use the real API and delete the `Pose` class used only in tests (it duplicates `Vector`).

---

### 1.5 `MotionSmoother` API Mismatch

**File:** `MotionSmoother.java` vs callers

The production `MotionSmoother.smooth(Vector target, double dt)` takes a `Vector`. But every test file calls `smoother.calculate(Pose target, Pose systemLimit, double dt)` and `smoother.setLimits(accel, jerk)`. Neither signature exists.

The `Pose` class used in tests (`SwerveSimServer`, `MotionSmootherTest`, `SwerveStressTest`) is also referenced but not defined anywhere in the codebase. There are two classes named `Pose` (one in `Geometry/Pose.java` referenced only in tests, and `Vector` used in production). This dual-representation creates a translation gap between the test layer and the production layer.

**Fix:** Delete the test-only `Pose` class. Rewrite the test files to use `Vector` directly. Add `setLimits()` to `MotionSmoother` for testability:

```java
public void setLimits(double maxAccel, double maxJerk) {
    // Allow tests to override config values
    this.maxAccel = maxAccel;
    this.maxJerk = maxJerk;
}

private double maxAccel = SwerveConfig.MAX_ACCEL;
private double maxJerk = SwerveConfig.MAX_JERK;
```

---

### 1.6 PID Controller Error in `SwerveModule`

**File:** `SwerveModule.java`

```java
rotationController.setSetpoint(targetAngle);
double pidOut = rotationController.calculate(currentAngle, dt);
```

This is called with `setpoint = targetAngle` and `current = currentAngle`. The PID computes `error = setpoint - current = targetAngle - currentAngle`, which is correct for small angles. However, for angles near the ±π boundary (e.g., target = 3.1 rad, current = -3.1 rad), the error computes as 6.2 rad instead of the correct 0.08 rad. On a coaxial swerve module this causes violent full-rotation movements when crossing the ±π boundary.

**Fix — wrap the error in `angleError()` before passing to PID:**

```java
// Don't pass raw angles. Compute wrapped error, then control on that.
double error = MathUtil.angleError(currentAngle, targetAngle);

rotationController.setSetpoint(0.0); // Controller always drives error to zero
double pidOut = rotationController.calculate(-error, dt); // Negate: PID expects (current, target)
```

Alternatively, override PID `calculate` to accept a pre-computed error:

```java
public double calculateFromError(double error, double dt) {
    // Bypasses setpoint subtraction — use when error is already wrapped
}
```

---

## Tier 2 — Performance Gaps

These issues compile and run but produce behavior that falls short of the "best in the world" bar.

### 2.1 Field-Centric Rotation Is Wrong in MainTeleOp fixed

**File:** `MainTeleOp.java` and `SwerveTeleOp.java`

```java
// MainTeleOp.java line ~70
Vector rawTranslation = new Vector(vx, vy).rotate(-heading);
```

Rotating by `-heading` is correct for field-centric driving. However, the convention depends on how the heading is defined. If the Pinpoint reports heading as CCW-positive (standard FTC convention) and you want "forward on the joystick = field north," the rotation should be `-heading`. If the physical IMU is mounted such that CW rotation gives a positive heading, the sign needs to flip.

The bigger issue: both `MainTeleOp` and `SwerveTeleOp` exist with slightly different control logic. `MainTeleOp` has the full SwerveController integration; `SwerveTeleOp` duplicates D-pad snapping inline. One file should be deleted.

**Recommendation:** Delete `SwerveTeleOp.java`. Keep `MainTeleOp.java` as the canonical OpMode. Add a `@TeleOp` annotation with a descriptive name.

---

### 2.2 The `MotionSmoother` S-Curve Is Mathematically Incorrect

**File:** `MotionSmoother.java`

The current implementation attempts jerk-limited smoothing but has a critical flaw:

```java
double desiredAccel = (targetVal - currentVelocity.get(i)) / dt;
double accelError = desiredAccel - currentAcceleration.get(i);
double limitedJerkAccelChange = MathUtil.clamp(accelError, -maxJerk * dt, maxJerk * dt);
double newAccel = MathUtil.clamp(currentAcceleration.get(i) + limitedJerkAccelChange, -maxAccel, maxAccel);
nextVel[i] = currentVelocity.get(i) + (newAccel * dt);
```

This treats `desiredAccel` as a single-step target, but `desiredAccel = (target - current) / dt` produces extreme values (e.g., if `target = 1.0` and `current = 0`, at `dt = 0.02`, `desiredAccel = 50 m/s²`). The jerk clamp then only limits the *change* in acceleration, but the desired acceleration itself is not bounded before the clamp — meaning at the next step, `accelError` resets to a new extreme value. The filter converges but takes much longer than necessary and doesn't produce a true S-curve.

**Correct approach — proper jerk-limited trapezoidal profile:**

```java
public Vector smooth(Vector target, double dt) {
    if (dt <= 0) return currentVelocity;

    double[] vel = new double[3];

    for (int i = 0; i < 3; i++) {
        double v = currentVelocity.get(i);
        double a = currentAcceleration.get(i);
        double vTarget = target.get(i);
        double vLast = lastTarget.get(i);

        // Intelligent braking: driver-initiated deceleration is immediate
        boolean driverBraking = Math.signum(vTarget) != Math.signum(vLast)
                                 || Math.abs(vTarget) < Math.abs(vLast) - 1e-4;

        if (driverBraking) {
            // Snap velocity directly to target — crisp driver response
            vel[i] = vTarget;
            currentAcceleration.components[i] = (vTarget - v) / dt;
        } else {
            // System ramp-up: enforce jerk then acceleration limits
            double aTarget = MathUtil.clamp((vTarget - v) / dt, -maxAccel, maxAccel);
            double jerk = MathUtil.clamp((aTarget - a) / dt, -maxJerk, maxJerk);
            double newA = MathUtil.clamp(a + jerk * dt, -maxAccel, maxAccel);
            vel[i] = v + newA * dt;
            currentAcceleration.components[i] = newA;
        }
    }

    currentVelocity = new Vector(vel);
    lastTarget = target;
    return currentVelocity;
}
```

Note: this requires `Vector.components` to be mutable or `currentAcceleration` to be a `double[]`. Refactor accordingly.

---

### 2.3 Heading Retention Activates Too Aggressively fixed

**File:** `SwerveController.java`

```java
} else if (isMoving && !isTurning) {
    if (!isMaintaining) {
        targetHeading = currentHeading;
        isMaintaining = true;
        maintainPID.reset();
    }
    calculatedTurn = maintainPID.calculate(currentHeading, targetHeading, dt);
```

Heading retention activates the moment the driver pushes the stick with zero rotation input. On a competition field, any joystick push without rotation should lock the heading — but with a 20ms loop and a `maintainPID` that has no derivative filter, the correction output can be noisy on first activation (derivative kick from zero). This causes a brief jerk every time the driver starts moving.

**Fix — add a short delay before engaging heading retention:**

```java
private static final double HEADING_LOCK_DELAY_S = 0.1; // 100ms settling
private double headingLockTimer = 0;

// In update():
if (isMoving && !isTurning) {
    headingLockTimer += dt;
    if (headingLockTimer > HEADING_LOCK_DELAY_S) {
        if (!isMaintaining) {
            targetHeading = currentHeading;
            isMaintaining = true;
            maintainPID.reset();
        }
        calculatedTurn = maintainPID.calculate(currentHeading, targetHeading, dt);
    } else {
        calculatedTurn = 0;
    }
} else {
    headingLockTimer = 0;
    isMaintaining = false;
    calculatedTurn = dturn;
}
```

---

### 2.4 `SwerveAuditor` Speed Desaturation Is Applied Twice fixed 

**File:** `SwerveDrivetrain.java` + `SwerveAuditor.java`

`SwerveDrivetrain.setVelocity()` desaturates the chassis speeds before passing them to `driveWithPipeline()`. Then `SwerveAuditor.optimize()` desaturates the module states again after kinematics. Double desaturation means a command of `0.8 * MAX_SPEED` gets computed correctly by kinematics but then potentially scaled down again by the auditor, causing a systematic underspeed of ~10-20% at high commanded velocities.

**Fix:** Remove the scaling from `SwerveDrivetrain.setVelocity()`. Let `SwerveAuditor` be the single authority on speed desaturation. The auditor already does this correctly after kinematics, which is the right place.

```java
// SwerveDrivetrain.setVelocity() — remove this block entirely:
double maxFound = 0.0;
for (SwerveModuleState s : rawStates) maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond));
double scalingFactor = (maxFound > SwerveConfig.MAX_SPEED_MPS) ? SwerveConfig.MAX_SPEED_MPS / maxFound : 1.0;
Vector systemLimit = chassisSpeeds.scale(scalingFactor);

// And pass chassisSpeeds directly to smoother:
Vector smoothedVelocity = smoother.smooth(chassisSpeeds, dt);
```

---

### 2.5 Lock Delay State Machine Has a Race Condition

**File:** `SwerveDrivetrain.java`

```java
case DRIVING:
    driveWithPipeline(smoothedVelocity, dt);
    if (!hasInput) {
        lockTimer.reset();
        state = States.WAITING_TO_LOCK;
    }
    break;
```

The state machine transitions to `WAITING_TO_LOCK` and resets the timer, but also calls `driveWithPipeline(smoothedVelocity, dt)` first. The smoother will already be ramping toward zero, so on the first frame of `WAITING_TO_LOCK` the robot immediately receives `Vector(0,0,0)` — the smoother ramps normally. However, if the driver rapidly taps the stick (input → no input → input within 200ms), the state machine can be in `WAITING_TO_LOCK` and call `driveWithPipeline(new Vector(0,0,0), dt)` while the smoother still has residual velocity. This creates a brief "stutter" where the robot overrides the smoother's output with zero.

**Fix — don't zero the smoother output in WAITING_TO_LOCK; let the smoother drain naturally:**

```java
case WAITING_TO_LOCK:
    // Don't command zero — let the smoother drain to zero naturally.
    driveWithPipeline(smoothedVelocity, dt);
    if (hasInput) {
        state = States.DRIVING;
    } else if (lockTimer.milliseconds() > SwerveConfig.LOCK_DELAY_MS
               && smoothedVelocity.magnitude() < 0.01) {
        // Only lock when the robot has actually stopped
        state = States.LOCKED;
    }
    break;
```

---

### 2.6 Cosine Scaling Reduces Power When Modules Are Aligned Fixed 

**File:** `SwerveModule.java`

```java
double cosScaler = Math.abs(Math.cos(error));
double drivePower = (driveSpeedMps / SwerveConfig.MAX_SPEED_MPS) * cosScaler;
```

Cosine scaling is a well-known technique in swerve drives: when a module is perpendicular to its target, drive power goes to zero to avoid scrubbing. This is correct in principle. However, the current implementation computes `cosScaler` from `error` and then applies it to `drivePower` without simultaneously applying it to the target speed sent back to the auditor. This means the auditor optimizes for the full target speed while the module delivers a scaled-down speed — the velocity observer will then report chassis speeds that don't match the command, degrading localization quality.

**Recommendation:** Apply cosine scaling at the module state level before the auditor, or document that the velocity observer already compensates for this discrepancy. The cleanest solution is to apply cosine scaling in the auditor during optimize():

```java
// In SwerveAuditor.optimize():
double cosineScale = Math.cos(MathUtil.angleError(currentAnglesRad[i], state.angleRadians));
state.speedMetersPerSecond *= Math.max(0, cosineScale); // Only positive cosine
```

And remove it from `SwerveModule.update()`.

---

## Tier 3 — Architectural Improvements

These are not bugs but structural issues that will limit the system's tunability and testability over a season.

### 3.1 Consolidate the Dual Pose/Vector Representation

The codebase has three representations of 2D/3D pose:
- `Vector` (production code) — correct, general, but verbose
- `Pose` (test code and `SwerveSimServer`) — not defined in main source
- `Point` (legacy, in `Geometry/Point.java`) — never used by the swerve system

The `Pose` class referenced in tests has fields `x`, `y`, `heading` — which maps directly to a 3-component `Vector`. The `Point` class is a 2D vector with less functionality than `Vector`.

**Recommendation:**
1. Delete `Geometry/Point.java` — it is never imported by any swerve file
2. Define a proper `Pose` type as a thin wrapper around `Vector` if the field names are important for readability:

```java
public class Pose {
    public final double x, y, heading;
    
    public Pose(double x, double y, double heading) {
        this.x = x; this.y = y; this.heading = heading;
    }
    
    public Vector toVector() { return new Vector(x, y, heading); }
    
    public static Pose from(Vector v) {
        return new Pose(v.x(), v.y(), v.omega());
    }
    
    public Pose addPose(Pose other) {
        return new Pose(x + other.x, y + other.y, heading + other.heading);
    }
}
```

This eliminates the test/production mismatch without changing any production logic.

---

### 3.2 `SwerveConfig` Needs to Be Split

**File:** `SwerveConfig.java`

`SwerveConfig` is 110 lines and already getting unwieldy. As the season progresses and more constants are added, this file becomes the central point of confusion. In FTC, constants are tuned under time pressure at competitions — a single file with 40+ tunable values is a liability.

**Recommendation:** Split into domain-specific inner classes that can still be used with `@Config`:

```java
@Config
public class SwerveConfig {
    
    @Config
    public static class Geometry {
        public static double TRACK_WIDTH_IN = 9.921;
        public static double WHEEL_BASE_IN = 9.927;
        public static double WHEEL_RADIUS_METERS = 0.05;
        public static double DRIVE_GEAR_REDUCTION = 1.0;
        public static double DRIVE_TICKS_PER_REV = 537.7;
    }
    
    @Config
    public static class Limits {
        public static double MAX_SPEED_MPS = 1.35;
        public static double MAX_ANGULAR_VELOCITY_RAD_S = 4.0;
        public static double MAX_ACCEL = 3.0;
        public static double MAX_JERK = 10.0;
        public static double DRIVE_CURRENT_THRESHOLD = 9.0;
    }
    
    @Config
    public static class Steering {
        public static double P = 0.325;
        public static double I = 0.0;
        public static double D = 0.01;
        public static double[] OFFSETS = { -0.2, 3.9, 1.4, 3.0 };
        public static boolean[] INVERSIONS = { false, false, false, false };
    }
    
    @Config
    public static class Heading {
        public static double RETAIN_P = 1.0;
        public static double RETAIN_I = 0.0;
        public static double RETAIN_D = 0.05;
        public static double SNAP_P = 2.0;
        public static double SNAP_I = 0.0;
        public static double SNAP_D = 0.1;
        public static double LOCK_DELAY_MS = 200.0;
        public static double LOCK_ENGAGE_DELAY_S = 0.1;
    }
    
    @Config
    public static class Drive {
        public static double P = 0.1;
        public static double I = 0.0;
        public static double D = 0.0;
    }
    
    @Config
    public static class Input {
        public static double DEADBAND = 0.05;
        public static double INTERCEPT = 0.001;
        public static double SPLINE_POINT = 0.66;
        public static double SLOPE = 4.0;
    }
}
```

Update all references from `SwerveConfig.STEER_P` → `SwerveConfig.Steering.P`, etc.

---

### 3.3 `SwerveLocalizer` IMU Fallback Has a Logic Error

**File:** `SwerveLocalizer.java`

```java
try {
    heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
} catch (Exception e) {
    heading = masterPose.omega() + observedVelocity.omega() * dt;
}
```

Using `try/catch` for control flow based on sensor availability is incorrect in FTC. The IMU does not throw exceptions when it lacks a reading — it returns the last known value or 0. This fallback will never execute. Additionally, `masterPose.omega()` stores heading in position [2], but `Vector.omega()` returns `components[2]`, which is correct. However, the integration `masterPose.omega() + observedVelocity.omega() * dt` accumulates unbounded drift since it integrates a noisy velocity signal with no correction.

**Recommendation:** Check Pinpoint status before deciding data source. Expose an explicit fallback flag:

```java
public void update(Vector observedVelocity, double dt) {
    odo.update();
    boolean pinpointOk = odo.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY;
    
    double heading;
    double x, y;
    
    if (pinpointOk) {
        Pose2D pos = odo.getPosition();
        heading = odo.getHeading(AngleUnit.RADIANS);
        x = pos.getX(DistanceUnit.INCH);
        y = pos.getY(DistanceUnit.INCH);
    } else {
        // IMU gives heading only (no position without odometry)
        heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS)
                  + headingOffset; // Account for reset offset
        // Dead-reckon position from last known good pose
        Vector worldVel = new Vector(observedVelocity.x(), observedVelocity.y())
                          .rotate(masterPose.omega());
        x = masterPose.x() + worldVel.x() * dt;
        y = masterPose.y() + worldVel.y() * dt;
    }
    
    masterPose = new Vector(x, y, heading);
    pinpointPreviouslyHealthy = pinpointOk;
}
```

---

### 3.4 `PIDController` Derivative Term Will Kick on Setpoint Change

**File:** `PIDController.java`

```java
double rawDerivative = (error - lastError) / dt;
```

When the setpoint changes suddenly (e.g., `snapController.setSnapTarget(Math.PI/2)`), `error` jumps discontinuously. `rawDerivative` becomes `(newError - oldError) / dt`, which is enormous. This is the classic "derivative kick" problem. For steering modules this causes a violent jerk toward the new angle.

**Fix — use derivative on measurement (not error):**

```java
// Store last measurement, not last error
private double lastMeasurement = 0;

public double calculate(double current, double dt) {
    double error = setpoint - current;
    double pOut = Kp * error;
    
    integralSum = MathUtil.clamp(integralSum + error * dt, -maxIntegralSum, maxIntegralSum);
    double iOut = Ki * integralSum;
    
    // Derivative on measurement: eliminates kick when setpoint changes
    double rawDerivative = -(current - lastMeasurement) / dt;
    derivativeBuffer = derivativeFilter * derivativeBuffer + (1.0 - derivativeFilter) * rawDerivative;
    double dOut = Kd * derivativeBuffer;
    
    lastError = error;
    lastMeasurement = current;
    
    return pOut + iOut + dOut;
}
```

---

### 3.5 `Vector` Class Should Be Immutable

**File:** `Geometry/Vector.java`

`Vector` declares `components` as `public final double[]`, but `final` on an array only prevents reassignment of the reference — the array elements themselves are mutable. `SwerveVelocityObserver` relies on the observer velocity being stable between reads. If any code mutates `components[]` in-place, the observer value changes unexpectedly.

The current `MotionSmoother` tries to do `currentAcceleration.components[i] = newA`, which mutates the array. This is the correct fix for the smoother bug, but it highlights the risk.

**Recommendation:** Make Vector fully immutable. Change all mutation sites to construct new Vectors:

```java
// Defensive copy in constructor
public Vector(double... components) {
    this.components = components.clone(); // defensive copy
    this.dimension = components.length;
}
```

This costs one array allocation per construction, which is negligible at 50Hz but eliminates an entire class of aliasing bugs.

---

### 3.6 `Logger` Performance Impact

**File:** `Logger.java`

```java
public Logger(Telemetry telemetry) {
    this.telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
```

`FtcDashboard.getInstance()` involves a network round-trip to transmit telemetry over WiFi. At 50Hz with 20+ `addData` calls per loop, this is a significant CPU and bandwidth load. During autonomous or high-speed maneuvering, this has caused visible loop-time increases in other teams' codebases.

**Recommendation:**

1. Gate dashboard telemetry behind a flag:
```java
public static boolean DASHBOARD_ENABLED = false; // Enable only during tuning
```

2. Use `telemetry.setMsTransmissionInterval(100)` to throttle FTC Dashboard updates to 10Hz:
```java
this.telemetry.setMsTransmissionInterval(100);
```

3. During competition, set `DASHBOARD_ENABLED = false` to eliminate the WiFi overhead.

---

## Tier 4 — Missing Implementations

These are referenced in comments, documentation, or test files but not yet built.

### 4.1 `AdvancedSystemTest` References Non-Existent APIs

**File:** `AdvancedSystemTest.java`

```java
Pose result = kinematics.toChassisSpeeds(states); // Does not exist
smoother.calculate(target, systemLimit, dt);       // Does not exist
```

These tests are testing things that are documented in `AdvancedSettingsGuide.md` but not implemented. Until the production API is fixed (see Tier 1 blockers), none of these tests can run.

The feedforward math test validates a formula (`V = kS + kV*v + kA*a`) but the feedforward model itself is never applied to the motors — it exists only as a documentation concept. See Section 4.3.

---

### 4.2 Physics Feedforward Is Never Applied (overscope do not implement)

**File:** `SwerveModule.java`

`SwerveConfig` defines `DRIVE_KS`, `DRIVE_KV`, and `DRIVE_KA`, and `AdvancedSettingsGuide.md` documents the feedforward model. But `SwerveModule.update()` computes drive power purely as:

```java
double drivePower = (driveSpeedMps / SwerveConfig.MAX_SPEED_MPS) * cosScaler;
```

This is linear power scaling — not a feedforward model. The feedforward constants are never read.

**Complete implementation of voltage-compensated feedforward:**

```java
// Add VoltageSensor reference to SwerveModule constructor
private final VoltageSensor voltageSensor;

public void update(double targetAngle, double driveSpeedMps, double dt) {
    double currentAngle = getCurrentRotation();
    double error = MathUtil.angleError(currentAngle, targetAngle);
    double cosScale = Math.max(0, Math.cos(error));
    
    // Feedforward: compute target voltage
    double targetVoltage = 0;
    if (Math.abs(driveSpeedMps) > 1e-4) {
        double accel = (driveSpeedMps - lastTargetVelocityMps) / dt;
        targetVoltage = SwerveConfig.DRIVE_KS * Math.signum(driveSpeedMps)
                      + SwerveConfig.DRIVE_KV * driveSpeedMps
                      + SwerveConfig.DRIVE_KA * accel;
    }
    
    // Voltage compensation: normalize by actual battery voltage
    double batteryVoltage = voltageSensor.getVoltage();
    double ffPower = MathUtil.clampPower(targetVoltage / batteryVoltage);
    
    // Apply cosine scaling for scrub reduction
    double drivePower = ffPower * cosScale;
    
    // Steering PID (unchanged)
    rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
    double pidOut = rotationController.calculateFromError(error, dt);
    double steeringPower = Math.abs(error) < 0.02 ? 0.0 : MathUtil.clampPower(pidOut);
    
    lastTargetAngleRad = targetAngle;
    lastTargetVelocityMps = driveSpeedMps;
    lastDrivePower = drivePower;
    
    steerServo.setPower(steeringPower);
    driveMotor.setPower(MathUtil.clampPower(drivePower));
}
```

Pass `VoltageSensor` from `HWMap` down through `SwerveDrivetrain` to each `SwerveModule`.

---

### 4.3 `GeometryTest` References `Pose.addPose()` That Doesn't Exist (test removed)

**File:** `GeometryTest.java`

```java
Pose result = start.addPose(shift);
```

The `Pose` class referenced here is not defined in the main source. If adopting the `Pose` wrapper described in Tier 3, implement:

```java
public Pose addPose(Pose other) {
    return new Pose(this.x + other.x, this.y + other.y,
                    MathUtil.normalizeAngle(this.heading + other.heading));
}
```

Note: heading addition should wrap through `normalizeAngle` so two 180° additions don't give 360°.

---

### 4.4 Autonomous Integration Is Incomplete

**File:** `Auto.java`

The `Auto.java` OpMode uses Pedro Pathing but the swerve drive integration is through `Constants.createFollower()`. The Pedro Pathing `CoaxialPod` is configured correctly for each module, but:

1. `Constants.followerConstants` has `forwardZeroPowerAcceleration(-91.502)` and `lateralZeroPowerAcceleration(-91.502)` — both identical, which is unusual. Lateral deceleration is typically different from forward deceleration on a swerve. These should be measured independently on the actual robot.

2. `SwerveConfig.MAX_SPEED_MPS = 1.35 m/s`, but `Constants.driveConstants.velocity(52.95)` is in inches/second ≈ 1.345 m/s. These are consistent, which is good.

3. There is no mechanism to transfer the end-of-autonomous pose into the teleop localizer. `PoseStorage.currentPose` exists in `Core/PoseStorage.java` but is never read or written anywhere.

**Implement pose handoff:**

```java
// End of Auto.java runOpMode():
// Store final pose for TeleOp pickup
PoseStorage.currentPose = follower.getPose();

// Start of MainTeleOp.java runOpMode(), after localizer init:
if (PoseStorage.currentPose != null) {
    localizer.setPose(new Vector(
        PoseStorage.currentPose.getX(),
        PoseStorage.currentPose.getY(),
        PoseStorage.getHeading()
    ));
}
```

---

### 4.5 Module Calibration OpMode Has Low-Fidelity PID

**File:** `SwerveModulePIDTune.java`

```java
double currentPos = (FLE.getVoltage() / 3.3) * 360;
double error = AngleUnit.normalizeDegrees(targetAngle - currentPos);
double power = pidfController.calculate(0, error);
```

This reads the encoder voltage and converts to degrees, but it uses FTCLib's `PIDFController`, not the production `PIDController`. If you tune gains here, they don't directly transfer to the production system because the production system normalizes angles to radians and applies a derivative filter. The gains measured in this tuning OpMode will be in `degrees⁻¹` units, but the production code expects `radians⁻¹`.

**Fix:** Use the production `PIDController` in the tuning OpMode and work in radians:

```java
private PIDController steerPID = new PIDController(SwerveConfig.STEER_P, 
                                                     SwerveConfig.STEER_I, 
                                                     SwerveConfig.STEER_D);

// In loop:
double currentRad = (FLE.getVoltage() / 3.3) * 2.0 * Math.PI - SwerveConfig.STEERING.OFFSETS[0];
double targetRad = Math.toRadians(targetAngle);
steerPID.setSetpoint(targetRad);
double power = steerPID.calculate(currentRad, 0.025);
```

This guarantees that gains tuned in this OpMode are identical to gains used in production.

---

## Summary Prioritization

| Priority | Issue | File | Impact |
|---|---|---|---|
| P0 | Missing constants (`DRIVE_TICKS_PER_REV` etc.) | `SwerveConfig.java` | Won't compile |
| P0 | Wrong import in VelocityObserver | `SwerveVelocityObserver.java` | Won't compile |
| P0 | Missing `SwerveConfig` import in HWMap | `HWMap.java` | Won't compile |
| P0 | Dual API mismatch (Kinematics) | `SwerveKinematics.java` + tests | Tests fail |
| P0 | `MotionSmoother` API mismatch | `MotionSmoother.java` + tests | Tests fail |
| P1 | Angle wrapping in module PID | `SwerveModule.java` | Violent module rotation |
| P1 | Double desaturation | `SwerveDrivetrain.java` | Systematic speed deficit |
| P1 | S-curve math is incorrect | `MotionSmoother.java` | Poor acceleration feel |
| P1 | Derivative kick on setpoint change | `PIDController.java` | Steering jolts |
| P2 | Feedforward never applied | `SwerveModule.java` | Battery-voltage-dependent behavior |
| P2 | Lock delay race condition | `SwerveDrivetrain.java` | Occasional stutter |
| P2 | Heading lock engages instantly | `SwerveController.java` | Jerk on stick push |
| P2 | Pose storage not implemented | `PoseStorage.java` | Auto→TeleOp pose lost |
| P3 | `SwerveConfig` too monolithic | `SwerveConfig.java` | Tuning confusion |
| P3 | Duplicate `Pose`/`Vector` types | Multiple files | Test/production mismatch |
| P3 | Logger dashboard overhead | `Logger.java` | Loop time degradation |
| P3 | Tuning OpMode uses wrong PID | `SwerveModulePIDTune.java` | Gains don't transfer |

---

## Recommended Implementation Order
(Rest of the file...)
