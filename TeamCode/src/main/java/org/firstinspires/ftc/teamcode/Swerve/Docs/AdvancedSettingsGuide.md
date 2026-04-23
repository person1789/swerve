# Advanced Swerve Settings Guide

This guide documents the settings that matter for the current robot and how they fit together.

It is not a dump of every constant. It is the "what should I touch, and why?" guide.

## Current baseline assumptions

The current docs assume:

- REV hub orientation:
  - `HUB_LOGO_DIR = LEFT`
  - `HUB_USB_DIR = DOWN`
- Pinpoint odometry offsets:
  - `ODO_X_OFFSET_MM = -127.6669`
  - `ODO_Y_OFFSET_MM = -52.23`
- linear limits:
  - `MAX_LINEAR_SPEED_IN_S = 72.0`
  - `MAX_LINEAR_ACCEL_IN_S2 = 72.0`
  - `MAX_LINEAR_JERK_IN_S3 = 360.0`

If those assumptions change on the real robot, update `SwerveConfig.java` first and then treat old tuning results with suspicion.

## 1. Hardware truth settings

These should match the physical robot before you do any tuning:

- `OFFSETS`
- `INVERSIONS`
- `ODO_X_OFFSET_MM`
- `ODO_Y_OFFSET_MM`
- `HUB_LOGO_DIR`
- `HUB_USB_DIR`
- `WHEEL_RADIUS_METERS`
- `TRACK_WIDTH_IN`
- `WHEEL_BASE_IN`

If these are wrong, PID tuning usually turns into expensive lying.

## 2. Drive architecture

The drivetrain currently uses:

- open-loop drive power
- encoder-derived wheel speed feedback
- software-side motion shaping
- software-side kinematics and optimization

The wheel motors run in `RUN_WITHOUT_ENCODER` on purpose. That means:

- the REV hub is not closing the wheel velocity loop for you
- your Java pipeline owns drive behavior
- wheel encoder velocity is still available for observation and localization

That is the intended design right now.

## 3. Motion shaping

`MotionSmoother` is the main "feel" layer.

It handles:

- non-linear stick shaping
- acceleration limiting
- jerk limiting during ramp-up
- faster snap-down response when the driver brakes or reverses

Primary tunables:

- `MAX_LINEAR_ACCEL_IN_S2`
- `MAX_LINEAR_JERK_IN_S3`
- `INPUT_INTERCEPT`
- `INPUT_SPLINE_POINT`
- `INPUT_SLOPE`

When to touch them:

- If the robot feels too lazy off the line, look at accel and jerk.
- If the robot feels twitchy around stick center, look at the input curve values.
- If the robot feels unstable at high command changes, reduce aggressiveness before touching PID.

## 4. Heading behavior

`SwerveController` currently provides:

- manual turn passthrough
- delayed heading maintenance
- heading snap support in the controller layer

Primary tunables:

- `HEADING_P`, `HEADING_I`, `HEADING_D`
- `SNAP_P`, `SNAP_I`, `SNAP_D`
- `HEADING_LOCK_DELAY_S`

Important note:

Do not tune heading hold until:

- module steering behaves correctly
- odometry and hub orientation are correct
- forward and strafe both make sense in `SwerveSystemCheck`

## 5. Kinematics and observer

The drivetrain uses second-order kinematics plus an observed chassis velocity estimate.

Relevant settings:

- `TRACK_WIDTH_IN`
- `WHEEL_BASE_IN`
- `OBSERVER_LPF_GAIN`

If commanded motion and observed motion disagree systematically, fix geometry or wheel conversion before touching observer filtering.

## 6. What to change first

Use this order:

1. hardware names and directions
2. module offsets and inversion
3. hub orientation
4. odometry offsets
5. wheel radius and geometry
6. motion shaping
7. steering PID
8. heading PID

That order saves a lot of pain.

## 7. Things intentionally not in scope right now

The current design intentionally does not rely on:

- REV closed-loop drive velocity control under the swerve logic
- full voltage feedforward modeling with `kS`, `kV`, `kA`
- a layered stack of competing drive controllers

That keeps control authority in one place and makes debugging simpler.
