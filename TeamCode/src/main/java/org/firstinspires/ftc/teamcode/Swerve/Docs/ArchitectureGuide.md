# Architecture Guide

This guide describes the current runtime path of the swerve stack as it exists in the code now.

It is meant to answer:

- what data enters the system
- which classes own which decisions
- where to debug a bad behavior
- what order the layers run in during teleop

## Current control path

For `MainTeleOp`, the high-level path is:

1. read driver sticks
2. reset heading if requested
3. update localization
4. convert driver request into chassis intent
5. shape that intent
6. convert chassis motion into module commands
7. apply those commands to the hardware

## Main runtime pieces

### `MainTeleOp`

Owns:

- gamepad input
- heading reset button handling
- field-centric translation transform
- calling the controller, localizer, and drivetrain in the loop

This is the top-level teleop entrypoint.

### `SwerveLocalizer`

Owns:

- Pinpoint position updates
- IMU heading fallback
- dead-reckoning fallback from observed chassis velocity

This is where to look when:

- heading reset feels wrong
- field-centric driving is rotated incorrectly
- observed robot motion and pose disagree badly

> Deprecated: this document describes a superseded architecture. The removed `SwerveController` and `MotionSmoother` layers are no longer part of the live runtime. The active teleop path is intentionally slimmer and centered on `OpMode -> SwerveDrivetrain -> SwerveModule`.
- brake/reverse snap-down behavior

This is the main "feel" layer. If the robot feels sluggish or too jumpy, this is the first place to inspect after hardware truth is verified.

### `SwerveKinematics`

Owns:

- chassis-to-module kinematics
- second-order discretization for combined translation and rotation

This is where to look when forward, strafe, and rotate commands produce the wrong wheel geometry.

### `SwerveAuditor`

Owns:

- shortest-path module steering optimization
- drive direction flipping
- speed desaturation

This is where to look when one module chooses a long steering move or when commanded wheel speeds are being clipped.

### `SwerveDrivetrain`

Owns:

- module construction
- smoother integration
- kinematics and auditing pipeline
- idle X-stance state machine
- hardware health scan

This is the main drivetrain coordinator.

### `SwerveModule`

Owns:

- individual pod steering PID
- drive power output
- steering power output
- encoder angle interpretation
- drive velocity conversion
- current and stall readout

This is where to look when a single pod is misbehaving even though the overall chassis command is correct.

## Current debug order

If the robot behaves badly, debug in this order:

1. hardware names and wiring direction
2. module offsets and inversion flags
3. hub orientation
4. odometry offsets
5. wheel radius and geometry
6. `SwerveSystemCheck`
7. motion shaping
8. steering PID
9. heading logic

That order matters because higher-level behavior often looks wrong when the lower-level truth is wrong.

## Which tool to use for which job

### Use `SwerveSystemCheck` when:

- you want canned forward/strafe/rotate commands
- you want live FTC Dashboard offset tuning
- you want CSV logs
- you want to verify one pod against the other three

### Use `MainTeleOp` when:

- you want to check actual driver feel
- you want to confirm field-centric behavior
- you want to validate heading reset in the real driving flow

### Use local JVM tests when:

- you are changing math
- you are changing controller logic
- you are changing smoothing or kinematics behavior

## Bottom line

The current stack is simple enough to debug if you keep the layer boundaries clear:

- teleop owns operator flow
- localizer owns pose
- controller owns heading decisions
- smoother owns motion shaping
- drivetrain owns command execution
- modules own pod behavior
