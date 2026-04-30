# Pedro Auto Self Tune Guide

`Pedro Auto Self Tune` is a robot-side autonomous tuning OpMode for the Pedro follower stack.

It does three jobs:

1. runs a short repeatable autonomous routine
2. records `Auto Telemetry` and CSV data from the follower, localizer, and drivetrain
3. computes recommended Pedro tuning values and can write them back into the FTC Dashboard tune classes

## Start assumption

By default the OpMode assumes the robot starts at:

- field center: `(0 in, 0 in)`
- heading: `0 rad`

This means the robot is treated as facing the same direction as the driver.

That default is controlled by:

- `USE_CENTER_START`

If you turn that off, the OpMode falls back to the current Pedro route start pose selection.

## What it runs

The routine currently executes these phases:

1. `Forward24`
2. `ReturnCenterX`
3. `Strafe24`
4. `ReturnCenterY`
5. `Curve90`

The distances are FTC Dashboard tuneable:

- `FORWARD_TEST_IN`
- `STRAFE_TEST_IN`
- `CURVE_TEST_IN`

## What it records

The telemetry suite is grouped under `Auto Telemetry`.

Live telemetry includes:

- phase name
- loop dt
- target pose
- actual pose
- translation error
- heading error
- chassis velocity
- distance remaining
- translation authority
- steer-ready state
- drivetrain state
- battery voltage
- Pinpoint used / invalid loop count
- raw Pinpoint pose
- current primary and secondary Pedro PID values
- centripetal scaling

If `CSV_LOGGING_ENABLED` is true, the OpMode also writes a CSV in the FTC settings folder.

## What it tunes

Recommendations are produced for:

- primary translation PID
- secondary translation PID
- primary heading PID
- secondary heading PID
- primary drive PID
- secondary drive PID
- centripetal scaling

These values live in the independent FTC Dashboard classes under:

`org.firstinspires.ftc.teamcode.pedroPathing.tuning`

## How apply works

If `APPLY_RECOMMENDATIONS = true`, the OpMode writes the recommended values back into the live tuning classes at the end of the run.

That means the new values become the new FTC Dashboard values immediately for the current session.

## Important limitation

This is not a full rigid-body system identification solver.

It is a practical autonomous tuning pass that uses measured tracking error, speed, heading behavior, authority loss, and localization health to produce better starting PID and centripetal values.

It should be treated as:

- a fast first-pass tuning tool
- a repeatable data collection OpMode
- a way to narrow the search space before final manual tuning

## Recommended workflow

1. Put the robot at the center of the field facing driver-forward
2. Open FTC Dashboard
3. Run `Pedro Auto Self Tune`
4. Watch `Auto Telemetry`
5. Pull the CSV if you want offline analysis
6. Re-run after each recommendation pass until the errors stabilize
7. Finish with short manual validation autos
