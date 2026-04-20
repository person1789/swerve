# Triple-Derivative Swerve Controller — Implementation Plan

**Project:** FTC Swerve Drive (FTC SDK v11.0, DECODE 2025-2026)
**Architecture:** Jerk-limited S-curve motion profiling → Robot-relative IK → Auditor optimization → Observer feedback loop

---

## System Architecture Overview

```
[Gamepad Input]
      |
      v
[Phase 1: Input Pre-Processor / Smoother]
  - Jerk cap → Acceleration cap → Smoothed Vx, Vy, Vθ
      |
      v
[Phase 2: Robot-Relative Inverse Kinematics]
  - 4x Module vectors (speed + angle) from smoothed robot velocity
      |
      v
[Phase 3: Auditor]
  - Direction optimization (flip + reverse vs. long rotation)
  - Power normalization to [-1, 1]
      |
      v
[Phase 4: Motor + Servo Controls]
  - Drive motors: PID velocity control
  - Steer servos: PID angle control
      |
      v
[Phase 5: Observer — Odometry Feedback]
  - Pinpoint (Pose Pinpoint) + wheel encoders
  - Updates Estimated Pose → feeds error back to Phase 1
```

---

## Agent Execution Roadmap

This roadmap groups features into logical, sequential phases designed for an agent to implement and verify systematically.

### Phase A: Foundation & Math Utilities
- [ ] **Step 0: PID Controller** — Create custom `PIDController.java` utility (High Priority).
- [ ] **Math Utilities** — Implement angle normalization and power clamping in `MathUtil.java`.
- [ ] **Data Structures** — Scaffold `SwerveModuleState.java` for vector data passing.

### Phase B: Kinematics & Optimization Engine
- [ ] **Inverse Kinematics** — Implement `SwerveKinematics.java` for robot-to-module translation.
- [ ] **The Auditor** — Implement `SwerveAuditor.java` for flip-optimization and power normalization.

### Phase C: Hardware Interface & Core Drivetrain
- [ ] **Hardware Mapping** — Update `HWMap.java` and `swerve.xml` for Axon Max steering and GoBILDA drive.
- [ ] **Swerve Module** — Implement `SwerveModule.java` combining PID, motors, and CRServos.
- [ ] **Drivetrain Subsystem** — Create `SwerveDrive.java` to coordinate all four modules.

### Phase D: Driver Control & Smoothing
- [ ] **Basic TeleOp** — Implement `SwerveTeleOp.java` for initial field testing.
- [ ] **Motion Profiling** — Implement `MotionSmoother.java` for Jerk-limited S-curve inputs.

### Phase E: Advanced Localization & Sophistications
- [ ] **The Observer** — Implement `PoseEstimator.java` using GoBILDA Pinpoint odometry.
- [ ] **Advanced Features** — Add `kS/kV/kA` Feedforward, X-Stance Defense, and Heading Snap.

---

## Phase 1 — Input Pre-Processor (The Smoother)

**Goal:** Convert raw joystick step-inputs into jerk-limited, S-curve velocity commands.

### Constants to define
```java
double MAX_VELOCITY      = /* m/s, measured from robot */;
double MAX_ACCELERATION  = /* m/s², tune after mechanical testing */;
double MAX_JERK          = /* m/s³, tune for S-curve smoothness */;
double LOOP_TIME_SECONDS = 0.020; // 20 ms control loop target
```

### Tasks

1. **Read raw joystick axes**
   - `gamepad1.left_stick_x` → raw `desiredVx`
   - `gamepad1.left_stick_y` → raw `desiredVy`
   - `gamepad1.right_stick_x` → raw `desiredVtheta`
   - Apply a deadband (e.g., ±0.05) to eliminate stick drift

2. **Scale to robot-max units**
   ```
   desiredVx     *= MAX_VELOCITY
   desiredVy     *= MAX_VELOCITY
   desiredVtheta *= MAX_ANGULAR_VELOCITY
   ```

3. **Jerk limiter (outer cap)**
   ```
   deltaAccel = (desiredAccel - currentAccel) / dt
   if |deltaAccel| > MAX_JERK:
       clamp deltaAccel to ±MAX_JERK * dt
   currentAccel += deltaAccel
   ```

4. **Acceleration limiter (inner cap)**
   ```
   deltaVelocity = currentAccel * dt
   if |currentVelocity + deltaVelocity - desiredVelocity| > MAX_ACCELERATION * dt:
       clamp accordingly
   currentVelocity += deltaVelocity
   ```

5. **Output:** smoothed `cmdVx`, `cmdVy`, `cmdVtheta` — these feed Phase 2.

---

## Phase 2 — Robot-Relative Inverse Kinematics

**Goal:** Translate `(cmdVx, cmdVy, cmdVtheta)` into 4 module `(speed, angle)` vectors.

### Robot geometry constants
```java
// Half-width and half-length of the wheel base (meters)
double L = TRACK_LENGTH / 12.0;  // front-back half-distance
double W = TRACK_WIDTH  / 12.0;  // left-right half-distance

// Module positions relative to robot center (FL, FR, RL, RR)
double[][] moduleOffsets = {
    { L,  W},   // Front-Left
    { L, -W},   // Front-Right
    {-L,  W},   // Rear-Left
    {-L, -W}    // Rear-Right
};
```

### Standard swerve IK equations

For each module `i` with offset `(lx, ly)`:

```
moduleVx[i] = cmdVx - cmdVtheta * ly
moduleVy[i] = cmdVy + cmdVtheta * lx

speed[i] = sqrt(moduleVx[i]^2 + moduleVy[i]^2)
angle[i] = atan2(moduleVy[i], moduleVx[i])
```

### Second-order kinematics correction (skew prevention)

At high speed + rotation, the module angle changes while the wheel is moving, causing heading skew. Apply correction:

```
// Previous module angle (from encoder feedback)
double prevAngle = moduleStates[i].angle;

// Predicted angle change this loop
double angularDelta = cmdVtheta * LOOP_TIME_SECONDS;

// Correct commanded angle
angle[i] -= angularDelta / 2.0;  // first-order correction; tune coefficient
```

### Tasks

1. Implement `SwerveKinematics.java` class with `toModuleStates(ChassisSpeeds speeds)` method
2. Return array of `SwerveModuleState { double speedMps; double angleRad; }`

---

## Phase 3 — The Auditor

**Goal:** Optimize module commands before sending to hardware — minimize mechanical wear and transition time.

### Direction optimization (flip optimization)

Rather than rotating a module up to 270° to reach the target heading, flip the motor direction and rotate ≤90° instead.

```java
double angleDelta = targetAngle - currentAngle;

// Normalize to (-π, π)
while (angleDelta >  Math.PI) angleDelta -= 2 * Math.PI;
while (angleDelta < -Math.PI) angleDelta += 2 * Math.PI;

if (Math.abs(angleDelta) > Math.PI / 2) {
    angleDelta -= Math.signum(angleDelta) * Math.PI;
    speed *= -1;  // reverse drive direction
}

commandedAngle = currentAngle + angleDelta;
```

### Power normalization

After IK, one or more modules may exceed `[−1, 1]`. Scale all modules proportionally:

```java
double maxSpeed = Arrays.stream(speeds).max().getAsDouble();
if (maxSpeed > 1.0) {
    for (int i = 0; i < 4; i++) speeds[i] /= maxSpeed;
}
```

### Tasks

1. Implement `SwerveAuditor.java` with `optimize(SwerveModuleState[] states, double[] currentAngles)` method
2. Apply logic to limit rotation scope and proportionalize velocities.

---

## Phase 4 — Motor and Servo Controls

**Goal:** Lock each module onto its commanded velocity and angle using PID.

### Drive motor — velocity PID

```java
// Per drive motor
double velocityError = commandedSpeedEncoderTicksPerSec - motor.getVelocity();
double output = kP * velocityError
              + kI * integralError
              + kD * (velocityError - prevError) / dt;
motor.setPower(output);
```

### Steer servo/motor — angle PID

```java
double angleError = commandedAngle - currentAngle;
// Normalize angleError to (-π, π) same as Auditor
double output = kP_steer * angleError + kD_steer * (angleError - prevAngleError) / dt;
steerMotor.setPower(output);
```

### Hardware configuration (`configuration_name.xml`)
```
front_left_drive   → DcMotorEx
front_right_drive  → DcMotorEx
rear_left_drive    → DcMotorEx
rear_right_drive   → DcMotorEx
front_left_steer   → CRServoEx
front_right_steer  → CRServoEx
rear_left_steer    → CRServoEx
rear_right_steer   → CRServoEx
```

### Tasks

1. **[HIGH PRIORITY]** Create a custom `PIDController.java` utility class. Note: Since FTC does not support native voltage/acceleration control, this PID must be tuned to translate errors directly into normalized `[-1, 1]` power values.
2. Create modular `SwerveModule.java` integrating **GoBILDA Drive Motors** and **Axon Max Steering Servos**.
3. Link PID calculations to the hardware mapped objects, ensuring `Servo` position commands or `CRServo` power commands are handled according to Axon Max configuration.
4. Implement a "Power Normalizer" that ensures the sum of PID + Feedforward never exceeds the `[-1, 1]` hardware limit.

---

## Phase 5 — Observer (Odometry Feedback)

### [FTC Hardware Alert]
> [!IMPORTANT]
> **FTC Control Loop Limitations**
> Unlike FRC, we cannot natively command acceleration or complex profiles. Phase 5 must output raw velocity targets which are then "processed" by the Triple-Derivative Smoother in Phase 1 to stay within hardware limits using only `.setPower()` or `.setVelocity()`.

### Pose update loop (runs every control cycle)

```java
// 1. Calculate Pose Errors
double xError     = targetPose.x     - currentPose.x;
double yError     = targetPose.y     - currentPose.y;
double thetaError = targetPose.theta - currentPose.theta;

// Normalize thetaError to (-π, π)
while (thetaError >  Math.PI) thetaError -= 2 * Math.PI;
while (thetaError < -Math.PI) thetaError += 2 * Math.PI;

// 2. Full PID Control (All 3 Terms) on Observer Feedback
// Positional error dictates the base velocity we want the robot to move at.
xIntegral += xError * dt;
double rawVx = (kP_x * xError) + (kI_x * xIntegral) + (kD_x * (xError - prevXError) / dt);

yIntegral += yError * dt;
double rawVy = (kP_y * yError) + (kI_y * yIntegral) + (kD_y * (yError - prevYError) / dt);

thetaIntegral += thetaError * dt;
double rawVtheta = (kP_t * thetaError) + (kI_t * thetaIntegral) + (kD_t * (thetaError - prevThetaError) / dt);

prevXError = xError;
prevYError = yError;
prevThetaError = thetaError;

// 3. Feed the PID-adjusted raw velocities into Phase 1 (Jerk/Accel Smoother)
// This strictly caps how fast the robot is allowed to correct its pose error
```

### Re-localization trigger

If the observer detects a jump > threshold (e.g. wheel slip event), trigger a re-localize:

```java
if (poseJumpMagnitude > RELOCALIZE_THRESHOLD_M) {
    pinpoint.resetPosAndIMU();
    estimatedPose = pinpoint.getPosition();
}
```

### Tasks

1. Call `pinpoint.update()` at the top of every `loop()` cycle
2. Implement the `kP`, `kI`, `kD` full PID loop utilizing `xError`, `yError`, `thetaError`
3. Feed the resulting observer `rawVx`, `rawVy`, `rawVtheta` into the Phase 1 Triple-Derivative smoother
4. Implement relocalization trigger with configurable threshold

---

## Phase 6 — Advanced Sophistications (FRC 254 Inspired)

**Goal:** Implement elite-level Swerve optimizations tuned for FTC hardware constraints.

### 1. Drive Motor Physics Feedforward (kS, kV, kA)
> [!NOTE]
> In FTC, these coefficients map directly to **Power Units (0.0 to 1.0)**.
- `kS` (Static Friction): Minimum power fraction to break wheel traction.
- `kV` (Velocity): Power fraction per unit of velocity.
- `kA` (Acceleration): Power fraction per unit of acceleration.
- *Task:* Implement `(kS * sign) + (kV * vel) + (kA * accel)` and sum with PID before calling `motor.setPower()`.

### 2. True X-Stance Defense Lock
- *Task:* Override the locked state so wheels turn 45 degrees inwards creating an "X" pattern.
- *Result:* Axon Max servos holding position in this state makes the robot nearly impossible to push.

### 3. Rotational-Priority Desaturation
- *Task:* Update the Auditor to scale down Translation first if total power exceeds 1.0. This ensures the robot always maintains its intended heading (theta) even if it can't reach the full translation speed.

### 4. Driver Aids: Heading Snap
- *Task:* Map buttons to "snap" the **target Heading (theta)** to exactly 0, 90, 180, or 270 degrees. The PID controller will then automatically handle the rotational correction.

---

---

## Full Control Loop — Execution Order

Each `loop()` call in your `TeleOp` OpMode must execute in this order:

```
1.  pinpoint.update()                        // Sense — refresh pose
2.  estimatedPose = pinpoint.getPosition()   // Sense — read pose
3.  rawVx, rawVy, rawVt = readGamepad()      // Input & Cubing
4.  cmdV = smoother.update(rawV, dt)         // Phase 1 — smooth (Jerk/Accel)
5.  moduleStates = kinematics.toModuleStates(cmdV)  // Phase 2 — IK
6.  moduleStates = auditor.optimize(moduleStates)   // Phase 3 — audit
7.  for each module: applyPID_and_Feedforward(moduleStates)  // Phase 4/6
8.  telemetry.update()                       // Debug output
```

---

## File Structure (TeamCode)

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── swerve/
│   ├── SwerveModule.java          // Single module: drive + steer PID
│   ├── SwerveDrive.java           // Coordinates all 4 modules
│   ├── SwerveKinematics.java      // Phase 2 IK math
│   ├── SwerveAuditor.java         // Phase 3 optimization
│   ├── MotionSmoother.java        // Phase 1 jerk/accel limiter
│   └── SwerveModuleState.java     // Data class: speed + angle
├── odometry/
│   └── PoseEstimator.java         // Phase 5 Observer
├── opmode/
│   ├── SwerveTeleOp.java          // Main driver-controlled OpMode
│   └── SwerveCalibration.java     // Standalone calibration OpMode
└── util/
    └── MathUtil.java              // Angle normalization, clamp, etc.
```

---

## Execution Plan (Programming Workflow)

To efficiently build the system in code with minimal risks, implement the features following this order:

**Step 1: Module-Level Programming (Phase 4)**
- Scaffold `SwerveModule.java` integrating **Axon Max** servos and **GoBILDA** motors.
- Set up class structures to receive input for Steering (Angle PID/Position) and Driving (Velocity PID) utilizing the custom PID class.

**Step 2: Inverse Kinematics (Phase 2)**
- Scaffold `SwerveKinematics.java`.
- Write the logic translating `(cmdVx, cmdVy, cmdVtheta)` to 4 `SwerveModuleState` vectors.

**Step 3: The Auditor (Phase 3)**
- Scaffold `SwerveAuditor.java`.
- Implement motor flip optimization and speed normalization mathematically.

**Step 4: Drivetrain Integration (Basic TeleOp)**
- Scaffold `SwerveDrive.java` acting as the main Drivetrain subsystem bringing together the modules, auditor, and IK logic.
- Create a basic `SwerveTeleOp.java` feeding basic (unsmoothed) Gamepad input.

**Step 5: Odometry and Advanced Features (Phases 1 & 5)**
- Add `PoseEstimator.java` to extract localization data.
- Integrate the GoBilda Pinpoint. 
- Make the drive field-centric by applying the IMU/Odometry heading mathematically to the joystick inputs.
- Implement the `MotionSmoother.java` and apply jerk-limiter calculations.

---

## Hardware Configuration & Map

Make sure physical elements represent code counterparts properly.

### Hardware Mapping Details (`configuration_name.xml`)
```
front_left_drive   → DcMotorEx (GoBILDA)
front_right_drive  → DcMotorEx (GoBILDA)
rear_left_drive    → DcMotorEx (GoBILDA)
rear_right_drive   → DcMotorEx (GoBILDA)

front_left_steer   → CRServoEx / Servo (Axon Max)
front_right_steer  → CRServoEx / Servo (Axon Max)
rear_left_steer    → CRServoEx / Servo (Axon Max)
rear_right_steer   → CRServoEx / Servo (Axon Max)

pinpoint           → I2cDevice (GoBILDA Pinpoint)
```

### Sensor Integrations

| Sensor | Role | FTC SDK Class |
|---|---|---|
| Pose Pinpoint (GoBilda) | Primary odometry — dead-wheel pose | `GoBildaPinpointDriver` |
| Drive encoders | Secondary velocity feedback for PID | `DcMotorEx.getVelocity()` |
| IMU (built-in) | Heading redundancy / sanity check | `IMU` |

### Hardware Tasks
1. Configure all 8 motors in the Robot Controller hardware config
2. Verify motor directions (positive power = forward/CCW) for each module
3. Add `GoBildaPinpointDriver` to hardware map and initialize it correctly in code hardware setup

---

## Testing & Tuning Sequence

### Phase 1 Tuning Notes (The Smoother)
- Start with `MAX_JERK` very high (effectively disabled) and tune `MAX_ACCELERATION` first.
- Re-introduce jerk limiting once acceleration is stable.
- Run robot in 8 cardinal directions + both rotation directions to validate no wheel slip occurs during ramp-up.

### Phase 2 Test (Inverse Kinematics)
- Print the IK outputs to telemetry without enabling power to motors.
- Unit test: feed `(1, 0, 0)` → all wheels should point forward at equal speed
- Unit test: feed `(0, 0, 1)` → all wheels should point tangentially for pure rotation
- Unit test: feed `(1, 0, 1)` → verify no two modules return identical angles (combined motion test)

### Phase 3 Test (The Auditor)
- Validate logs for `angleDelta` pre/post optimization logic.
- Verify flip optimization: mock command 200° → module should specify to target 20° with reversed motor speed value
- Verify normalization: if one module calculates to 1.4, ensure all four properly scale down dynamically by 1/1.4

### Phase 4 Test (Motor Control / PID)
- Tune `kP`, `kI`, `kD` for drive velocity — start with `kP=0.01`, `kI=0`, `kD=0`
- Tune `kP_steer`, `kD_steer` — aim for < 5° steady-state error at full speed
- Run a standalone opmode to set specific target angles and speeds to single modules. Validate that they reach those targets quickly without oscillating. Validate command 90° module heading → physical module reads 90° ± 1°
- Verify all 8 directions on blocks before dropping it to the floor.

### Phase 5 Full System Calibration Sequence
Run these in order before competition on the main TeleOp configuration:

| Step | Test | Pass Criterion |
|---|---|---|
| 1 | Push robot 1 m forward by hand | Pose reads (1.0, 0, 0) ± 1 cm |
| 2 | Rotate robot 90° by hand | Pose reads (0, 0, 90°) ± 1° |
| 3 | Drive forward at 50% for 2 s | Straight line, no yaw drift |
| 4 | Slam stick from 0 → 100% | No wheel squeal, S-curve visible in velocity telemetry |
| 5 | Full-speed rotation | All 4 modules within ± 2° of target heading |
| 6 | 8-direction drive test | No heading errors accumulate over 10 s |
| 7 | Combined translation + rotation | Observer pose remains accurate, no relocalize triggers |

---

## Key References

- FTC SDK Javadoc: https://javadoc.io/doc/org.firstinspires.ftc
- FTC Docs: https://ftc-docs.firstinspires.org
- GoBilda Pinpoint driver: available in SDK v11.0 via `GoBildaPinpointDriver`
- FTC Community (technical questions): https://ftc-community.firstinspires.org