# TeleOp Bring-Up Guide

This guide is for running `MainTeleOp` only. You do not need autonomous for this workflow.

## Is the code ready to run?

Yes. It is ready for a teleop field test in the basic sense that:

- `MainTeleOp` compiles
- the local unit tests pass
- the recent teleop reset and pose-handoff fixes are in place

Before a full drive, still verify:

1. hardware names in the configuration match `HWMap.java`
2. module offsets in `SwerveConfig.java` are close
3. hub orientation in `SwerveConfig.java` matches the real hub mounting
4. the robot really uses a 49 mm diameter wheel if you keep the current wheel-radius constant
5. the inch-based motion limits feel sane on the floor:
   - max linear speed: `72 in/s`
   - max linear acceleration: `72 in/s^2`
   - max linear jerk: `360 in/s^3`
6. the REV hub orientation matches the real robot:
   - logo facing `LEFT`
   - USB facing `DOWN`
7. the Pinpoint odometry pod offsets match the real robot:
   - `ODO_X_OFFSET_MM = -127.6669`
   - `ODO_Y_OFFSET_MM = -52.23`

## Required hardware names

`MainTeleOp` expects these names from `HWMap.java`:

- drive motors: `FLM`, `FRM`, `BRM`, `BLM`
- steer servos: `FLS`, `FRS`, `BRS`, `BLS`
- analog encoders: `FLE`, `FRE`, `BRE`, `BLE`
- Pinpoint: `odo`
- IMU: `imu`

If one of those names is wrong in the Robot Controller configuration, the OpMode will fail during init.

## TeleOp-only setup

### 1. Confirm the config values

Before running, check these in `SwerveConfig.java`:

- `OFFSETS`
- `INVERSIONS`
- `HUB_LOGO_DIR`
- `HUB_USB_DIR`
- `ODO_X_OFFSET_MM`
- `ODO_Y_OFFSET_MM`
- `WHEEL_RADIUS_METERS`

Current robot-specific values:

- `HUB_LOGO_DIR = LEFT`
- `HUB_USB_DIR = DOWN`
- `ODO_X_OFFSET_MM = -127.6669`
- `ODO_Y_OFFSET_MM = -52.23`

Offsets do not need to be perfect for a first smoke test, but they do need to be close enough that modules do not fight each other immediately.

### 2. Put the robot in a safe starting position

For teleop-only testing:

1. place the robot anywhere with open floor space
2. point the robot in the direction you want to count as field-forward
3. keep the robot still during init

Practical recommendation:

- point the robot away from the driver station before starting
- that makes the first field-centric check easier to read

### 3. Initialize and start `MainTeleOp`

Once the OpMode is selected:

1. press **INIT**
2. make sure the robot stays still while sensors come up
3. press **START**

If you want to redefine heading after start, tap the gamepad `START` button. The current code resets:

- the localizer heading
- the controller heading target
- the drivetrain smoother state

That keeps heading reset behavior clean during teleop.

## First test sequence

Run these in order.

### Test 1: Wheels off the floor

Put the robot on blocks first.

Check:

- no module spins wildly at idle
- small stick inputs make modules rotate toward sensible angles
- steering does not oscillate hard when you let go

If this looks wrong, stop here and re-check offsets and encoder inversion.

### Test 2: Slow forward and backward

Put the robot on the floor and use very small left-stick inputs.

Check:

- the robot moves mostly straight
- modules agree on direction
- no pod is obviously reversed

### Test 3: Slow strafe

Use small left-stick sideways commands.

Check:

- the robot slides left and right without one corner lagging badly
- the robot does not immediately yaw when you only asked for strafe

### Test 4: Slow rotation

Use small right-stick rotation only.

Check:

- the robot turns smoothly
- it stops trying to rotate when you release the stick

### Test 5: Heading reset

Rotate the robot to a random angle, then tap gamepad `START`.

Check:

- the current facing direction becomes the new field-forward reference
- pushing forward on the stick now drives in that re-zeroed direction

## Driver controls currently in use

From `MainTeleOp.java`:

- left stick Y: forward/back
- left stick X: strafe
- right stick X: rotate
- left bumper: cycle logger mode
- start/options: reset field heading

## What can still bite you on the real robot

Even with passing tests, these are still the most likely real-world issues.

### Module offsets are off

Symptoms:

- jittering at standstill
- diagonal fighting between pods
- translation command causes strong rotation

### Hub orientation is wrong

Symptoms:

- heading reset feels wrong
- field-centric drives in the wrong rotated frame

### Wheel radius does not match reality

Symptoms:

- telemetry and observer speed feel wrong relative to actual motion
- fallback localization drifts too fast or too slow

### One pod wiring direction is flipped

Symptoms:

- one corner always fights the others
- pure forward becomes a curve

## Fast rollback plan during testing

If the first floor test looks bad:

1. stop driving
2. put the robot back on blocks
3. test one axis at a time
4. verify offsets and inversion flags
5. re-test heading reset before trying full-speed motion

## Recommended next step

After the first successful teleop drive, use `SwerveSystemCheck` to validate canned forward, strafe, and rotate commands before pushing speed or retuning offsets.
