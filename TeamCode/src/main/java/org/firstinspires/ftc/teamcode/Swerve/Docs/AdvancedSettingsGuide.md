# Advanced Swerve Settings Guide

This guide explains the high-fidelity control features implemented in the "Superior" Swerve Control System.

## 1. Physics Feedforward ($kS, kV, kA$)
While the FTC SDK uses a normalized "Power" scale ($[-1, 1]$), our control system uses a **Voltage-Compensated Motor Model**. This ensures that "1.0 Volt" of force feels the same whether your battery is at 14V or 11V.

*   **$kS$ (Static Friction)**: The voltage needed to overcome static friction. Recommended: ~1.0V.
*   **$kV$ (Velocity Constant)**: Volts required per meter/second. Link this to your gear ratio.
*   **$kA$ (Acceleration Constant)**: Volts required for initial acceleration bursts.

**How it works**:
We measure the actual battery voltage every loop. The required voltage ($V_{target}$) is calculated, and the power sent to the motor is:
`power = V_target / actualBatteryVoltage`

**Tuning Logic**:
1. Increase $kS$ until the wheels barely turn at minimal input.
2. Adjust $kV$ so `Observed Velocity` matches `Target Velocity` in a steady state.
3. Add $kA$ to sharpen the "kick" when starting from a standstill.

## 2. Second-Order Kinematics (Discretization)
Standard swerve math assumes modules move in straight lines. During high-speed rotation, this causes "rotation-translation skew."
*   **The Fix**: Our system "discretizes" the velocity by integrating the desired motion over the upcoming loop window. 
*   **Result**: The robot remains perfectly centered during aggressive "spin-strafing" maneuvers.

## 3. Temporal Input Filtering (LPF)
To prevent joystick jitter from vibrating the modules, we use separate **Low-Pass Filters** for translation and rotation.
*   **Translation Gain**: Typically 0.20 for smooth driving.
*   **Rotation Gain**: Typically 0.25 for snappy heading snap response.
