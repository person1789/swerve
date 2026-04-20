# 🏎️ Movement Logic: S-Curve & Asymmetric Braking

This document explains the advanced motion profiling logic that gives this swerve drivetrain its high-performance feel.

## 1. S-Curve Smoothing (Velocity & Jerk)

To prevent motor saturation and mechanical wear, all acceleration is filtered through a **Jerk-Limited S-Curve** in the `MotionSmoother` class.

- **Acceleration Limit**: Prevents the motors from drawing too much current during initial takeoff.
- **Jerk Limit**: Smooths the *change* in acceleration, ensuring that speed increases are elastic and fluid rather than robotic and jerky.

## 2. Intelligent Asymmetric Braking

The core "magic" of this system is its ability to distinguish between **Driver Intent** and **System Controls**.

### The Problem
Traditional smoothing makes the robot feel "slidy" when trying to stop, as the smoother ramps down the speed slowly even when you pull your finger off the stick.

### The Solution: Asymmetric Logic
The `MotionSmoother` now receives two separate targets:
1.  **Driver Target**: The raw stick input.
2.  **System Limit**: The scaled version of that input (after `SwerveAuditor` ensures motors won't saturate).

**Behavior Logic:**
- **When Speeding Up**: The robot follows the smooth S-curve.
- **When Slowing Down (By Choice)**: If the `MotionSmoother` detects that the **Driver Target** is decreasing (you pulled back on the stick), it **bypasses all smoothing** and snaps to the target instantly.
- **When Slowing Down (By System)**: If the robot slows down because of an internal limit (but your stick is still pushed forward), it **maintains smooth deceleration** to keep the chassis stable.

## 3. Skew Correction

At high rotational speeds, swerve drives tend to "skew" or orbit slightly. Our `SwerveKinematics` includes a **Half-Angle Skew correction** that pre-calculates where the wheels *will be* after the frame's rotation, ensuring a perfectly straight line during high-speed spinning maneuvers.

## 🛠️ Tuning
Values for these systems are located in `SwerveConfig.java`:
- `MAX_ACCEL`: Maximum m/s² allowed.
- `MAX_JERK`: Maximum m/s³ allowed (the higher this is, the "snappier" it feels).
