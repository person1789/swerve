# PID Tuning Guide

This guide defines how to tune each PID loop in the current codebase and what counts as pass or fail for each loop.

Tune in this order:

1. Steering module PID
2. Heading maintain PID
3. Heading snap PID
4. Pedro translational PID
5. Pedro heading PID
6. Pedro drive PID

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
2. Drive the robot in straight translations with no turn input.
3. Start with `HEADING_I = 0`.
4. Lower `HEADING_P` if the robot weaves or corrects too aggressively.
5. Increase `HEADING_P` if the robot drifts without correcting enough.
6. Increase `HEADING_D` if corrections overshoot.
7. Adjust `HEADING_LOCK_DELAY_S` if heading hold engages too early or too late.

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

Source:

- `pedroPathing/Constants.java`
- `translationalPIDFCoefficients`
- `secondaryTranslationalPIDFCoefficients`

Purpose:

- Corrects path-tracking error in X/Y translation while following paths.

How to tune:

1. Confirm the base swerve stack already drives straight and holds heading.
2. Run short simple paths first.
3. Tune the primary translational loop before the secondary loop.
4. Raise translational `P` until the robot converges on the path aggressively enough.
5. Raise translational `D` if it overshoots or wobbles around the path.
6. Add `I` only if there is repeatable residual position error.

Pass conditions:

- The robot converges to the path quickly.
- Cross-track error decreases without oscillation.
- Straight paths remain straight.
- Endpoints are reached without a long settling tail.

Fail conditions:

- The robot wanders beside the path.
- The robot crosses over the path repeatedly.
- Endpoint approach is unstable.
- Position error remains large after the path should be complete.

## 5. Pedro Heading PID

Source:

- `pedroPathing/Constants.java`
- `headingPIDFCoefficients`
- `secondaryHeadingPIDFCoefficients`

Purpose:

- Controls robot orientation while following a path.

How to tune:

1. Confirm translational path tracking already passes.
2. Use paths with clear heading targets.
3. Tune the primary heading loop first.
4. Reduce `P` if the robot oscillates near heading targets.
5. Increase `P` if the robot lags behind desired heading.
6. Increase `D` if the robot overshoots and rebounds.
7. Add `I` only if repeatable final heading error remains.

Pass conditions:

- The robot tracks commanded heading smoothly during motion.
- Final heading at path end is repeatable.
- Heading corrections do not destabilize translational tracking.
- Endpoint orientation settles cleanly.

Fail conditions:

- The robot swings past heading targets.
- Heading lags badly through curves.
- Final heading varies too much run to run.
- Heading correction causes visible path wobble.

## 6. Pedro Drive PID

Source:

- `pedroPathing/Constants.java`
- `drivePIDFCoefficients`
- `secondaryDrivePIDFCoefficients`

Purpose:

- Shapes velocity-related drive response inside the follower.

How to tune:

1. Tune this last.
2. Use repeatable paths with clear acceleration and deceleration phases.
3. Make small changes only.
4. Compare path timing, endpoint overshoot, and smoothness across runs.

Pass conditions:

- Path speed changes feel smooth and predictable.
- The robot does not surge at path start.
- The robot does not lunge through deceleration zones.
- Endpoint approach remains controlled.

Fail conditions:

- The robot surges on acceleration.
- The robot brakes too late or too abruptly.
- Path timing is inconsistent across identical runs.
- Small coefficient changes create unstable behavior.

## 7. High-Gain Review Rule

Any P gain greater than `0.5` should be treated as a review checkpoint, not an automatic failure.

Before accepting such a gain, confirm:

- the lower-level loop beneath it already passes
- the robot is stable over repeated runs
- the gain solves a real tracking issue rather than masking a geometry or localization problem

Current values above `0.5`:

- `SwerveConfig.HEADING_P = 1.0`
- `SwerveConfig.SNAP_P = 2.0`
- `Constants.headingPIDFCoefficients(... 1.2, ...)`
- `Constants.secondaryHeadingPIDFCoefficients(... 0.63, ...)`

## 8. Tuning Stop Rules

Stop tuning and fix the underlying system first if any of these are true:

- module zero offsets are not validated
- localization pose is inconsistent
- heading units are inconsistent
- path start pose is wrong
- alliance or goal coordinates are not trusted

PID tuning cannot compensate for bad geometry, bad units, or bad localization.
