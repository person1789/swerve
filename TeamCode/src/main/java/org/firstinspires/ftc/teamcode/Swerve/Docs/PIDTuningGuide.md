# PID Tuning Guide

This guide defines how to tune the loops that matter in the current codebase and what counts as pass or fail for each one.

Right now, this repo is teleop-first. If you are not actively running autonomous, the most important tuning order is:

1. module steering PID
2. heading maintain PID
3. heading snap PID if you actually use it

Do not jump into pathing or autonomous follower gains until the drivetrain is already boringly correct in:

- `MainTeleOp`
- `SwerveSystemCheck`

Current default note:

- heading maintain is disabled by default
- heading snap is disabled by default

That is intentional. Tune steering and module calibration first, then enable and tune heading behavior.

Tune in this order:

1. Steering module PID
2. Heading maintain PID
3. Heading snap PID
4. Pedro translational PID if you return to autonomous
5. Pedro heading PID if you return to autonomous
6. Pedro drive PID if you return to autonomous

Do not tune a higher-level loop until the lower-level loop below it passes.

## 1. Steering Module PID

Source:

- `Swerve/Core/SwerveConfig.java`
- `STEER_P`, `STEER_I`, `STEER_D`

Used by:

- `Swerve/Hardware/SwerveModule.java`
- `Swerve/OpModes/SwerveModulePIDTune.java`

Purpose:

- Rotates an individual module to its commanded angle.

How to tune:

1. Put the robot on blocks so the wheels can steer freely.
2. Run the module tuning OpMode.
3. Start with `STEER_I = 0`.
4. Increase `STEER_P` until the module reaches target angle quickly.
5. If the module overshoots or chatters, increase `STEER_D`.
6. Only add `STEER_I` if the module consistently stops short under steady load.

Pass conditions:

- The module reaches a commanded angle quickly.
- The module settles without continuous oscillation.
- Reversing between two target angles does not produce long overshoot.
- The module holds angle without audible hunting.

Fail conditions:

- The module oscillates around the target.
- The module overshoots and rebounds repeatedly.
- The module takes too long to settle.
- The module only holds target with constant chatter.

## 2. Heading Maintain PID

Source:

- `Swerve/Core/SwerveConfig.java`
- `HEADING_P`, `HEADING_I`, `HEADING_D`
- `HEADING_LOCK_DELAY_S`

Used by:

- `Swerve/Logic/Control/SwerveController.java`

Purpose:

- Holds the robot heading after the driver stops commanding manual rotation.

How to tune:

1. Confirm steering PID already passes.
2. Confirm hub orientation and odometry are correct.
3. Drive the robot in straight translations with no turn input.
4. Start with `HEADING_I = 0`.
5. Lower `HEADING_P` if the robot weaves or corrects too aggressively.
6. Increase `HEADING_P` if the robot drifts without correcting enough.
7. Increase `HEADING_D` if corrections overshoot.
8. Adjust `HEADING_LOCK_DELAY_S` if heading hold engages too early or too late.

Pass conditions:

- The robot maintains heading during straight translation.
- Small disturbances are corrected smoothly.
- Heading hold does not fight the driver immediately after turn input is released.
- Corrections do not create visible side-to-side weaving.

Fail conditions:

- The robot snakes while translating.
- Heading drifts for too long before correcting.
- Corrections overshoot and reverse repeatedly.
- The heading hold engages so fast that it feels like turn input is being resisted.

## 3. Heading Snap PID

Source:

- `Swerve/Core/SwerveConfig.java`
- `SNAP_P`, `SNAP_I`, `SNAP_D`

Used by:

- `Swerve/Logic/Control/SwerveController.java`

Purpose:

- Rotates the robot to a deliberate target heading such as a cardinal direction.

How to tune:

1. Confirm heading maintain already passes.
2. Command repeated snap turns to known headings.
3. Start with `SNAP_I = 0`.
4. Raise `SNAP_P` until snap turns are decisive.
5. Raise `SNAP_D` if the robot overshoots and bounces back.
6. Add `SNAP_I` only if the robot consistently stops with repeatable steady-state error.

Pass conditions:

- Snap turns reach target heading promptly.
- The robot settles near the target without repeated rebound.
- Different starting headings produce similar final behavior.
- Small final heading errors disappear quickly.

Fail conditions:

- Snap turns feel weak or stall before target.
- The robot overshoots and rings.
- Final heading error remains after settling.
- Behavior changes drastically depending on starting angle.

## 4. Pedro Translational PID

If you are not running autonomous right now, skip this section.

Source:

- `pedroPathing/Constants.java`
- `translationalPIDFCoefficients`
- `secondaryTranslationalPIDFCoefficients`

Purpose:

- Corrects path-tracking error in X/Y translation while following paths.

## 5. Pedro Heading PID

If you are not running autonomous right now, skip this section.

Source:

- `pedroPathing/Constants.java`
- `headingPIDFCoefficients`
- `secondaryHeadingPIDFCoefficients`

Purpose:

- Controls robot orientation while following a path.

## 6. Pedro Drive PID

If you are not running autonomous right now, skip this section.

Source:

- `pedroPathing/Constants.java`
- `drivePIDFCoefficients`
- `secondaryDrivePIDFCoefficients`

Purpose:

- Shapes velocity-related drive response inside the follower.

## 7. Tuning Stop Rules

Stop tuning and fix the underlying system first if any of these are true:

- module zero offsets are not validated
- localization pose is inconsistent
- heading units are inconsistent
- path start pose is wrong
- alliance or goal coordinates are not trusted

PID tuning cannot compensate for bad geometry, bad units, or bad localization.
