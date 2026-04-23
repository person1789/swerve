# System Check OpMode

`SwerveSystemCheck` is a dedicated diagnostic OpMode for the swerve drivetrain. It is meant to answer one question cleanly:

"If I command a known chassis vector, what do the modules and chassis estimate actually do?"

That is the right layer to inspect when the robot feels wrong and `MainTeleOp` is too busy to show why.

This guide assumes you are using a REV Control Hub and that CSV files will be retrieved over a USB cable.

## What this OpMode is for

Use `SwerveSystemCheck` when you want to verify:

- module steering offsets
- module ordering
- encoder inversion
- drive motor direction
- steer servo direction
- axis conventions for forward and strafe
- commanded vs observed chassis motion
- whether one corner is fighting the other three
- whether stall or current behavior looks suspicious

Use it before a first drive, after rewiring, after changing offsets, after touching kinematics, or whenever teleop behavior feels wrong but not obviously broken.

## What this OpMode is not for

It is not a replacement for full teleop testing. It deliberately strips away a lot of the normal operator path:

- no field-centric transform
- no heading snap workflow
- no autonomous logic
- no normal driver-intent interpretation

That is intentional. The point is to isolate the drivetrain.

## Controls

Current controls in `SwerveSystemCheck`:

- `LB`: previous canned test
- `RB`: next canned test
- `DPAD UP`: increase command scale by 0.05
- `DPAD DOWN`: decrease command scale by 0.05
- `A`: hold to actively run the selected canned command
- `B`: stop the motion and reset the smoother

FTC Dashboard calibration values exposed by the OpMode:

- `dashboardOffsets`
- `dashboardInversions`

Those arrays are applied live while the OpMode runs.

## Current canned tests

The OpMode currently includes:

- `Stop`
- `Forward`
- `Backward`
- `Strafe Left`
- `Strafe Right`
- `Rotate CCW`
- `Rotate CW`

These tests are enough to expose most direction, offset, and convention problems without turning the OpMode into a large tuning workflow.

## Before you run it

### Physical setup

For the first run:

1. Put the robot on blocks.
2. Make sure wheels are free to spin and steer without touching the floor.
3. Verify battery is reasonably charged.
4. Make sure all REV hubs are powered and connected.
5. Start with a small command scale such as `0.20` to `0.35`.

Only go to the floor after the block test looks sane.

### Software setup

Before running, confirm:

- hardware names in `HWMap.java` match the Robot Controller configuration
- offsets in `SwerveConfig.java` are at least close
- `HUB_LOGO_DIR` and `HUB_USB_DIR` match the actual REV hub orientation
- wheel radius still matches your actual hardware
- `csvLoggingEnabled` is true if you want a run log written
- `SwerveConfig.DASHBOARD_ENABLED` is true if you want FTC Dashboard output mirrored live

Current robot-specific values:

- `HUB_LOGO_DIR = LEFT`
- `HUB_USB_DIR = DOWN`
- `ODO_X_OFFSET_MM = -127.6669`
- `ODO_Y_OFFSET_MM = -52.23`

## How to use it

### Quick-start version

1. Select `Swerve System Check` on the Driver Station.
2. Press `INIT`.
3. Watch for obvious bad behavior while the robot is still on blocks.
4. Use `LB` and `RB` to select a test.
5. Use `DPAD UP` and `DPAD DOWN` to choose a conservative command scale.
6. Hold `A` to run the selected command.
7. Release `A` to return to zero command.
8. Press `B` if you want to hard-stop the sequence and reset the smoother.
9. Stop the OpMode cleanly after the session so the CSV flushes fully.

### Recommended first session

Run this order:

1. `Stop`
2. `Forward`
3. `Backward`
4. `Strafe Left`
5. `Strafe Right`
6. `Rotate CCW`
7. `Rotate CW`

Do that once on blocks, then repeat on the floor at low scale.

## Live module calibration in FTC Dashboard

`SwerveSystemCheck` is set up as a live calibration OpMode, not just a read-only diagnostic.

While the OpMode is running, FTC Dashboard exposes:

- `dashboardOffsets`
- `dashboardInversions`

Those values are pushed into the drivetrain continuously, so you can:

1. leave the OpMode running
2. edit one module offset or inversion in FTC Dashboard
3. immediately observe the effect on module target/current angle behavior
4. keep iterating without exiting the OpMode

### Recommended workflow for offsets

1. Put the robot on blocks.
2. Start `SwerveSystemCheck`.
3. Select `Forward` at a small command scale.
4. Hold `A`.
5. Watch whether all four modules settle into the same forward-facing direction.
6. Adjust one entry in `dashboardOffsets` at a time until that module agrees with the others.

Repeat the same logic with:

- `Backward`
- `Strafe Left`
- `Strafe Right`

If a module is close but consistently mirrored, the issue may be inversion rather than offset.

### Recommended workflow for inversion flags

Use `dashboardInversions` when:

- one module angle response looks mirrored
- one pod always seems to choose the wrong side of the circle
- changing the offset alone never gets the module to agree with the others

Flip only one module at a time, then re-check:

- `Forward`
- `Strafe`
- `Rotate`

### Important note

The dashboard values are temporary live values for the running OpMode.

If you find values that work:

1. copy them back into `SwerveConfig.OFFSETS`
2. copy them back into `SwerveConfig.INVERSIONS`
3. rebuild or redeploy as needed

The CSV log also records the live offset and inversion values used during the run, so you can recover the good set later.

## What telemetry it provides

The OpMode sends telemetry to the Driver Station, and if dashboard telemetry is enabled it also appears in FTC Dashboard.

### Session-level telemetry

- selected test name
- whether the command is actively running
- runtime in seconds
- loop delta time in milliseconds
- whether CSV logging is enabled
- the output CSV filename

### Command telemetry

- command scale
- commanded `x`, `y`, and `omega` in normalized units
- commanded `x` and `y` in inches/second
- commanded `omega` in radians/second

### Chassis telemetry

- observed `x` velocity in inches/second
- observed `y` velocity in inches/second
- observed angular velocity in radians/second
- smoothed chassis `x`, `y`, and `omega`
- drivetrain state
- battery voltage
- configured max linear speed, acceleration, and jerk

The observed chassis values come from the drivetrain velocity observer. The smoothed values are the motion-smoother output between the commanded chassis target and the kinematics layer.

### Per-module telemetry

For each module:

- raw kinematics target angle in degrees
- raw kinematics target speed in inches/second
- post-auditor target angle in degrees
- post-auditor target speed in inches/second
- target steering angle in degrees
- current steering angle in degrees
- steering error in degrees
- target wheel speed in inches/second
- actual wheel speed in inches/second
- drive power command
- steer power command
- current draw in amps
- stall flag

The raw target values show the direct inverse-kinematics result. The post-auditor values show the same command after the auditor chooses the shorter steering path and wheel-direction flip. The final target/current fields show what each module actually received and how close it got.

That gives you a full record of the command path for each loop:

1. commanded chassis target
2. smoothed chassis target
3. raw kinematics module states
4. post-auditor module states
5. observed chassis response from the velocity observer
6. final per-module electrical and motion behavior

## How to read the telemetry

### Forward test

Expected:

- all modules point near the same direction
- left/right and front/back should not strongly disagree
- observed `x` should dominate if your forward convention is `x`
- `y` should stay near zero
- `omega` should stay near zero

Red flags:

- one module angle differs wildly from the other three
- one module current is high while speed stays low
- commanded forward produces a strong observed rotate term

### Strafe test

Expected:

- all modules point to the strafe direction
- the robot should not look like it wants to spin while strafing
- observed sideways velocity should dominate

Red flags:

- robot yaws during pure strafe
- one pod points about 180 degrees from the others
- one module constantly stalls or draws much more current than the others

### Rotation test

Expected:

- module target angles should form the rotation pattern you expect
- observed angular velocity should be the dominant term
- translational observed velocity should be relatively small

Red flags:

- one corner lags badly
- one pod tracks the wrong quadrant
- commanded rotation produces large translation

## What kinds of problems this OpMode catches well

This OpMode is especially strong at catching:

- front-left/front-right/back-right/back-left ordering mistakes
- analog encoder inversion mistakes
- one bad module offset
- one flipped motor direction
- one flipped servo direction
- unit mistakes in wheel-speed conversion
- raw kinematics that look reasonable but optimized states that are clipping or flipping in an unexpected way
- chassis-axis convention mismatches

## CSV logging

When `csvLoggingEnabled` is true, the OpMode writes a CSV file with a timestamped name like:

- `SwerveSystemCheck-<timestamp>.csv`

The CSV includes:

- runtime
- loop dt
- selected test
- whether the command was actively running
- command scale
- commanded chassis values
- smoothed chassis values
- observed chassis values from the velocity observer
- drivetrain state
- battery voltage
- raw kinematics targets
- post-auditor targets
- per-module target angle
- per-module current angle
- per-module angle error
- per-module target speed
- per-module actual speed
- per-module drive power
- per-module steer power
- per-module current draw
- per-module stall flag
- live dashboard offset values
- live dashboard inversion values

This is the best record of the run if you want to compare sessions or graph behavior, because it captures the full chain from command to observer output.

## How to extract the CSV on a REV Control Hub over USB

This is the documented retrieval workflow for this robot.

### What you need

- the robot powered by battery
- a USB-C data cable
- a Windows laptop

### Before you unplug anything

1. Release `A` so the test command is no longer running.
2. Wait about one second.
3. Stop the OpMode normally on the Driver Station.

Do that first so the CSV writer flushes and closes the file cleanly.

### Physical connection steps

1. Leave the Control Hub powered by the robot battery.
2. Plug a USB-C data cable into the Control Hub.
3. Plug the other end into your laptop.
4. Wait for Windows to detect the Control Hub storage.

### Finding the file in Windows

1. Open `This PC`.
2. Open the device storage for the Control Hub.
3. Open the internal shared storage.
4. Open the `FIRST` folder.
5. Open the `settings` folder.
6. Look for the newest file named like `SwerveSystemCheck-<timestamp>.csv`.

The OpMode uses the FTC settings-file path, so `FIRST/settings` is the place to check first.

### Copying the file

1. Copy the newest `SwerveSystemCheck-...csv` file to your laptop.
2. Rename it immediately with something meaningful.

Good examples:

- `2026-04-23-blocks-forward-offset-check.csv`
- `2026-04-23-floor-rotate-ccw-scale035.csv`

### If you do not see the file

Check these in order:

1. make sure `csvLoggingEnabled` was true in the OpMode
2. make sure you stopped the OpMode cleanly before connecting the cable
3. refresh the folder view in Windows
4. sort by modified time and look again in `FIRST/settings`
5. run a very short new test, stop it cleanly, and check for the newest timestamped file

### Recommended team workflow

1. run one short diagnostic session
2. stop the OpMode cleanly
3. connect the USB-C cable
4. pull the CSV from `FIRST/settings`
5. rename it with date, surface, test type, and command scale
6. only then start another major round of changes

## Important note about stopping the OpMode

Stop the OpMode cleanly after the run.

Why:

- the CSV writer flushes during the run
- it also flushes and closes during shutdown
- if you yank power immediately after a test, you risk ending up with a partial file

Recommended habit:

1. release `A`
2. wait a second
3. stop the OpMode normally
4. then extract the CSV

## Suggested analysis workflow

Use this process:

1. run one test session on blocks
2. extract the CSV
3. rename it with date and setup notes
4. repeat on the floor
5. compare forward, strafe, and rotate runs separately

Good filename examples after download:

- `2026-04-23-blocks-forward-offsets-v3.csv`
- `2026-04-23-floor-strafe-left-scale035.csv`

## What to do if the data looks wrong

### One module angle is consistently wrong

Look at:

- offset for that module
- encoder inversion for that module
- servo direction assumptions

### One module speed is low but current is high

Look at:

- mechanical binding
- wheel rubbing
- bad drive motor direction or power path
- a module stalled near a bad steering angle

### Observed chassis motion does not match the command

Look at:

- wheel radius
- module ordering
- track width / wheelbase
- one corner physically reversed

### Rotation creates translation or translation creates rotation

Look at:

- offsets
- module indexing
- kinematic geometry assumptions

## Bottom line

For this robot, `SwerveSystemCheck` gives you a repeatable drivetrain test, live telemetry in FTC Dashboard, and a CSV trail you can pull off the Control Hub over USB afterward.
