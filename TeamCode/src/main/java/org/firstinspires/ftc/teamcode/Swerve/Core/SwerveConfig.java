/**
 * SwerveConfig: The central control panel for the swerve drive.
 * This file contains all the "magic numbers" — like the robot's size, 
 * motor speeds, and steering offsets — in one place so they can be 
 * easily adjusted and tuned for the best performance.
 */
package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.config.Config;

/**
 * SwerveConfig
 * 
 * Central repository for all swerve-related constants. 
 * Using @Config allows adjustments via FTC Dashboard during tuning.
 */
@Config
public class SwerveConfig {

    // ─────────────────────────────────────────────────────────────────────────
    // Robot Geometry
    // ─────────────────────────────────────────────────────────────────────────
    
    /** Full track width in inches (left-to-right wheel centres). */
    public static double TRACK_WIDTH_IN = 9.921;
    
    /** Full wheelbase in inches (front-to-rear wheel centres). */
    public static double WHEEL_BASE_IN = 9.927;

    // ─────────────────────────────────────────────────────────────────────────
    // Performance Limits
    // ─────────────────────────────────────────────────────────────────────────

    /** Physical top speed of the drive motor in m/s. */
    public static double MAX_SPEED_MPS = 1.35;
    
    /** Max angular velocity in rad/s. */
    public static double MAX_ANGULAR_VELOCITY_RAD_S = 4.0;
    
    /** Max acceleration in m/s^2. */
    public static double MAX_ACCEL = 3.0; // Moderate for smoothness
    
    /** Max jerk in m/s^3. (Controls S-curve) */
    public static double MAX_JERK = 10.0; // Lower is smoother
    
    /** Loop period in seconds (target 20ms). */
    public static double LOOP_TIME_SEC = 0.020;

    // ─────────────────────────────────────────────────────────────────────────
    // Module Calibration (Offsets and Inversions)
    // ─────────────────────────────────────────────────────────────────────────
    // Order: [0] Front-Left, [1] Front-Right, [2] Rear-Right, [3] Rear-Left

    /** Encoder zero-forward offsets in radians. */
    public static double[] OFFSETS = {
            -0.2, // FL
             3.9, // FR
             1.4, // RR
             3.0  // RL
    };

    /** Inversion flags for steering encoders. */
    public static boolean[] INVERSIONS = {
            false, // FL
            false, // FR
            false, // RR
            false  // RL
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Localization (Pinpoint Odometry)
    // ─────────────────────────────────────────────────────────────────────────

    /** X offset of the Pinpoint pod in millimeters. */
    public static double ODO_X_OFFSET_MM = -127.6669;

    /** Y offset of the Pinpoint pod in millimeters. */
    public static double ODO_Y_OFFSET_MM = -52.23;

    // ─────────────────────────────────────────────────────────────────────────
    // Tuning (PID Gains)
    // ─────────────────────────────────────────────────────────────────────────

    public static double STEER_P = 0.325;
    public static double STEER_I = 0.0;
    public static double STEER_D = 0.01;
    public static double STEER_STATIC_FF = 0.0;
    
    public static double DRIVE_P = 0.1;
    public static double DRIVE_I = 0.0;
    public static double DRIVE_D = 0.0;

    // Heading Retention (Drift Correction)
    public static double HEADING_P = 1.0;
    public static double HEADING_I = 0.0;
    public static double HEADING_D = 0.05;
    public static double HEADING_HOLD_THRESHOLD = 0.02; // rad/s for "not turning"

    // Heading Snap Tuning (Intentional Snap)
    public static double SNAP_P = 2.0;
    public static double SNAP_I = 0.0;
    public static double SNAP_D = 0.1;

    // ─────────────────────────────────────────────────────────────────────────
    // Hardware Specs (Module Drive)
    // ─────────────────────────────────────────────────────────────────────────

    /** Gear ratio of the drive motors (typically for GoBilda 4-bar swerve). */
    public static double DRIVE_GEAR_RATIO = 1.0; // Standard 1:1 for now, note: adjust if using reduction gears

    /** Encoder ticks per revolution for the drive motor internal encoder. */
    public static double DRIVE_TICKS_PER_REV = 537.7; // Standard for GoBilda 5203 Series (312 RPM)

    /** Radius of the drive wheel in meters. */
    public static double WHEEL_RADIUS_METERS = 0.048; // Standard 96mm wheel

    // ─────────────────────────────────────────────────────────────────────────
    // Feedforward & Health (Physics Model)
    // ─────────────────────────────────────────────────────────────────────────

    /** Static friction voltage (Volts). Min voltage to move. */
    public static double DRIVE_KS = 1.05;

    /** Velocity constant (Volts / (m/s)). Volt per unit speed. */
    public static double DRIVE_KV = 4.2;

    /** Acceleration constant (Volts / (m/s^2)). Volt per unit accel. */
    public static double DRIVE_KA = 0.45;

    /** Current draw threshold for stall detection (Amps). */
    public static double DRIVE_CURRENT_THRESHOLD = 9.5;

    // ─────────────────────────────────────────────────────────────────────────
    // Observer & Input Filters
    // ─────────────────────────────────────────────────────────────────────────

    /** Low-pass filter gain for the velocity observer (0.0 to 1.0). */
    public static double OBSERVER_LPF_GAIN = 0.15;

    /** LPF for translation joysticks (0.1 to 0.3 recommended). */
    public static double TRANSLATION_LPF_GAIN = 0.20;

    /** LPF for rotation joysticks (0.2 to 0.4 recommended). */
    public static double ROTATION_LPF_GAIN = 0.25;

    // ─────────────────────────────────────────────────────────────────────────
    // State machine logic
    // ─────────────────────────────────────────────────────────────────────────

    /** Time in ms with no input before locking (X-stance). */
    public static double LOCK_DELAY_MS = 200.0;
}
