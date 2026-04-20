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

## 4. Core Utilities (`Swerve.Core` & `Swerve.Geometry`)
- **SwerveConfig**: Centralized tuning for everything from PID gains to wheeloffsets. **This is the ONLY file most users will ever need to edit.**
- **MathUtil**: Specialized helpers for angle normalization and shortest-path calculation.
- **Pose/Point**: Basic math types used throughout the library.

---

## The Loop Flow
Each control cycle follows this path:
1. `SwerveTeleOp` reads joysticks and passes them to `SwerveDrivetrain`.
2. `SwerveDrivetrain` smooths the request using `MotionSmoother`.
3. `SwerveKinematics` calculates 4 target wheel states.
4. `SwerveAuditor` optimizes those states to minimize steering time.
5. `SwerveModule` instances use their internal **PIDControllers** to snap the wheels to the right angle and speed.
6. `SwerveLocalizer` updates the field position for field-centric driving.
