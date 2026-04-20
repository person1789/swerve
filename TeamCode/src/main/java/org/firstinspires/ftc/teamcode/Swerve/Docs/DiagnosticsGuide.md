# Hardware Diagnostics & Health Guide

The Swerve Control System includes real-time hardware monitoring to prevent motor burnout and mechanical damage.

## 1. Stall Detection
The system monitors the "Health" of each module every loop cycle. A **Stall Alarm** is triggered if:
*   Drive power is significant (> 30%).
*   Actual wheel velocity is near zero (< 0.05 m/s).
*   Current draw is high (> 9.0 Amps).

**Troubleshooting**:
If a stall alarm appears in the logs:
1. Check for field debris (carpet, tape) tangled in the wheels.
2. Verify that the steering module is not stuck in a bind.
3. Check for stripped gears.

## 2. Current Monitoring
Real-time current draw is logged to telemetry for each motor.
*   **Normal Driving**: 1.0A - 4.0A
*   **Heavy Acceleration**: 5.0A - 8.0A
*   **Danger Zone**: > 9.0A (sustained)

## 3. Observer Disparity
The **Velocity Observer** compares the encoder-measured speed to the IMU/Odometry speed.
*   If `Observed Velocity` >> `Odometry Velocity`, the robot is slipping (traction loss).
*   If `Observed Velocity` << `Odometry Velocity`, a module might be jammed or an encoder might be disconnected.
