# Swerve Library Overview

Welcome to the custom Swerve codebase. This library is designed for zero-dependency, high-performance robot control. It includes built-in support for **Field-Centric driving**, **Jerk-Limited S-Curves**, and **Pinpoint Localization**.

## Quick Start for New Programmers

### 1. Where do I tune the robot?
Go to `Swerve/Core/SwerveConfig.java`. This file contains all the constants for:
- PID gains for steering and heading snap.
- Digital offsets for your wheel pods.
- Speed and acceleration limits.

### 2. How do I run the robot?
Use the `SwerveTeleOp` OpMode in the `Swerve/OpModes` folder. 
- **Options Button**: Resets the field-centric heading (points "forward" away from you).
- **D-Pad**: Snaps the robot to cardinal directions (0, 90, 180, 270 degrees).
- **Left Stick**: Move the robot in any direction.
- **Right Stick**: Rotate the robot.

### 3. How do I verify my changes?
Before testing on the real field, always run the **Unit Tests** on your computer. See the [Unit Testing Guide](./UnitTestingGuide.md) for instructions.

## Documentation Index
- [Architecture & Data Flow](./Architecture.md): How the "brains" of the robot work.
- [Unit Testing Guide](./UnitTestingGuide.md): How to prevent bugs on your laptop.

---
*Created for the FTC Swerve Project.*
