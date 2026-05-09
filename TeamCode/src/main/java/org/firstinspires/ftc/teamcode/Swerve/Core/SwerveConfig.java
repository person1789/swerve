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

        // ─────────────────────────────────────────────────────────────────────────
        // 1. Robot Geometry
        // ─────────────────────────────────────────────────────────────────────────

        /** Full track width in inches (left-to-right wheel centres). */
        public static double TRACK_WIDTH_IN = 9.921;

        /** Full wheelbase in inches (front-to-rear wheel centres). */
        public static double WHEEL_BASE_IN = 9.927;

        // ─────────────────────────────────────────────────────────────────────────
        // 2. Physical Limits (Hard Caps)
        // ─────────────────────────────────────────────────────────────────────────

        /** Physical top speed of the drivetrain in inches/second. */
        public static double MAX_LINEAR_SPEED_IN_S = 72.0;

        /** Max angular velocity in rad/s. */
        public static double MAX_ANGULAR_VELOCITY_RAD_S = 4.0;

        /** Loop period in seconds (target 20ms). */
        public static double LOOP_TIME_SEC = 0.020;

        /** Number of recent loop samples used by the rolling-average dt estimator. */
        public static int LOOP_TIME_AVERAGE_WINDOW = 8;

        /** Emit teleop telemetry every N loops to reduce hot-path overhead. */
        public static int TELEOP_TELEMETRY_INTERVAL_LOOPS = 2;

        /** Emit autonomous telemetry every N loops to reduce hot-path overhead. */
        public static int AUTO_TELEMETRY_INTERVAL_LOOPS = 3;

        /**
         * Largest allowed ratio between a measured loop time and the current rolling
         * average before the sample is treated as an outlier and ignored.
         */
        public static double LOOP_TIME_OUTLIER_MULTIPLIER = 2.0;

        /**
         * Absolute max loop time in seconds before the sample is treated as an outlier.
         */
        public static double LOOP_TIME_OUTLIER_MAX_SEC = 0.060; // 60ms

        // ─────────────────────────────────────────────────────────────────────────
        // 3. Motion Smoothing (Dynamics)
        // ─────────────────────────────────────────────────────────────────────────

        /** Max linear acceleration in inches/second^2. */
        public static double MAX_LINEAR_ACCEL_IN_S2 = 400.0;

        /**
         * Max linear jerk in inches/second^3.
         */
        public static double MAX_LINEAR_JERK_IN_S3 = 4000.0;

        /** Translation angle change above this enters redirect braking mode. */
        public static double TRANSLATION_REDIRECT_ANGLE_RAD = Math.toRadians(55.0);

        /** Exit redirect mode once translation speed falls below this fraction of max speed. */
        public static double TRANSLATION_REDIRECT_RELEASE_SPEED_FRACTION = 0.08;

        /** Extra braking authority used while redirecting between large translation angles. */
        public static double TRANSLATION_REDIRECT_DECEL_MULTIPLIER = 2.25;

        /** Enable current-angle feasibility scaling for translation during steering transitions. */
        public static boolean FEASIBLE_TRANSLATION_FILTER_ENABLED = true;

        /** Penalty strength used when projecting desired chassis velocity onto current wheel geometry. */
        public static double FEASIBLE_TRANSLATION_PENALTY = 18.0;

        /** Minimum translation authority allowed while the feasible-translation filter is active. */
        public static double FEASIBLE_TRANSLATION_MIN_AUTHORITY = 0.05;

        // ─────────────────────────────────────────────────────────────────────────
        // 4. Input Conditioning (Driver Feel)
        // ─────────────────────────────────────────────────────────────────────────

        /** Deadband threshold for driver inputs (component-wise). */
        public static double INPUT_DEADBAND = 0.05;

        // Joystick Sensitivity Curve
        public static double INPUT_INTERCEPT = 0.001;
        public static double INPUT_SPLINE_POINT = 0.66;
        public static double INPUT_SLOPE = 4.0;

        // ─────────────────────────────────────────────────────────────────────────
        // 5. Tuning (PID Gains)
        // ─────────────────────────────────────────────────────────────────────────

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

        // ─────────────────────────────────────────────────────────────────────────
        // 6. Hardware Specs & Calibration
        // ─────────────────────────────────────────────────────────────────────────
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
        public static RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO_DIR = RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
        public static RevHubOrientationOnRobot.UsbFacingDirection HUB_USB_DIR = RevHubOrientationOnRobot.UsbFacingDirection.DOWN;

        /** Angle error threshold in radians before flipping module direction. */
        public static double FLIP_THRESHOLD = Math.PI / 2.0;

        /** Diagnostic gate: hold drive speed at zero until pods reach steering targets. */
        public static boolean REQUIRE_STEER_READY_FOR_DRIVE = false;

        /** Steering-ready tolerance for allowing drive motion. */
        public static double STEER_READY_ANGLE_TOLERANCE_RAD = Math.toRadians(8.0);

        /** Below this steer error, drive authority is left untouched. */
        public static double STEER_DRIVE_FULL_AUTHORITY_RAD = Math.toRadians(6.0);

        /** Above this steer error, normal steer-error scaling reaches its minimum authority. */
        public static double STEER_DRIVE_HARD_CUTOFF_RAD = Math.toRadians(22.0);

        /**
         * Minimum drive authority retained during normal steer-error derating.
         * Set to 0 only if you explicitly want the robot to wait on module alignment.
         */
        public static double STEER_DRIVE_MIN_AUTHORITY = 0.12;

        /**
         * Minimum retained cosine-based drive factor during steer transitions.
         * This prevents large steer errors from silently zeroing wheel speed even when
         * the normal authority floor is nonzero.
         */
        public static double STEER_DRIVE_MIN_COSINE_FACTOR = 0.35;

        /**
         * Lock stance module angles in radians, ordered FL, FR, RR, RL.
         * Defaults to a turn-ready stance tangent to a pure in-place rotation.
         */
        public static double[] LOCKED_STANCE_ANGLES_RAD = {
                Math.toRadians(135.0),
                Math.toRadians(45.0),
                Math.toRadians(-45.0),
                Math.toRadians(-135.0)
        };

        // ─────────────────────────────────────────────────────────────────────────
        // 7. Filters & Observer
        // ─────────────────────────────────────────────────────────────────────────

        /** Low-pass filter gain for the velocity observer (0.0 to 1.0). */
        public static double OBSERVER_LPF_GAIN = 0.15;

        /** Enable dashboard telemetry during tuning only. */
        public static boolean DASHBOARD_ENABLED = true;

        // ─────────────────────────────────────────────────────────────────────────
        // 8. Drive Motor Physical Constants
        // ─────────────────────────────────────────────────────────────────────────

        /** Encoder ticks per revolution of the drive motor shaft. */
        public static double DRIVE_TICKS_PER_REV = 28.0; // Gobilda 6000 RPM Yellow Jacket

        /** Gear reduction between drive motor and wheel. */
        public static double DRIVE_GEAR_RATIO = 7.43; // Reduction

        /** Wheel radius in meters. */
        public static double WHEEL_RADIUS_METERS = 0.0245; // 49 mm diameter swerve wheel

        /** Current draw threshold in amps above which a stall is declared. */
        public static double DRIVE_CURRENT_THRESHOLD = 7.0;

        /**
         * Simulated steering max rate in rad/s.
         * Based on a 0.10 s / 60 deg steering spec at full command.
         */
        public static double SIM_MAX_STEER_RATE_RAD_PER_SEC = Math.toRadians(600.0);

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

        // ─────────────────────────────────────────────────────────────────────────
        // 9. Limelight Vision Relocalization
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Master enable switch for Limelight pose relocalization.
         *
         * FALSE (default): LimelightLocalizer is never instantiated. No hardware
         * map lookup is attempted. SwerveLocalizer runs on Pinpoint + IMU only.
         * The camera does not need to be physically connected.
         *
         * TRUE: LimelightLocalizer is created at OpMode init and applyVisionUpdate()
         * is called every loop where the camera produces a valid result.
         *
         * This is a @Config field — it can be toggled live on FTC Dashboard without
         * redeploying, which is useful for comparing odometry-only vs. vision-fused
         * accuracy during practice.
         */
        public static boolean LIMELIGHT_ENABLED = false;

        /**
         * Reject Limelight results that are older than this many milliseconds.
         * Staleness > threshold means the camera has not computed a new frame since
         * the last poll — applying a stale correction moves the pose backward in time.
         */
        public static long LIMELIGHT_MAX_STALENESS_MS = 100;

        /**
         * Suppress vision updates when the robot is spinning faster than this
         * threshold (degrees/second). Fast spins introduce IMU lag that corrupts
         * the MT2 heading seed, causing transient XY errors.
         */
        public static double LIMELIGHT_MAX_ANGULAR_VEL_DEG_S = 360.0;

        /**
         * Maximum allowed camera-to-tag depth (meters) when only one tag is visible.
         * Single-tag accuracy degrades at long range; multi-tag estimates are exempt.
         */
        public static double LIMELIGHT_MAX_TAG_DISTANCE_M = 3.0;

        /**
         * Blending alpha applied to the masterPose when exactly 1 tag is visible.
         * 0.10 = 10% vision correction per loop, 90% odometry retained.
         */
        public static double LIMELIGHT_SINGLE_TAG_ALPHA = 0.10;

        /**
         * Blending alpha applied to the masterPose when 2+ tags are visible.
         * Higher trust warranted by reduced ambiguity and better geometry.
         */
        public static double LIMELIGHT_MULTI_TAG_ALPHA = 0.30;

        /**
         * If vision disagrees with odometry by more than this many inches, perform
         * a hard snap to the vision pose (X/Y only) instead of a soft lerp.
         * This recovers from significant Pinpoint drift in one step.
         */
        public static double LIMELIGHT_HARD_RESET_THRESHOLD_IN = 18.0;
}
