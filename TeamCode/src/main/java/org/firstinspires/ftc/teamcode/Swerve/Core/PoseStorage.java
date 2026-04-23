package org.firstinspires.ftc.teamcode.Swerve.Core;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

public final class PoseStorage {
    private static Pose currentPose = null;

    private PoseStorage() {
    }

    public static void setCurrentPose(Pose pose) {
        currentPose = pose;
    }

    public static void setCurrentPose(double xInches, double yInches, double headingRadians) {
        currentPose = new Pose(xInches, yInches, headingRadians);
    }

    public static void setFromPedroPose(com.pedropathing.geometry.Pose pose) {
        if (pose == null) {
            currentPose = null;
            return;
        }
        currentPose = new Pose(pose.getX(), pose.getY(), pose.getHeading());
    }

    public static Pose getCurrentPose() {
        return currentPose;
    }

    public static void clear() {
        currentPose = null;
    }
}
