package org.firstinspires.ftc.teamcode.Swerve.Geometry;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;

/**
 * Thin pose wrapper for APIs that benefit from named fields.
 */
public class Pose {
    public final double x;
    public final double y;
    public final double heading;

    public Pose(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public Vector toVector() {
        return new Vector(x, y, heading);
    }

    public static Pose from(Vector vector) {
        return new Pose(vector.x(), vector.y(), vector.omega());
    }

    public Pose addPose(Pose other) {
        return new Pose(
                x + other.x,
                y + other.y,
                MathUtil.normalizeAngle(heading + other.heading));
    }
}
