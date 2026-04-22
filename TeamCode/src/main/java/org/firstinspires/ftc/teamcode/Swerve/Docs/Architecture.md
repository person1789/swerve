# Swerve Controls Architecture

This document outlines the standard architectural layers for a high-performance swerve control system in FTC. Each layer has a specific responsibility and should only interact with the layer directly below it.

## 1. Hardware Layer (`Hardware`)
**Responsibility**: Interface with actual REV hardware (moter, servos, sensors).
- **SwerveModule**: Encapsulates one drive motor and one turn motor/encoder.
- **IMU/Sensors**: Handles orientation and raw sensor data.
- **HWMap**: A central class to initialize all hardware.

## 2. Kinematics Layer (`Logic.Kinematics`)
**Responsibility**: The mathematics of swerve.
- **Forward Kinematics**: Calculating robot velocity from module states.
- **Inverse Kinematics**: Calculating module states (angle and speed) from a desired robot velocity (Vx, Vy, Vomega).
- **Desaturator**: Ensures module speeds never exceed 1.0 while maintaining the ratio between modules.

## 3. Localization Layer (`Logic.Localization`)
**Responsibility**: Tracking where the robot is on the field.
- **Localizer**: Combines IMU data and module encoders (or dead-wheels/Pinpoint) to update the current `Pose`.
- **Latency Compensation**: Predicting position based on velocity and time since last update.

## 4. Control Layer (`Logic.Control`)
**Responsibility**: Deciding what the robot *should* do.
- **PIDControllers**: Standard PID loops for heading, position, etc.
- **Feedforward**: Calculating power needed based on physical constants (kV, kA, kS).
- **Path Followers**: Logic to follow a series of waypoints.

## 5. OpMode Layer (`OpModes`)
**Responsibility**: entry points and state machines.
- **TeleOp**: Maps joystick input to robot velocities.
- **Autonomous**: Controls the robot to complete a task.

---

### Integration with Swerve Scope
All layers should ideally report their state to a central "Logger" or "Scope" interface. 
- **Hardware**: Raw power, actual vs target angle.
- **Kinematics**: Target vector vs actual vector.
- **Localization**: (X, Y, Heading) pose.
- **Control**: Error values and PID output.
