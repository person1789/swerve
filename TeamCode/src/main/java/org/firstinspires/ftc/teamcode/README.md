# Swerve Control System

A swerve drivetrain library for the FTC control loop.

## Current focus

This codebase is currently centered on:

- teleop bring-up
- drivetrain validation on real hardware
- canned diagnostic testing through `SwerveSystemCheck`
- local JVM tests for the math and control stack

Simulator and historical design notes still exist, but they are no longer the main entry point.

## Code layout

- `Hardware/`: motors, encoders, Pinpoint, and drivetrain orchestration
- `Logic/`: control, kinematics, localization, and observers
- `Geometry/`: vector and pose math types
- `Input/`: driver input shaping and smoothing
- `docs/`: current setup, tuning, and testing docs
- `docs/stale/`: older or superseded notes kept for reference

## Getting started

1. Initialize hardware in `HWMap.java`.
2. Calibrate module offsets in `SwerveConfig.java`.
3. Set the odometry offsets and REV hub orientation in `SwerveConfig.java` to match the robot.
4. Read `docs/TeleOpBringupGuide.md` before the first teleop-only run.
5. Use `docs/SystemCheckOpMode.md` before pushing speed or blaming PID.
6. Read `docs/SwerveConfigGuide.md` for the current configuration assumptions.
7. Read `docs/ArchitectureGuide.md` for the current control-path layout.
8. Read `docs/PIDTuningGuide.md` before tuning any closed-loop behavior.

## Testing and verification

The repo includes local JVM tests under `src/test/java`.

- Use `docs/LocalTestingGuide.md` for quick commands and test entry points.
- Use `docs/UnitTestingGuide.md` for deeper Gradle and test-structure details.
- Use `docs/SystemCheckOpMode.md` for on-robot canned-command diagnostics and CSV logging.
- Use `docs/stale/README.md` only for historical context, not day-to-day setup.
