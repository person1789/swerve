package org.firstinspires.ftc.teamcode.Swerve.Drive;

import com.acmerobotics.dashboard.config.Config;

/**
 * SwerveConfig — Phase C
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
    // Tuning (PID Gains)
    // ─────────────────────────────────────────────────────────────────────────

    public static double STEER_P = 0.325;
    public static double STEER_I = 0.0;
    public static double STEER_D = 0.01;
    public static double STEER_STATIC_FF = 0.0;
    
    public static double DRIVE_P = 0.1;
    public static double DRIVE_I = 0.0;
    public static double DRIVE_D = 0.0;

    // ─────────────────────────────────────────────────────────────────────────
    // State machine logic
    // ─────────────────────────────────────────────────────────────────────────

    /** Time in ms with no input before locking (X-stance). */
    public static double LOCK_DELAY_MS = 200.0;
}
