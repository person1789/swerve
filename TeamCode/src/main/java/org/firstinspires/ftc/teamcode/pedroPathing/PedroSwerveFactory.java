package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.control.PredictiveBrakingCoefficients;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

/**
 * Central Pedro factory for the current robot configuration.
 */
public final class PedroSwerveFactory {
    private PedroSwerveFactory() {
    }

    public static FollowerConstants createFollowerConstants() {
        FollowerConstants followerConstants = new FollowerConstants();
        applyToFollowerConstants(followerConstants);
        return followerConstants;
    }

    public static void applyLiveTuning(Follower follower) {
        if (follower == null) {
            return;
        }

        FollowerConstants followerConstants = follower.getConstants();
        if (followerConstants == null) {
            followerConstants = new FollowerConstants();
        }

        applyToFollowerConstants(followerConstants);
        follower.setConstants(followerConstants);
        follower.setTranslationalPIDFCoefficients(primaryTranslationalPid());
        follower.setSecondaryTranslationalPIDFCoefficients(secondaryTranslationalPid());
        follower.setHeadingPIDFCoefficients(primaryHeadingPid());
        follower.setSecondaryHeadingPIDFCoefficients(secondaryHeadingPid());
        follower.setDrivePIDFCoefficients(primaryDrivePid());
        follower.setSecondaryDrivePIDFCoefficients(secondaryDrivePid());
        follower.setCentripetalScaling(SwerveConfig.PEDRO_CENTRIPETAL_SCALING);
        follower.useTranslational = SwerveConfig.PEDRO_USE_TRANSLATIONAL_PID;
        follower.useHeading = SwerveConfig.PEDRO_USE_HEADING_PID;
        follower.useDrive = SwerveConfig.PEDRO_USE_DRIVE_PID;
        follower.usePredictiveBraking = SwerveConfig.PEDRO_USE_PREDICTIVE_BRAKING;
        follower.useCentripetal = SwerveConfig.PEDRO_CENTRIPETAL_SCALING != 0.0;
        follower.updateConstants();
    }

    public static PedroRobot createRobot(HardwareMap hardwareMap, Logger logger) {
        PedroUnifiedSwerveStack stack = new PedroUnifiedSwerveStack(hardwareMap, logger);
        stack.restorePoseFromStorage();

        FollowerConstants followerConstants = createFollowerConstants();

        Follower follower = new FollowerBuilder(followerConstants, hardwareMap)
                .setLocalizer(new PedroLocalizerAdapter(stack))
                .setDrivetrain(new PedroDrivetrainAdapter(stack))
                .build();
        applyLiveTuning(follower);

        return new PedroRobot(stack, follower);
    }

    public static Follower createFollower(HardwareMap hardwareMap) {
        return createRobot(hardwareMap, null).follower;
    }

    public static final class PedroRobot {
        public final PedroUnifiedSwerveStack stack;
        public final Follower follower;

        private PedroRobot(PedroUnifiedSwerveStack stack, Follower follower) {
            this.stack = stack;
            this.follower = follower;
        }
    }

    private static void applyToFollowerConstants(FollowerConstants followerConstants) {
        followerConstants.mass(SwerveConfig.PEDRO_MASS)
                .forwardZeroPowerAcceleration(SwerveConfig.PEDRO_FORWARD_ZERO_POWER_ACCELERATION)
                .lateralZeroPowerAcceleration(SwerveConfig.PEDRO_LATERAL_ZERO_POWER_ACCELERATION)
                .translationalPIDFCoefficients(primaryTranslationalPid())
                .secondaryTranslationalPIDFCoefficients(secondaryTranslationalPid())
                .headingPIDFCoefficients(primaryHeadingPid())
                .secondaryHeadingPIDFCoefficients(secondaryHeadingPid())
                .drivePIDFCoefficients(primaryDrivePid())
                .secondaryDrivePIDFCoefficients(secondaryDrivePid())
                .translationalPIDFSwitch(SwerveConfig.PEDRO_TRANSLATIONAL_PID_SWITCH)
                .headingPIDFSwitch(SwerveConfig.PEDRO_HEADING_PID_SWITCH_RAD)
                .drivePIDFSwitch(SwerveConfig.PEDRO_DRIVE_PID_SWITCH)
                .turnHeadingErrorThreshold(SwerveConfig.PEDRO_TURN_HEADING_ERROR_THRESHOLD_RAD)
                .centripetalScaling(SwerveConfig.PEDRO_CENTRIPETAL_SCALING)
                .automaticHoldEnd(SwerveConfig.PEDRO_AUTOMATIC_HOLD_END)
                .holdPointTranslationalScaling(SwerveConfig.PEDRO_HOLD_POINT_TRANSLATIONAL_SCALING)
                .holdPointHeadingScaling(SwerveConfig.PEDRO_HOLD_POINT_HEADING_SCALING)
                .BEZIER_CURVE_SEARCH_LIMIT(SwerveConfig.PEDRO_BEZIER_CURVE_SEARCH_LIMIT)
                .useSecondaryTranslationalPIDF(SwerveConfig.PEDRO_USE_SECONDARY_TRANSLATIONAL_PID)
                .useSecondaryHeadingPIDF(SwerveConfig.PEDRO_USE_SECONDARY_HEADING_PID)
                .useSecondaryDrivePIDF(SwerveConfig.PEDRO_USE_SECONDARY_DRIVE_PID)
                .predictiveBrakingCoefficients(predictiveBraking())
                .driveKalmanFilterModelCovariance(SwerveConfig.PEDRO_DRIVE_KALMAN_MODEL_COVARIANCE)
                .driveKalmanFilterDataCovariance(SwerveConfig.PEDRO_DRIVE_KALMAN_DATA_COVARIANCE)
                .stuckVelocity(SwerveConfig.PEDRO_STUCK_VELOCITY)
                .stuckTValue(SwerveConfig.PEDRO_STUCK_T_VALUE)
                .stuckTimeout(SwerveConfig.PEDRO_STUCK_TIMEOUT);
        followerConstants.usePredictiveBraking = SwerveConfig.PEDRO_USE_PREDICTIVE_BRAKING;
    }

    private static PIDFCoefficients primaryTranslationalPid() {
        return new PIDFCoefficients(
                SwerveConfig.PEDRO_TRANSLATIONAL_P,
                SwerveConfig.PEDRO_TRANSLATIONAL_I,
                SwerveConfig.PEDRO_TRANSLATIONAL_D,
                SwerveConfig.PEDRO_TRANSLATIONAL_F);
    }

    private static PIDFCoefficients secondaryTranslationalPid() {
        return new PIDFCoefficients(
                SwerveConfig.PEDRO_SECONDARY_TRANSLATIONAL_P,
                SwerveConfig.PEDRO_SECONDARY_TRANSLATIONAL_I,
                SwerveConfig.PEDRO_SECONDARY_TRANSLATIONAL_D,
                SwerveConfig.PEDRO_SECONDARY_TRANSLATIONAL_F);
    }

    private static PIDFCoefficients primaryHeadingPid() {
        return new PIDFCoefficients(
                SwerveConfig.PEDRO_HEADING_P,
                SwerveConfig.PEDRO_HEADING_I,
                SwerveConfig.PEDRO_HEADING_D,
                SwerveConfig.PEDRO_HEADING_F);
    }

    private static PIDFCoefficients secondaryHeadingPid() {
        return new PIDFCoefficients(
                SwerveConfig.PEDRO_SECONDARY_HEADING_P,
                SwerveConfig.PEDRO_SECONDARY_HEADING_I,
                SwerveConfig.PEDRO_SECONDARY_HEADING_D,
                SwerveConfig.PEDRO_SECONDARY_HEADING_F);
    }

    private static FilteredPIDFCoefficients primaryDrivePid() {
        return new FilteredPIDFCoefficients(
                SwerveConfig.PEDRO_DRIVE_P,
                SwerveConfig.PEDRO_DRIVE_I,
                SwerveConfig.PEDRO_DRIVE_D,
                SwerveConfig.PEDRO_DRIVE_F,
                SwerveConfig.PEDRO_DRIVE_T);
    }

    private static FilteredPIDFCoefficients secondaryDrivePid() {
        return new FilteredPIDFCoefficients(
                SwerveConfig.PEDRO_SECONDARY_DRIVE_P,
                SwerveConfig.PEDRO_SECONDARY_DRIVE_I,
                SwerveConfig.PEDRO_SECONDARY_DRIVE_D,
                SwerveConfig.PEDRO_SECONDARY_DRIVE_F,
                SwerveConfig.PEDRO_SECONDARY_DRIVE_T);
    }

    private static PredictiveBrakingCoefficients predictiveBraking() {
        return new PredictiveBrakingCoefficients(
                SwerveConfig.PEDRO_BRAKING_LINEAR,
                SwerveConfig.PEDRO_BRAKING_QUADRATIC_FRICTION,
                SwerveConfig.PEDRO_BRAKING_P)
                .withMaximumBrakingPower(SwerveConfig.PEDRO_BRAKING_MAX_POWER);
    }
}
