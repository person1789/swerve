# PID Tuning Guide

This guide covers the PID loops that still matter in the simplified swerve stack.

Tune in this order:

1. Steering module PID
2. Kooky-style autonomous pose controller if you use auto

Do not tune higher-level behavior until the robot already behaves correctly in:

- `MainTeleOp`
- `SwerveSystemCheck`

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

## 2. Kooky-style autonomous pose controller

If you are not running autonomous right now, skip this section.

Source:

- `auto/KookyAutoController.java`
- `auto/SimpleAutoSequence.java`

Purpose:

- Drives the robot from one target pose to the next without a heavy pathing framework.

See:

- `KookyAutoTuningGuide.md`

## Tuning Stop Rules

Stop tuning and fix the underlying system first if any of these are true:

- module zero offsets are not validated
- localization pose is inconsistent
- heading units are inconsistent
- autonomous start pose is wrong

PID tuning cannot compensate for bad geometry, bad units, or bad localization.
