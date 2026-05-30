package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;

/**
 * Dashboard-tunable constants for the simplified Kooky-style swerve stack.
 */
@Config
public class SwerveConfig {

        /** Full track width in inches, measured left wheel center to right wheel center. */
        public static double TRACK_WIDTH_IN = 9.63;

        /** Full wheelbase in inches, measured front wheel center to rear wheel center. */
        public static double WHEEL_BASE_IN = 8.502;

        /** Physical top speed of the drivetrain in inches per second. */
        public static double MAX_LINEAR_SPEED_IN_S = 52.95;

        /** Physical top angular velocity in radians per second. */
        public static double MAX_ANGULAR_VELOCITY_RAD_S = 4.0;

        /** Base voltage the PID loops are tuned for. */
        public static double NOMINAL_VOLTAGE = 13.0;
        
        /** True to enable dynamic battery voltage compensation for drive motors. */
        public static boolean VOLTAGE_COMPENSATION_ENABLED = true;

        /** Nominal loop period used by second-order kinematics before measured dt is available. */
        public static double LOOP_TIME_SEC = 0.020;

        /** Component-wise joystick deadband. */
        public static double INPUT_DEADBAND = 0.05;

        /** Auto PID gains, Kooky-style normalized drive output from inch/radian pose error. */
        public static double AUTO_X_P = 0.04;
        public static double AUTO_X_D = 0.05;
        public static double AUTO_Y_P = 0.04;
        public static double AUTO_Y_D = 0.05;
        public static double AUTO_HEADING_P = 0.6;
        public static double AUTO_HEADING_D = 0.3;

        /** Auto output clamps and completion tolerances. */
        public static double AUTO_MAX_TRANSLATION_POWER = 1.0;
        public static double AUTO_MAX_TURN_POWER = 0.5;
        public static double AUTO_TRANSLATION_DEADBAND = 0.01;
        public static double AUTO_TRANSLATION_TOLERANCE_IN = 0.25;
        public static double AUTO_HEADING_TOLERANCE_RAD = Math.toRadians(1.0);
        public static double AUTO_SETTLE_DELAY_MS = 0.0;
        public static double AUTO_MOVE_TIMEOUT_MS = 2500.0;

        /** Auto module steering gate. */
        public static double AUTO_AZIMUTH_TOLERANCE_RAD = Math.toRadians(8.0);
        public static double AUTO_AZIMUTH_TIMEOUT_MS = 350.0;
        public static boolean AUTO_ABORT_ON_AZIMUTH_TIMEOUT = true;

        /** Pinpoint odometry pod offsets relative to robot center, kept from this year's Decode code. */
        public static double ODO_X_OFFSET_MM = -127.6669;
        public static double ODO_Y_OFFSET_MM = -52.23;

        /** This year's Pedro branch module geometry, ordered FL, FR, BR, BL. */
        public static double[] MODULE_X_IN = { 4.251, 4.251, -4.251, -4.251 };
        public static double[] MODULE_Y_IN = { 4.815, -4.815, -4.815, 4.815 };

        /** This year's drive-only auto poses from Decode GeneratedTraj. */
        public static double AUTO_CLOSE_START_X_IN = 120.0;
        public static double AUTO_CLOSE_START_Y_IN = 127.87;
        public static double AUTO_CLOSE_START_HEADING_RAD = Math.toRadians(319.6);
        public static double AUTO_CLOSE_SCORE_X_IN = 86.720;
        public static double AUTO_CLOSE_SCORE_Y_IN = 90.0;
        public static double AUTO_CLOSE_SCORE_HEADING_RAD = 0.0;
        public static double AUTO_FAR_START_X_IN = 89.0;
        public static double AUTO_FAR_START_Y_IN = 8.0;
        public static double AUTO_FAR_START_HEADING_RAD = 0.0;
        public static double AUTO_FAR_SCORE_X_IN = 105.0;
        public static double AUTO_FAR_SCORE_Y_IN = 30.5;
        public static double AUTO_FAR_SCORE_HEADING_RAD = 0.0;

        /** Probably should be tuned some day. This controller receives radians, not Pedro's pod degrees. */
        public static double STEER_P = 0.325;
        public static double STEER_I = 0.0;
        public static double STEER_D = 0.01;

        /** Below this steering error, the CRServo is commanded to zero. */
        public static double STEER_DEADBAND_RAD = 0.02;

        /** Encoder zero-forward offsets in radians, ordered FL, FR, BR, BL. */
        public static double[] OFFSETS = { 5.0, 0.2, -1.2, 1.3 };

        /** Encoder inversion flags, ordered FL, FR, BR, BL. */
        public static boolean[] INVERSIONS = { false, false, false, false };

        /** Time in ms with no driver input before the FSM enters locked stance. */
        public static double LOCK_DELAY_MS = 200.0;

        /** Module lock angles in radians, ordered FL, FR, BR, BL. 
         * Calculated geometrically to point exactly towards the center of rotation.
         */
        public static double[] LOCKED_STANCE_ANGLES_RAD = {
                Math.atan2(MODULE_X_IN[0], -MODULE_Y_IN[0]),
                Math.atan2(MODULE_X_IN[1], -MODULE_Y_IN[1]),
                Math.atan2(MODULE_X_IN[2], -MODULE_Y_IN[2]),
                Math.atan2(MODULE_X_IN[3], -MODULE_Y_IN[3])
        };

        /** Angle error threshold in radians before reversing drive direction. */
        public static double FLIP_THRESHOLD = Math.PI / 2.0;

        /** IMU orientation. */
        public static RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO_DIR =
                RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
        public static RevHubOrientationOnRobot.UsbFacingDirection HUB_USB_DIR =
                RevHubOrientationOnRobot.UsbFacingDirection.DOWN;

        /** Enable FTC Dashboard telemetry in MainTeleOp. */
        public static boolean DASHBOARD_ENABLED = true;

        /** Drive encoder ticks per motor revolution. */
        public static double DRIVE_TICKS_PER_REV = 28.0;

        /** Gear reduction between drive motor shaft and wheel. */
        public static double DRIVE_GEAR_RATIO = 7.43;

        /** Wheel radius in meters. */
        public static double WHEEL_RADIUS_METERS = 0.0245;

        public static double getMaxLinearSpeedMPS() {
                return inchesToMeters(MAX_LINEAR_SPEED_IN_S);
        }

        public static double inchesToMeters(double inches) {
                return inches * 0.0254;
        }
}
