package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/**
 * Shared DECODE lane route used by robot auto and simulator.
 */
@Config
public final class PedroDecodeRoute {
    public static double laneExitXIn = -30.0;
    public static double laneExitYIn = -60.0;

    public static double goalApproachControlXIn = -8.0;
    public static double goalApproachControlYIn = -46.0;
    public static double goalApproachXIn = 18.0;
    public static double goalApproachYIn = -28.0;
    public static double goalApproachHeadingDeg = 32.0;

    public static double classifierBypassXIn = 28.0;
    public static double classifierBypassYIn = -6.0;
    public static double classifierBypassHeadingDeg = 90.0;

    public static double returnControlXIn = -10.0;
    public static double returnControlYIn = -8.0;
    public static double baseReturnXIn = -52.0;
    public static double baseReturnYIn = -44.0;
    public static double baseReturnHeadingDeg = 180.0;

    private PedroDecodeRoute() {
    }

    public static Pose laneExit(Pose startPose) {
        return new Pose(laneExitXIn, laneExitYIn, startPose.getHeading());
    }

    public static Pose goalApproach() {
        return new Pose(goalApproachXIn, goalApproachYIn, Math.toRadians(goalApproachHeadingDeg));
    }

    public static Pose classifierBypass() {
        return new Pose(classifierBypassXIn, classifierBypassYIn, Math.toRadians(classifierBypassHeadingDeg));
    }

    public static Pose baseReturn() {
        return new Pose(baseReturnXIn, baseReturnYIn, Math.toRadians(baseReturnHeadingDeg));
    }

    public static PathChain buildLaneAuto(Follower follower, Pose startPose) {
        return PedroBlockRouteBuilder.build(
                follower,
                startPose,
                PedroBlockCommand.straight(
                        laneExitXIn,
                        laneExitYIn,
                        Math.toDegrees(startPose.getHeading()),
                        1.0),
                PedroBlockCommand.curved(
                        goalApproachXIn,
                        goalApproachYIn,
                        goalApproachHeadingDeg,
                        goalApproachControlXIn,
                        goalApproachControlYIn,
                        Math.toDegrees(startPose.getHeading()),
                        1.0,
                        0.8),
                PedroBlockCommand.straight(
                        classifierBypassXIn,
                        classifierBypassYIn,
                        classifierBypassHeadingDeg,
                        0.9),
                PedroBlockCommand.curved(
                        baseReturnXIn,
                        baseReturnYIn,
                        baseReturnHeadingDeg,
                        returnControlXIn,
                        returnControlYIn,
                        135.0,
                        1.0,
                        0.9));
    }
}
