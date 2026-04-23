# System Check OpMode

`SwerveSystemCheck` is a dedicated diagnostic OpMode for the swerve drivetrain. It is meant to answer one question cleanly:

"If I command a known chassis vector, what do the modules and chassis estimate actually do?"

That sounds simple, but it is exactly the layer you want when the robot feels wrong and `MainTeleOp` is too busy to tell you why.

This guide assumes you are using a REV-based FTC control system. If you have a REV Control Hub, the CSV extraction steps below are written for that setup first. If you are using an Expansion Hub with a separate Robot Controller phone, there is a section for that too.

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

Use it before a first drive, after rewiring, after changing offsets, after touching kinematics, or whenever teleop behavior feels "kind of wrong" but not obviously broken.

## What this OpMode is not for

It is not a replacement for full teleop testing. It deliberately strips away a lot of the normal operator path:

- no field-centric transform
- no heading snap workflow
- no autonomous logic
- no normal driver-intent interpretation

That is a feature, not a limitation. The point is to isolate the drivetrain.

## Controls

Current controls in `SwerveSystemCheck`:

- `LB`: previous canned test
- `RB`: next canned test
- `DPAD UP`: increase command scale by 0.05
- `DPAD DOWN`: decrease command scale by 0.05
- `A`: hold to actively run the selected canned command
- `B`: stop the motion and reset the smoother

## Current canned tests

The OpMode currently includes:

- `Stop`
- `Forward`
- `Backward`
- `Strafe Left`
- `Strafe Right`
- `Rotate CCW`
- `Rotate CW`

Those tests are intentionally enough to expose most direction, offset, and convention problems without turning the OpMode into a tuning lab.

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
- drivetrain state
- configured max linear speed, acceleration, and jerk

### Per-module telemetry

For each module:

- target steering angle in degrees
- current steering angle in degrees
- steering error in degrees
- target wheel speed in inches/second
- actual wheel speed in inches/second
- drive power command
- steer power command
- current draw in amps
- stall flag

That is enough to diagnose most real drivetrain issues without adding another special-purpose logger.

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
- observed chassis values
- drivetrain state
- per-module target angle
- per-module current angle
- per-module angle error
- per-module target speed
- per-module actual speed
- per-module drive power
- per-module steer power
- per-module current draw
- per-module stall flag

This is the best record of the run if you want to compare sessions or graph behavior.

## How to extract the CSV on a REV Control Hub

If you are using a REV Control Hub, there are two realistic ways to pull the CSV.

### Method 1: Robot Controller Console over Wi-Fi

Use this when you want the simplest no-cable workflow.

1. Connect your laptop to the Control Hub network.
2. Open the Robot Controller Console in a browser.
3. Go to the management area of the Control Hub interface.
4. Open the files or settings-file management area.
5. Find the newest file named like `SwerveSystemCheck-...csv`.
6. Download it to your laptop.

Use this right after the test session, while the latest file is still obvious.

### Method 2: USB-C direct file access from a PC

Use this when you want the most direct file access path.

1. Leave the Control Hub powered from the robot battery.
2. Plug a USB-C cable into the Control Hub top board and your computer.
3. On Windows, browse the Control Hub storage from `This PC`.
4. Open the internal shared storage.
5. Look for the `FIRST` area and the newest `SwerveSystemCheck-...csv` file.
6. Copy it to your computer.

This method is especially useful if Wi-Fi is inconvenient or if you want to pull several files at once.

## If you are using a REV Expansion Hub plus a Robot Controller phone

The OpMode still works the same way, but the CSV lives on the Robot Controller Android device rather than inside a Control Hub Android board.

Practical extraction methods:

1. Use Android Studio Device File Explorer.
2. Use normal Android file access over USB if supported by that phone.
3. Pull the newest `SwerveSystemCheck-...csv` from the app's settings/storage area.

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

For a REV-based FTC robot, `SwerveSystemCheck` is one of the highest-value debugging tools in this repo. It gives you a clean, repeatable drivetrain test, live telemetry in FTC Dashboard, and a CSV trail you can pull off the hub afterward and analyze like an adult instead of arguing with vibes.
