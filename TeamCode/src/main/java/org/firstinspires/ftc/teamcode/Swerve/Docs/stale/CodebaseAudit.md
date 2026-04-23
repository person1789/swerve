# Codebase Audit

This document now serves as a resolved-audit record for the current swerve and `pedroPathing` codebase.

It tracks:

- impossible or internally inconsistent conditions
- stale documentation or claims that no longer match implementation
- unimplemented or partially implemented features
- PID values greater than `0.5`
- teleop-path issues that can distort driver intent

The goal is to preserve what was found, what was fixed, and what still depends on on-robot validation.

## 1. Teleop Path Status

The active teleop path is:

- `MainTeleOp`
- `SwerveController`
- `MotionSmoother`
- `SwerveDrivetrain`
- `SwerveLocalizer`

The legacy `Swerve/Hardware/Pinpoint.java` helper is currently redundant for teleop.

Status:

- `MainTeleOp` does not construct or use `Pinpoint`
- `SwerveLocalizer` talks to the Pinpoint device directly through `HWMap`
- `Pinpoint.java` has been marked deprecated to reflect that redundancy

## 2. Recently Fixed In The Active Teleop Path

These issues were present in the live driver-control stack and have now been corrected:

### 2.1 Button-hold repeat behavior on teleop controls

Files:

- `Swerve/Core/Logger.java`
- `Swerve/OpModes/MainTeleOp.java`

Fix:

- logging-mode toggle now changes once per press
- heading reset now triggers once per press

### 2.2 Heading reset preserving position

File:

- `Swerve/Logic/Localization/SwerveLocalizer.java`

Fix:

- teleop heading reset now preserves current X/Y pose instead of resetting full localization state

### 2.3 Physical speed limits now applied to driver commands

Files:

- `Swerve/Input/MotionSmoother.java`

Fix:

- normalized driver commands are now converted into physical chassis targets using
  `MAX_LINEAR_SPEED_IN_S` and `MAX_ANGULAR_VELOCITY_RAD_S`

### 2.4 Module flipping no longer collapses speed to zero

File:

- `Swerve/Logic/Kinematics/SwerveAuditor.java`

Fix:

- module-state flipping now happens before cosine scaling so a 180-degree reversal becomes an inverted drive command instead of a near-zero wheel-speed command

### 2.5 Fallback localization no longer mixes meters-per-second with inches

File:

- `Swerve/Logic/Localization/SwerveLocalizer.java`

Fix:

- observer velocity is now converted before integrating into the inch-based pose state used by teleop

## 3. Remaining Driver-Intent And Performance Risks

### 3.1 Hot-path object allocation is still higher than it needs to be

Files:

- `Swerve/Input/MotionSmoother.java`
- `Swerve/Logic/Control/SwerveController.java`
- `Swerve/Logic/Kinematics/SwerveKinematics.java`
- `Swerve/Logic/Localization/SwerveLocalizer.java`
- `Swerve/Geometry/Vector.java`

Why this matters:

- The teleop loop creates many short-lived `Vector` objects each cycle.
- FTC runs on constrained Android hardware where unnecessary allocations can increase GC jitter.

Impact:

- Usually not catastrophic, but it can contribute to loop-time variance and make driver feel less consistent under telemetry-heavy conditions.

Recommended action:

- Keep the API clean, but reduce avoidable allocations in the hottest paths if loop timing becomes inconsistent on-robot.

### 3.2 Telemetry volume can still affect loop consistency during tuning

Files:

- `Swerve/Core/Logger.java`
- `Swerve/Hardware/SwerveDrivetrain.java`
- `Swerve/Hardware/SwerveModule.java`
- `Swerve/OpModes/MainTeleOp.java`

Why this matters:

- Four modules plus observer and loop timing are logged every cycle.
- Dashboard mirroring increases that cost further when enabled.

Impact:

- Heavy telemetry can reduce control-loop consistency and make the robot feel different between tuning and match configurations.

Recommended action:

- Keep dashboard disabled for match use and avoid production-level telemetry spam when chasing loop-time issues.

### 3.3 Auto-to-teleop pose handoff depends on matching coordinate conventions

Files:

- `Swerve/Core/PoseStorage.java`
- `Swerve/OpModes/MainTeleOp.java`
- `pedroPathing/Auto.java`

Why this matters:

- Teleop imports `PoseStorage.currentPose` directly into `SwerveLocalizer`.
- That handoff assumes Pedro pose X/Y and heading conventions match the inch-based teleop localizer conventions.

Impact:

- If those conventions drift, teleop can inherit a bad starting pose after autonomous.

Status:

- The handoff is now explicit through `PoseStorage.setFromPedroPose(...)`.
- Local unit tests verify that the stored pose preserves `x`, `y`, and heading values.

### 3.4 X-stance lock is intentional, but it overrides neutral free-roll after a short delay

Files:

- `Swerve/Hardware/SwerveDrivetrain.java`
- `Swerve/Core/SwerveConfig.java`

Why this matters:

- After `LOCK_DELAY_MS`, zero input transitions the drivetrain into an X-stance hold.
- That is good for resisting drift, but it is still an automatic behavior layered on top of driver neutral.

Impact:

- Drivers may perceive this as the robot “digging in” shortly after stick release.

Status:

- Idle X-stance is now configurable through `ENABLE_IDLE_X_STANCE`.
- The default is now the less intrusive driver-intent-preserving mode.

### 3.5 `Auto` declares more autonomous paths than it uses

File: `pedroPathing/Auto.java`

Status:

- The unused multi-cycle declarations were removed so the file now reflects that it is currently a preload-only autonomous.

## 4. Documentation And Architecture Mismatches

### 5.1 Documentation should continue to treat unit consistency as a goal, not a guarantee

Files:

- `Swerve/docs/README.md`
- `Swerve/docs/Architecture.md`

Why this needs nuance:

- The architecture intends to use a clean units model.
- The current `Pinpoint` helpers still contain inch/meter mixing, so the codebase is not yet fully unit-consistent.

Recommended action:

- Phrase unit consistency as a design goal, not as a guaranteed property.

## 5. PID Values Greater Than 0.5

This section is intentionally only a compiled list. A value greater than `0.5` is not automatically wrong, but it deserves deliberate justification and test evidence.

### 5.1 Internal swerve P gains greater than `0.5`

File: `Swerve/Core/SwerveConfig.java`

- `HEADING_P = 1.0`
- `SNAP_P = 2.0`

Audit note:

- These are the most aggressive internal closed-loop gains in the custom swerve stack.
- They should be validated on-robot with explicit pass/fail criteria before competition use.

### 5.2 Pedro heading P gains greater than `0.5`

File: `pedroPathing/Constants.java`

- `headingPIDFCoefficients(new PIDFCoefficients(1.2, 0, 0.005, 0))`
- `secondaryHeadingPIDFCoefficients(new PIDFCoefficients(0.63, 0, 0.035, 0))`

Audit note:

- These gains belong to a separate path-following stack and should not be evaluated by the same standard as module steering.
- They still warrant focused validation because they can produce oscillation or overshoot at path endpoints.

## 6. Items That Are Intentionally Out Of Scope, Not Bugs

These are important because they may look like missing features when they are actually deliberate design boundaries.

### 6.1 Voltage feedforward

Status:

- Intentionally out of scope for this FTC implementation.

Reason:

- The current drivetrain uses open-loop drive power plus encoder observation, not direct voltage control.

### 6.2 REV built-in drive velocity control

Status:

- Intentionally not used.

Reason:

- `RUN_WITHOUT_ENCODER` is being used so the custom Java logic remains the only drive controller.
- Encoder velocity is still read through `getVelocity()` for observation.

### 6.3 Particle filter localization

Status:

- Analyzed but not implemented.

Relevant file:

- `Swerve/docs/ParticleFilterAnalysis.md`

Reason:

- The document explicitly argues against deploying that approach on the FTC control stack.

## 7. Remaining Assumption

The only material assumption introduced without robot access is the field-frame mirroring used in `RobotSettings`.

Status:

- Goal poses and blue-side start poses are now internally consistent and unit-consistent.
- They still need on-field validation against your real alliance coordinate convention.

## 8. Tuning Order Recommendation

Do not tune the high-level heading loops first.

Recommended order:

1. Validate module angle offsets and steering PID.
2. Validate wheel-speed observation and localization sanity.
3. Validate heading maintain and heading snap.
4. Tune Pedro follower gains only after the underlying swerve stack is repeatable.
