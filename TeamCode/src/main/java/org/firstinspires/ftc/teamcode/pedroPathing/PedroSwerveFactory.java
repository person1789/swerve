package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.control.PredictiveBrakingCoefficients;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroFollowerModelTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroFollowerSafetyTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPathControlTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPredictiveBrakingTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryDriveTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryHeadingTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryTranslationTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryDriveTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryHeadingTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryTranslationTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.FilteredPidfSnapshot;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.ModelSnapshot;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.PathControlSnapshot;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.PidfSnapshot;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.PredictiveBrakingSnapshot;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroTuneSnapshots.SafetySnapshot;

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
        PidfSnapshot primaryTranslation = PedroPrimaryTranslationTuning.snapshot();
        PidfSnapshot primaryHeading = PedroPrimaryHeadingTuning.snapshot();
        FilteredPidfSnapshot primaryDrive = PedroPrimaryDriveTuning.snapshot();
        PathControlSnapshot pathControl = PedroPathControlTuning.snapshot();
        PredictiveBrakingSnapshot braking = PedroPredictiveBrakingTuning.snapshot();

        follower.setCentripetalScaling(pathControl.centripetalScaling);
        follower.useTranslational = primaryTranslation.enabled;
        follower.useHeading = primaryHeading.enabled;
        follower.useDrive = primaryDrive.enabled;
        follower.usePredictiveBraking = braking.enabled;
        follower.useCentripetal = pathControl.centripetalScaling != 0.0;
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
        ModelSnapshot model = PedroFollowerModelTuning.snapshot();
        PathControlSnapshot pathControl = PedroPathControlTuning.snapshot();
        SafetySnapshot safety = PedroFollowerSafetyTuning.snapshot();
        PredictiveBrakingSnapshot braking = PedroPredictiveBrakingTuning.snapshot();
        followerConstants.mass(model.mass)
                .forwardZeroPowerAcceleration(model.forwardZeroPowerAcceleration)
                .lateralZeroPowerAcceleration(model.lateralZeroPowerAcceleration)
                .translationalPIDFCoefficients(primaryTranslationalPid())
                .secondaryTranslationalPIDFCoefficients(secondaryTranslationalPid())
                .headingPIDFCoefficients(primaryHeadingPid())
                .secondaryHeadingPIDFCoefficients(secondaryHeadingPid())
                .drivePIDFCoefficients(primaryDrivePid())
                .secondaryDrivePIDFCoefficients(secondaryDrivePid())
                .translationalPIDFSwitch(PedroPrimaryTranslationTuning.snapshot().switchThreshold)
                .headingPIDFSwitch(PedroPrimaryHeadingTuning.snapshot().switchThreshold)
                .drivePIDFSwitch(PedroPrimaryDriveTuning.snapshot().switchThreshold)
                .turnHeadingErrorThreshold(pathControl.turnHeadingErrorThresholdRad)
                .centripetalScaling(pathControl.centripetalScaling)
                .automaticHoldEnd(pathControl.automaticHoldEnd)
                .holdPointTranslationalScaling(pathControl.holdPointTranslationalScaling)
                .holdPointHeadingScaling(pathControl.holdPointHeadingScaling)
                .BEZIER_CURVE_SEARCH_LIMIT(pathControl.bezierCurveSearchLimit)
                .useSecondaryTranslationalPIDF(PedroSecondaryTranslationTuning.snapshot().enabled)
                .useSecondaryHeadingPIDF(PedroSecondaryHeadingTuning.snapshot().enabled)
                .useSecondaryDrivePIDF(PedroSecondaryDriveTuning.snapshot().enabled)
                .predictiveBrakingCoefficients(predictiveBraking())
                .driveKalmanFilterModelCovariance(safety.driveKalmanModelCovariance)
                .driveKalmanFilterDataCovariance(safety.driveKalmanDataCovariance)
                .stuckVelocity(safety.stuckVelocity)
                .stuckTValue(safety.stuckTValue)
                .stuckTimeout(safety.stuckTimeout);
        followerConstants.usePredictiveBraking = braking.enabled;
    }

    private static PIDFCoefficients primaryTranslationalPid() {
        return PedroPrimaryTranslationTuning.snapshot().toPidf();
    }

    private static PIDFCoefficients secondaryTranslationalPid() {
        return PedroSecondaryTranslationTuning.snapshot().toPidf();
    }

    private static PIDFCoefficients primaryHeadingPid() {
        return PedroPrimaryHeadingTuning.snapshot().toPidf();
    }

    private static PIDFCoefficients secondaryHeadingPid() {
        return PedroSecondaryHeadingTuning.snapshot().toPidf();
    }

    private static FilteredPIDFCoefficients primaryDrivePid() {
        return PedroPrimaryDriveTuning.snapshot().toPidf();
    }

    private static FilteredPIDFCoefficients secondaryDrivePid() {
        return PedroSecondaryDriveTuning.snapshot().toPidf();
    }

    private static PredictiveBrakingCoefficients predictiveBraking() {
        return PedroPredictiveBrakingTuning.snapshot().toCoefficients();
    }
}
