# Pedro Follower Tuning Guide

This guide covers the Pedro autonomous tuning values that are now exposed through:

- `Swerve/Core/SwerveConfig.java`

They are FTC Dashboard tuneable because:

- `SwerveConfig` is `@Config`
- the active Pedro autos now call `PedroSwerveFactory.applyLiveTuning(follower)` during the run loop

That means you can change these values in FTC Dashboard while the robot is running an auto and the follower will pick up the new values without redeploying.

## What is tuneable now

These Pedro follower settings are now surfaced in `SwerveConfig`.

### Path correction

- `PEDRO_TRANSLATIONAL_P`
- `PEDRO_TRANSLATIONAL_I`
- `PEDRO_TRANSLATIONAL_D`
- `PEDRO_TRANSLATIONAL_F`
- `PEDRO_TRANSLATIONAL_PID_SWITCH`
- `PEDRO_USE_TRANSLATIONAL_PID`

- `PEDRO_SECONDARY_TRANSLATIONAL_P`
- `PEDRO_SECONDARY_TRANSLATIONAL_I`
- `PEDRO_SECONDARY_TRANSLATIONAL_D`
- `PEDRO_SECONDARY_TRANSLATIONAL_F`
- `PEDRO_USE_SECONDARY_TRANSLATIONAL_PID`

### Heading correction

- `PEDRO_HEADING_P`
- `PEDRO_HEADING_I`
- `PEDRO_HEADING_D`
- `PEDRO_HEADING_F`
- `PEDRO_HEADING_PID_SWITCH_RAD`
- `PEDRO_USE_HEADING_PID`

- `PEDRO_SECONDARY_HEADING_P`
- `PEDRO_SECONDARY_HEADING_I`
- `PEDRO_SECONDARY_HEADING_D`
- `PEDRO_SECONDARY_HEADING_F`
- `PEDRO_USE_SECONDARY_HEADING_PID`

### Drive correction

- `PEDRO_DRIVE_P`
- `PEDRO_DRIVE_I`
- `PEDRO_DRIVE_D`
- `PEDRO_DRIVE_F`
- `PEDRO_DRIVE_T`
- `PEDRO_DRIVE_PID_SWITCH`
- `PEDRO_USE_DRIVE_PID`

- `PEDRO_SECONDARY_DRIVE_P`
- `PEDRO_SECONDARY_DRIVE_I`
- `PEDRO_SECONDARY_DRIVE_D`
- `PEDRO_SECONDARY_DRIVE_F`
- `PEDRO_SECONDARY_DRIVE_T`
- `PEDRO_USE_SECONDARY_DRIVE_PID`

### Curve handling and hold behavior

- `PEDRO_CENTRIPETAL_SCALING`
- `PEDRO_AUTOMATIC_HOLD_END`
- `PEDRO_HOLD_POINT_TRANSLATIONAL_SCALING`
- `PEDRO_HOLD_POINT_HEADING_SCALING`
- `PEDRO_TURN_HEADING_ERROR_THRESHOLD_RAD`
- `PEDRO_BEZIER_CURVE_SEARCH_LIMIT`

### Robot model values

- `PEDRO_MASS`
- `PEDRO_FORWARD_ZERO_POWER_ACCELERATION`
- `PEDRO_LATERAL_ZERO_POWER_ACCELERATION`

### Predictive braking

- `PEDRO_USE_PREDICTIVE_BRAKING`
- `PEDRO_BRAKING_LINEAR`
- `PEDRO_BRAKING_QUADRATIC_FRICTION`
- `PEDRO_BRAKING_P`
- `PEDRO_BRAKING_MAX_POWER`

### Follower health and stuck detection

- `PEDRO_DRIVE_KALMAN_MODEL_COVARIANCE`
- `PEDRO_DRIVE_KALMAN_DATA_COVARIANCE`
- `PEDRO_STUCK_VELOCITY`
- `PEDRO_STUCK_T_VALUE`
- `PEDRO_STUCK_TIMEOUT`

## Tuning order

Do this in order.

1. Make sure teleop is already correct.
2. Make sure start pose and route geometry are correct.
3. Tune robot model values.
4. Tune translational correction.
5. Tune heading correction.
6. Tune drive correction.
7. Tune centripetal scaling on curves.
8. Tune predictive braking only after the rest is stable.

Do not start with centripetal or predictive braking if the robot still cannot track a straight line.

## 1. Preconditions

Before touching Pedro gains, confirm all of this already passes:

- module offsets are correct
- module inversion flags are correct
- hub orientation is correct
- Pinpoint offsets are correct
- wheel radius is correct
- `SwerveSystemCheck` forward / strafe / rotate all behave correctly
- `MainTeleOp` is controllable and not fighting itself

If those are wrong, Pedro tuning will lie to you.

## 2. Robot model values

These values tell Pedro what kind of drivetrain it is dealing with.

Tune:

- `PEDRO_MASS`
- `PEDRO_FORWARD_ZERO_POWER_ACCELERATION`
- `PEDRO_LATERAL_ZERO_POWER_ACCELERATION`

What they affect:

- coast behavior
- stop distance
- path lookahead response
- braking feel

How to tune:

1. Run a simple straight auto.
2. Watch whether the robot overshoots or feels too eager to stop.
3. If the robot coasts farther than Pedro expects, move the zero-power acceleration magnitudes closer to zero.
4. If the robot stops faster than Pedro expects, make the zero-power acceleration magnitudes more negative.
5. Use `PEDRO_MASS` only for smaller shaping changes after the zero-power values are close.

## 3. Translational PID

This is the first correction loop to tune for actual path tracking.

Tune:

- `PEDRO_TRANSLATIONAL_P`
- `PEDRO_TRANSLATIONAL_I`
- `PEDRO_TRANSLATIONAL_D`
- optionally the secondary translational PID set

Use this for:

- XY path error
- line tracking
- staying on the intended route

How to tune:

1. Start with a straight path.
2. Increase `PEDRO_TRANSLATIONAL_P` until the robot corrects path error firmly.
3. Add a small amount of `PEDRO_TRANSLATIONAL_D` if it oscillates or zig-zags around the path.
4. Leave `I` at zero unless there is repeatable steady-state error you cannot remove with sane `P`.
5. Only use the secondary translational PID after the primary one is already working.

Bad signs:

- low `P`: robot drifts off the path and takes too long to return
- high `P`: robot wobbles side to side
- high `D`: sluggish correction

## 4. Heading PID

This controls how the robot orients itself during the path.

Tune:

- `PEDRO_HEADING_P`
- `PEDRO_HEADING_I`
- `PEDRO_HEADING_D`
- `PEDRO_HEADING_PID_SWITCH_RAD`
- optional secondary heading PID

How to tune:

1. Use a route with an intentional heading change.
2. Increase `PEDRO_HEADING_P` until the robot turns decisively toward the target heading.
3. Add `D` if it overshoots and rings.
4. Keep `I` at zero unless there is stable residual heading bias.

Bad signs:

- low `P`: robot lags heading targets badly
- high `P`: robot snaps too hard and overshoots
- high `D`: heading response becomes dull

## 5. Drive PID

This is the follower's drive-response layer.

Tune:

- `PEDRO_DRIVE_P`
- `PEDRO_DRIVE_I`
- `PEDRO_DRIVE_D`
- `PEDRO_DRIVE_F`
- `PEDRO_DRIVE_T`
- `PEDRO_DRIVE_PID_SWITCH`

Start conservative here. This is not the first knob to touch.

How to tune:

1. Leave `I` and `F` at zero at first.
2. Add a little `P` until response tightens.
3. Add a little `D` only if the drive correction overshoots.
4. Touch `T` only if you know you need the filtered derivative behavior.

## 6. Centripetal scaling

This is the one you specifically asked about.

Tune:

- `PEDRO_CENTRIPETAL_SCALING`

What it does:

- changes how strongly Pedro corrects for curved-path lateral effects

When to tune it:

- only after straight and gentle paths are already working
- on arcs and linked curves

How to tune:

1. Run the same curve repeatedly.
2. If the robot cuts the inside of the curve, increase `PEDRO_CENTRIPETAL_SCALING`.
3. If the robot gets pushed too hard outward or looks over-corrected on curves, decrease it.

This is a curve-shape tuning knob, not a straight-line fix.

## 7. Predictive braking

Tune only after the rest is solid.

Tune:

- `PEDRO_USE_PREDICTIVE_BRAKING`
- `PEDRO_BRAKING_LINEAR`
- `PEDRO_BRAKING_QUADRATIC_FRICTION`
- `PEDRO_BRAKING_P`
- `PEDRO_BRAKING_MAX_POWER`

Use it when:

- the robot consistently overshoots end points
- the rest of path tracking is already clean

Leave it off if:

- you are still fixing basic route tracking
- you are still changing mass or zero-power acceleration

## 8. Secondary PID sets

Pedro supports secondary PID sets for translational, heading, and drive.

These are useful only if you have a real reason to switch behavior across error ranges.

That means:

- do not turn them on just because they exist
- get the primary PID working first

## 9. Dashboard workflow

Recommended workflow:

1. Put the robot on a repeatable route.
2. Open FTC Dashboard.
3. Change one family of values at a time.
4. Record the values that helped.
5. Stop when behavior is clean enough, not when every number feels "perfect."

Good route order:

1. short straight
2. long straight
3. gentle arc
4. tighter arc
5. multi-segment route with heading change

## 10. What not to do

Do not:

- tune centripetal first
- tune predictive braking first
- tune on a bad start pose
- tune multiple PID families at once
- assume a pathing problem is a Pedro problem before checking swerve geometry and localization

## 11. Live tuneable note

The current unified Pedro autos and simulator wrappers re-apply follower settings from `SwerveConfig` while running.

That means these values are:

- Dashboard tuneable
- redeploy-free
- intended for live iteration

If a change does not seem to affect behavior, check that you are running a Pedro auto path and not a teleop-only test mode.
