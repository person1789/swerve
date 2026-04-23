# Swerve System Architecture

This library follows a strict **Input -> Brain -> Pipeline -> Hardware** abstraction hierarchy. The system is designed around a unified **Vector domain language** to reduce unit conversion mistakes and keep drivetrain logic modular.

## 1. Input Layer (`Swerve.Input`)
- **Responsibility**: Processes raw driver intent into physically achievable velocity commands.
- **Key Components**: 
  - `MotionSmoother`: The **Sole Authority** for robot dynamics. It integrates non-linear joystick scaling, responsive braking logic, and S-curve smoothing (accel/jerk limiting) into a single high-performance pipeline.

## 2. Brain Layer (`Swerve.Logic.Control`)
- **Responsibility**: Interprets the driver's goal (e.g., "point at the goal while moving left").
- **Key Components**:
  - `SwerveController`: Manages state-driven logic like **Heading Retention** and **Cardinal Snapping**. It outputs the final target `Vector` for the drivetrain.

## 3. Pipeline Layer (`Swerve.Logic.Kinematics`)
- **Responsibility**: Translates chassis-level velocity into module-level motor commands.
- **Key Components**:
  - `SwerveKinematics`: Performs **Second-Order Inverse Kinematics** to calculate module speeds/angles while correcting for curvilinear "skew."
  - `SwerveAuditor`: Optimizes module movements to minimize turn time (e.g., direction flipping).

## 4. Hardware Layer (`Swerve.Hardware`)
- **Responsibility**: Direct hardware orchestration and feedback.
- **Key Components**:
  - `SwerveDrivetrain`: Coordinates the flow from the Controller through the Pipeline. It also manages the **Fail-Safe Localization** system.
  - `SwerveModule`: Controls an individual pod's motor and servo via local PID control.

## 5. Fail-Safe Localization (`Swerve.Logic.Localization`)
- **Responsibility**: Tracks the robot's pose on the field with triple-redundancy.
- **Key Components**:
  - `SwerveLocalizer`: Fuses **GoBILDA Pinpoint** (primary), **Internal IMU** (heading fallback), and **SwerveVelocityObserver** (encoder dead-reckoning) into a single robust field position.
  - `SwerveVelocityObserver`: Calculates actual chassis velocity from wheel feedback using Forward Kinematics.

## 6. Core Utilities (`Swerve.Core` & `Swerve.Geometry`)
- **Swerve.Config**: `SwerveConfig.java` is the centralized "Control Panel" for the robot. All PID gains, limits, and offsets are tuned here and can be updated in real-time via FTC Dashboard.
- **Swerve.Geometry**: Unified `Vector` class handles all N-dimensional arithmetic, rotations, and interpolation, replacing fragmented legacy types like `Point` and `Pose`.

---

## The Control Loop (50Hz+)
1. **Localization**: `SwerveLocalizer` fuses Pinpoint, IMU, and Wheel Feedback to find current `Pose`.
2. **Intent**: `OpMode` reads sticks and calls `SwerveController`.
3. **Brain**: `SwerveController` applies Heading Hold/Snap logic.
4. **Dynamics**: `MotionSmoother` enforces Accel/Jerk limits and applies joystick curves.
5. **Kinematics**: `SwerveKinematics` resolves the 3D Vector into 4 Module States.
6. **Execution**: `SwerveModule` updates the hardware.
