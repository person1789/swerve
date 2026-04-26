package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.pedropathing.paths.PathBuilder;

/**
 * Builds a Pedro PathChain from simple route blocks.
 */
public final class PedroBlockRouteBuilder {
    private PedroBlockRouteBuilder() {
    }

    public static PathChain build(Follower follower, Pose startPose, PedroBlockCommand... commands) {
        PathBuilder builder = follower.pathBuilder();
        Pose current = startPose;

        for (PedroBlockCommand command : commands) {
            Pose end = command.endPose();
            Path path;
            if (command.curved) {
                Pose control = scaledControlPose(current, end, command);
                path = new Path(new BezierCurve(current, control, end));
            } else {
                path = new Path(new BezierLine(current, end));
            }

            builder.addPath(path);

            if (Math.abs(current.getHeading() - end.getHeading()) < 1e-9) {
                builder.setConstantHeadingInterpolation(current.getHeading());
            } else {
                builder.setLinearHeadingInterpolation(
                        current.getHeading(),
                        end.getHeading(),
                        command.headingInterpolationWeight);
            }

            current = end;
        }

        return builder.build();
    }

    private static Pose scaledControlPose(Pose start, Pose end, PedroBlockCommand command) {
        double baseX = command.controlXIn != null ? command.controlXIn : midpoint(start.getX(), end.getX());
        double baseY = command.controlYIn != null ? command.controlYIn : midpoint(start.getY(), end.getY());
        double baseHeadingDeg = command.controlHeadingDeg != null
                ? command.controlHeadingDeg
                : Math.toDegrees(start.getHeading());

        double scaledX = start.getX() + (baseX - start.getX()) * command.controlScale;
        double scaledY = start.getY() + (baseY - start.getY()) * command.controlScale;
        return new Pose(scaledX, scaledY, Math.toRadians(baseHeadingDeg));
    }

    private static double midpoint(double a, double b) {
        return (a + b) * 0.5;
    }
}
