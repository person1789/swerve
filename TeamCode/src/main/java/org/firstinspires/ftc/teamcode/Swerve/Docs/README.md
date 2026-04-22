# Swerve Library Overview

Welcome to the refactored Swerve codebase. This library provides a high-performance, unit-aware architecture designed for the most demanding FTC game challenges.

## 🚀 Core Features
- **Unified Vector Math**: All geometry and dynamics use a single, robust `Vector` class.
- **Fail-Safe Localization**: Triple-redundant fusion of GoBILDA Pinpoint, Internal IMU, and Forward Kinematics.
- **Unit-Aware Control**: Everything speaks in meters, radians, and seconds—no more "normalized power" guesswork.
- **Centralized Tuning**: `SwerveConfig` allows real-time tuning of every PID gain and limit via FTC Dashboard.

## 🕹️ Quick Start for Drivers

### 1. Heading Calibration
Press the **Options/Start** button to reset the field-centric heading. "Forward" will now be pointing away from you.

### 2. Cardinal Snapping
Use the **D-Pad** to snap the robot's heading to 0°, 90°, 180°, or 270° instantly.

### 3. Precision Control
The joysticks use non-linear cubic scaling. Use the center of the stick for fine adjustments and the edges for maximum speed.

## 🛠️ Developer Guide

### Where is the "Brain"?
- **`SwerveController`**: Handles the logic of what the robot *wants* to do (Heading Hold, Snapping).
- **`MotionSmoother`**: Handles the physics of how the robot *moves* (Scaling, Accel/Jerk limits).
- **`SwerveDrivetrain`**: Orchestrates the hardware execution.

### How do I tune?
Open `Swerve/Core/SwerveConfig.java`. It is organized into:
- **Geometry**: Wheelbase and trackwidth.
- **Limits**: Max speed, accel, and jerk.
- **PID Gains**: Steering and Heading coefficients.
- **Hardware**: Pinpoint offsets and motor inversions.

---
*Built for performance. Refactored for precision.*
