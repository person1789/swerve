# Advanced Swerve Settings Guide

This guide documents the advanced control features that are actually in scope for this FTC swerve implementation.

## 1. Open-Loop Drive Power With Encoder Observation

The drivetrain drives the wheel motors in `RUN_WITHOUT_ENCODER` mode on purpose.

That means:

- motor output is open-loop power
- the REV hub is not running its built-in drive velocity loop
- the software still reads encoder-derived velocity through `DcMotorEx.getVelocity()` for the observer

That is the intended pairing for the current design:

- custom Java logic owns the drive behavior
- motor encoders still provide wheel speed feedback
- the observer can estimate chassis velocity from real wheel motion

## 2. Second-Order Kinematics

The kinematics layer discretizes chassis motion over the loop interval before resolving module states.

Result:

- better translation accuracy during simultaneous rotation
- less spin-strafe skew at high angular velocity

## 3. Motion Smoothing

`MotionSmoother` is the single authority for shaping requested chassis velocity.

The current implementation includes:

- non-linear input shaping
- acceleration limiting
- jerk limiting during ramp-up
- immediate snap-down when the driver brakes or reverses intent

Primary tunables:

- `MAX_ACCEL`
- `MAX_JERK`
- `INPUT_INTERCEPT`
- `INPUT_SPLINE_POINT`
- `INPUT_SLOPE`

## 4. Heading Hold And Snap

`SwerveController` provides:

- manual turn passthrough
- delayed heading maintenance while translating
- heading snap targets

Primary tunables:

- `HEADING_P`, `HEADING_I`, `HEADING_D`
- `SNAP_P`, `SNAP_I`, `SNAP_D`
- `HEADING_LOCK_DELAY_S`

## 5. Observer Filtering

The wheel-speed observer estimates chassis velocity from module states and then smooths that estimate with a low-pass blend.

Primary tuning value:

- `OBSERVER_LPF_GAIN`

Higher values react faster but pass more noise. Lower values are smoother but lag more.

## 6. Intentionally Out Of Scope

The following are intentionally not part of the current FTC implementation:

- voltage-compensated feedforward using `kS`, `kV`, and `kA`
- direct voltage targeting at the motor layer
- using `RUN_USING_ENCODER` as a second drive controller underneath the custom pipeline

That keeps control authority in one place and avoids stacking:

- custom swerve logic
- hub closed-loop velocity control
- a separate feedforward voltage model
