# Superior Swerve Control System

A high-performance swerve drivetrain library for the FTC control loop.

## Key Features

- Second-order kinematics for combined translation and rotation
- Open-loop drive power with encoder-based velocity observation
- Local steering PID at each module
- Jerk-limited motion smoothing
- Browser-based mocked-hardware simulator for drivetrain logic

## System Architecture

- `Hardware/`: motors, encoders, Pinpoint, and drivetrain orchestration
- `Logic/`: control, kinematics, localization, and observers
- `Geometry/`: vector and pose math types
- `Input/`: driver input shaping and smoothing
- `docs/`: tuning, simulation, testing, and audit docs

## Getting Started

1. Initialize hardware in `HWMap.java`.
2. Calibrate module offsets in `SwerveConfig.java`.
3. Read `docs/AdvancedSettingsGuide.md` for the current control-scope assumptions.
4. Read `docs/PIDTuningGuide.md` before tuning any closed-loop behavior.

## Testing And Verification

The repo includes local JVM tests under `src/test/java`.

- Use `docs/UnitTestingGuide.md` for test structure and Gradle details.
- Use `docs/Simulator.md` to run the mocked-hardware drivetrain simulator.

---
Built for competition robotics and ongoing iteration.
