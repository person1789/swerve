# Swerve Docs

This folder is the current documentation set for the robot as it exists now.

If you are trying to get the robot driving, tuning, or diagnosed on real hardware, start here rather than in the historical notes.

## Read These First

### For the first real robot run

- `TeleOpBringupGuide.md`
- `SystemCheckOpMode.md`

### For configuration and control assumptions

- `SwerveConfigGuide.md`
- `ArchitectureGuide.md`
- `PIDTuningGuide.md`
- `KookyAutoTuningGuide.md`

### For local development and tests

- `LocalTestingGuide.md`
- `UnitTestingGuide.md`

## Current robot-specific assumptions

These are the values the docs now assume unless a guide says otherwise:

- REV hub orientation:
  - logo facing `LEFT`
  - USB facing `DOWN`
- Pinpoint odometry offsets:
  - `ODO_X_OFFSET_MM = -127.6669`
  - `ODO_Y_OFFSET_MM = -52.23`
- linear motion limits:
  - `MAX_LINEAR_SPEED_IN_S = 72.0`
  - `MAX_LINEAR_ACCEL_IN_S2 = 72.0`
  - `MAX_LINEAR_JERK_IN_S3 = 360.0`

## Main workflows

### Teleop bring-up

Use `TeleOpBringupGuide.md` to:

- confirm hardware assumptions
- place the robot correctly
- do first drive tests
- understand what can still go wrong on the floor

### Drivetrain diagnostics

Use `SystemCheckOpMode.md` to:

- run canned forward, strafe, and rotate tests
- inspect FTC Dashboard telemetry
- log CSV output
- pull the CSV off a REV Control Hub over USB

### Configuration review

Use `SwerveConfigGuide.md` and `ArchitectureGuide.md` to:

- understand which values must match the robot
- understand the current control path
- know what to fix before touching PID

### Tuning

Use `PIDTuningGuide.md` and `KookyAutoTuningGuide.md` after the robot already behaves basically correctly in:

- `MainTeleOp`
- `SwerveSystemCheck`

Do not treat PID tuning as the first fix for bad offsets, bad orientation, or bad module direction.

## Analysis & Technical Records

Technical deep-dives and current implementation plans live in:

- `analysis/`

Historical records and superseded analysis documents are located in:

- `analysis/stale/` (Marked with `@depreciatted`)
