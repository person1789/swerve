package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;

/**
 * Central Pedro factory for the current robot configuration.
 */
public final class PedroSwerveFactory {
    private PedroSwerveFactory() {
    }

    public static FollowerConstants createFollowerConstants() {
        FollowerConstants followerConstants = new FollowerConstants()
                .mass(10.65)
                .forwardZeroPowerAcceleration(-40.0)
                .lateralZeroPowerAcceleration(-40.0);
        followerConstants.usePredictiveBraking = false;
        return followerConstants;
    }

    public static PedroRobot createRobot(HardwareMap hardwareMap, Logger logger) {
        PedroUnifiedSwerveStack stack = new PedroUnifiedSwerveStack(hardwareMap, logger);
        stack.restorePoseFromStorage();

        FollowerConstants followerConstants = createFollowerConstants();

        Follower follower = new FollowerBuilder(followerConstants, hardwareMap)
                .setLocalizer(new PedroLocalizerAdapter(stack))
                .setDrivetrain(new PedroDrivetrainAdapter(stack))
                .build();

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
}
