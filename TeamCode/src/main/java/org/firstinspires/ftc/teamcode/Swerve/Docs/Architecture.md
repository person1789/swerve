# Swerve System Architecture

This library follows a strict **Input -> Logic -> Hardware** abstraction hierarchy. This design separates driver-input logic from geometric math and hardware coordination, making the code easier to test and tune.

## 1. Input Layer (`Swerve.Input`)
- **Responsibility**: Takes raw joystick data and smooths it for the robot.
- **Key Components**: 
  - `JoystickScaling`: Applies non-linear curves to make fine control easier.
  - `MotionSmoother`: Implements S-curve profiles (jerk/accel limiting) to prevent the chassis from rocking or flipping during high-speed maneuvers.

## 2. Logic Layer (`Swerve.Logic`)
- **Responsibility**: The mathematical "brains" of the robot. 
- **Kinematics/**: 
  - `SwerveKinematics`: The core engine that calculates how each of the 4 wheels must move to achieve a desired chassis velocity.
  - `SwerveAuditor`: Optimizes wheel movements (e.g., flipping module directions so they only ever turn a maximum of 90 degrees).
- **Localization/**: 
  - `SwerveLocalizer`: Interfaces with the **GoBILDA Pinpoint** computer to track the robot's precise (X, Y, Heading) pose on the field.

## 3. Hardware Layer (`Swerve.Hardware`)
- **Responsibility**: Direct management of the robot's physical components.
- **Key Components**:
  - `SwerveDrivetrain`: The high-level coordinator that feeds smoothed inputs into the kinematics engine and commands the modules.
  - `SwerveModule`: Controls an individual "pod," managing its specific Drive Motor and Steer Servo via a local PID loop.
- **Config**: `swerve.xml` (located here) defines the hardware port mapping.

## 4. Operational Entry Points (`Swerve.OpModes`)
- **Main Driver Logic**:
  - `SwerveTeleOp`: The primary competitive tele-op mode.
  - `MainTeleOp`: Alternative/Legacy driver control logic.
  - `SwerveModulePIDTune`: A specialized utility for calibrating steering response.

## 5. Core Utilities (`Swerve.Core` & `Swerve.Geometry`)
- **Swerve.Core**:
  - `SwerveConfig`: Centralized tuning for everything from PID gains to wheel offsets.
  - `HWMap`: The "Bridge" that connects software variables to physical robot hardware.
  - `Logger`: A high-performance telemetry system for real-time debugging.
  - `Pinpoint`: Driver for the GoBILDA Pinpoint computer.
- **Swerve.Geometry**:
  - `MathUtil`: Specialized helpers for angle normalization.
  - `Pose`/`Point`/`D2Vector`: Math types used throughout the library.

## 6. Simulation & Testing (`swerve-scope/`)
- **Digital Twin**: Located outside the main Java source, this is a separate app that mirrors your robot's exact physics and logic for practice and testing.

---

## The Control Loop
Each 20ms control cycle follows this path:
1. `SwerveTeleOp` reads raw joysticks and passes them to `SwerveDrivetrain`.
2. `SwerveDrivetrain` identifies **Driver Intent** and calculates **System Limits**.
3. `MotionSmoother` performs **Asymmetric Braking** (Snappy human response, smooth system ramp).
4. `SwerveKinematics` calculates 4 target wheel states with **Skew Correction**.
5. `SwerveAuditor` optimizes those states to minimize steering turn-time.
6. `SwerveModule` instances use PID to command physical motors.
7. `SwerveLocalizer` updates the field coordinate (X, Y, θ).
