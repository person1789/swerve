package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;

/**
 * SwerveConfig
 * 
 * Central repository for all swerve-related constants. 
 * Categorized for ease of tuning via FTC Dashboard.
 */
@Config
public class SwerveConfig {

    // =========================================================================
    // 1. Robot Geometry
    // =========================================================================
    
    /** Full track width in inches (left-to-right wheel centres). */
    public static double TRACK_WIDTH_IN = 9.921;
    
    /** Full wheelbase in inches (front-to-rear wheel centres). */
    public static double WHEEL_BASE_IN = 9.927;

    // =========================================================================
    // 2. Physical Limits (Hard Caps)
    // =========================================================================

    /** Physical top speed of the drivetrain in inches/second. */
    public static double MAX_LINEAR_SPEED_IN_S = 72.0;
    
    /** Max angular velocity in rad/s. */
    public static double MAX_ANGULAR_VELOCITY_RAD_S = 4.0;
    
    /** Loop period in seconds (target 20ms). */
    public static double LOOP_TIME_SEC = 0.020;

    /** Number of recent loop samples used by the rolling-average dt estimator. */
    public static int LOOP_TIME_AVERAGE_WINDOW = 8;

    /**
     * Largest allowed ratio between a measured loop time and the current rolling
     * average before the sample is treated as an outlier and ignored.
     */
    public static double LOOP_TIME_OUTLIER_MULTIPLIER = 2.0;

    /** Absolute max loop time in seconds before the sample is treated as an outlier. */
    public static double LOOP_TIME_OUTLIER_MAX_SEC = 0.060; //60ms

    // =========================================================================
    // 3. Motion Smoothing (Dynamics)
    // =========================================================================

    /** Max linear acceleration in inches/second^2. */
    public static double MAX_LINEAR_ACCEL_IN_S2 = 72.0;
    
    /** Max linear jerk in inches/second^3. Reaches max accel in about 0.2 seconds. */
    public static double MAX_LINEAR_JERK_IN_S3 = 360.0;

    // =========================================================================
    // 4. Input Conditioning (Driver Feel)
    // =========================================================================

    /** Deadband threshold for driver inputs (component-wise). */
    public static double INPUT_DEADBAND = 0.05;

    // Joystick Sensitivity Curve
    public static double INPUT_INTERCEPT = 0.001;
    public static double INPUT_SPLINE_POINT = 0.66;
    public static double INPUT_SLOPE = 4.0;

    // =========================================================================
    // 5. Tuning (PID Gains)
    // =========================================================================

    // Steering Module PID
    public static double STEER_P = 0.325;
    public static double STEER_I = 0.0;
    public static double STEER_D = 0.01;
    
    // Drive Motor PID (if using RUN_USING_ENCODER)
    public static double DRIVE_P = 0.1;
    public static double DRIVE_I = 0.0;
    public static double DRIVE_D = 0.0;

    // Heading Retention (Maintain Heading)
    // Disabled by default until tuned on the real robot.
    public static double HEADING_P = 0.0;
    public static double HEADING_I = 0.0;
    public static double HEADING_D = 0.0;
    public static double HEADING_LOCK_DELAY_S = 0.1;

    // Heading Snap (Intentional Target)
    // Disabled by default until tuned on the real robot.
    public static double SNAP_P = 0.0;
    public static double SNAP_I = 0.0;
    public static double SNAP_D = 0.0;

    // =========================================================================
    // 6. Hardware Specs & Calibration
    // =========================================================================
    // Order: [0] Front-Left, [1] Front-Right, [2] Rear-Right, [3] Rear-Left

    /** Encoder zero-forward offsets in radians. */
    public static double[] OFFSETS = { -0.2, 3.9, 1.4, 3.0 };

    /** Inversion flags for steering encoders. */
    public static boolean[] INVERSIONS = { false, false, false, false };

    /** Odometry pod offsets relative to center in mm. */
    public static double ODO_X_OFFSET_MM = -127.6669;
    public static double ODO_Y_OFFSET_MM = -52.23;

    /** Time in ms with no input before locking (X-stance). */
    public static double LOCK_DELAY_MS = 200.0;

    /** Whether the drivetrain should automatically enter X-stance while idle. */
    public static boolean ENABLE_IDLE_X_STANCE = false;

    /** Hub Orientation for fallback IMU. */
    public static RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO_DIR = 
            RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
    public static RevHubOrientationOnRobot.UsbFacingDirection HUB_USB_DIR =
            RevHubOrientationOnRobot.UsbFacingDirection.DOWN;

    /** Angle error threshold in radians before flipping module direction. */
    public static double FLIP_THRESHOLD = Math.PI / 2.0;

    // =========================================================================
    // 7. Filters & Observer
    // =========================================================================

    /** Low-pass filter gain for the velocity observer (0.0 to 1.0). */
    public static double OBSERVER_LPF_GAIN = 0.15;

    /** Enable dashboard telemetry during tuning only. */
    public static boolean DASHBOARD_ENABLED = true;

    // =========================================================================
    // 8. Drive Motor Physical Constants
    // =========================================================================

    /** Encoder ticks per revolution of the drive motor shaft. */
    public static double DRIVE_TICKS_PER_REV = 28.0; // Gobilda 6000 RPM Yellow Jacket

    /** Gear reduction between drive motor and wheel. */
    public static double DRIVE_GEAR_RATIO = 7.43; // Reduction

    /** Wheel radius in meters. */
    public static double WHEEL_RADIUS_METERS = 0.0245; // 49 mm diameter swerve wheel

    /** Current draw threshold in amps above which a stall is declared. */
    public static double DRIVE_CURRENT_THRESHOLD = 7.0;

    public static double getMaxLinearSpeedMPS() {
        return inchesToMeters(MAX_LINEAR_SPEED_IN_S);
    }

    public static double getMaxLinearAccelMPS2() {
        return inchesToMeters(MAX_LINEAR_ACCEL_IN_S2);
    }

    public static double getMaxLinearJerkMPS3() {
        return inchesToMeters(MAX_LINEAR_JERK_IN_S3);
    }

    public static double inchesToMeters(double inches) {
        return inches * 0.0254;
    }

    // =========================================================================
    // 9. Limelight Vision Relocalization
    // =========================================================================

    /**
     * Master enable switch for Limelight pose relocalization.
     */
    public static boolean LIMELIGHT_ENABLED = false;

    /**
     * Reject Limelight results that are older than this many milliseconds.
     */
    public static long LIMELIGHT_MAX_STALENESS_MS = 100;

    /**
     * Suppress vision updates when the robot is spinning faster than this
     * threshold (degrees/second).
     */
    public static double LIMELIGHT_MAX_ANGULAR_VEL_DEG_S = 360.0;

    /**
     * Maximum allowed camera-to-tag depth (meters) when only one tag is visible.
     */
    public static double LIMELIGHT_MAX_TAG_DISTANCE_M = 4.0;

    /**
     * Blending alpha applied to the masterPose when exactly 1 tag is visible.
     */
    public static double LIMELIGHT_SINGLE_TAG_ALPHA = 0.05;

    /**
     * Blending alpha applied to the masterPose when 2+ tags are visible.
     */
    public static double LIMELIGHT_MULTI_TAG_ALPHA = 0.15;

    /**
     * If vision disagrees with odometry by more than this many inches, perform
     * a hard snap to the vision pose (X/Y only) instead of a soft lerp.
     */
    public static double LIMELIGHT_HARD_RESET_THRESHOLD_IN = 18.0;
}