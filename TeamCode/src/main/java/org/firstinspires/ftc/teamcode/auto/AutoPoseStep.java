package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

/**
 * One Kooky-style autonomous target pose with timeout and settle requirements.
 */
public class AutoPoseStep {
    public final Pose targetPose;
    public final double settleTimeSec;
    public final double timeoutSec;

    public AutoPoseStep(Pose targetPose, double settleTimeSec, double timeoutSec) {
        this.targetPose = targetPose;
        this.settleTimeSec = settleTimeSec;
        this.timeoutSec = timeoutSec;
    }
}
