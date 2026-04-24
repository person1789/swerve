@depreciatted
# 🏎️ Movement Logic: Units, Curves, and Smoothing

This system treats the robot as a physical entity with mass and momentum, rather than just a collection of motors. All logic operates in **Standard Units** (m/s, rad/s).

## 1. The Unified Pipeline
The `MotionSmoother` is the sole authority for how the robot *feels* and *moves*. It handles two distinct responsibilities:

### A. Non-Linear Input Scaling
Joystick inputs are not linear. We apply a cubic sensitivity curve (tunable via `STICK_SCALAR`) to allow for pinpoint precision at low speeds while maintaining full power at the edges. This is calculated **component-wise** to allow for cleaner axial snapping.

### B. S-Curve Dynamics (Velocity & Jerk)
To prevent the robot from rocking, slipping, or flipping, all movement follows a **Jerk-Limited S-Curve**:
- **Acceleration Limit**: Prevents high-current spikes and wheel slip.
- **Jerk Limit**: Smooths the *rate of change* of acceleration, resulting in "elastic" and natural movement.

## 2. Intelligent Responsive Braking
The system distinguishes between **Intentional Deceleration** and **System-Imposed Limits**:
- **Braking**: If you release the stick or pull it back, the system detects a "Braking" state and bypasses the S-curve to give you immediate, crisp response.
- **Ramping**: If you push the stick forward, the system enforces the smooth S-curve to protect the hardware.

## 3. Second-Order Kinematics
Standard swerve kinematics suffer from "skew" when rotating and translating simultaneously. Our `SwerveKinematics` implements **Second-Order Discretization**:
- It pre-rotates the translational velocity vector by half the angular displacement expected over the next loop cycle.
- This ensures that if you command "Drive North while spinning," the robot travels in a perfectly straight line relative to the field.

## 4. Component-Wise Deadbanding
We use a component-wise deadband (`DEADBAND_THRESHOLD`) rather than a radial one. This allows the driver to "lock" onto cardinal axes (Pure X or Pure Y) more easily during precision tasks like scoring.

## 🛠️ Tuning via SwerveConfig
All movement constants are centrally located for real-time dashboard tuning:
- `MAX_LINEAR_SPEED_IN_S`: The physical top speed in inches/second.
- `MAX_LINEAR_ACCEL_IN_S2` / `MAX_LINEAR_JERK_IN_S3`: The "sharpness" of the smoothing.
- `STICK_SCALAR`: The sensitivity of the joystick curves.
