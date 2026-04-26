package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.geometry.Pose;

/**
 * Common DECODE start poses in field coordinates.
 */
public enum PedroStartPose {
    RED_BASE_CORNER(-60.0, -60.0, 0.0),
    RED_BASE_CENTER(-48.0, -60.0, 0.0),
    BLUE_BASE_CORNER(-60.0, 60.0, 0.0),
    BLUE_BASE_CENTER(-48.0, 60.0, 0.0);

    public final double xIn;
    public final double yIn;
    public final double headingDeg;

    PedroStartPose(double xIn, double yIn, double headingDeg) {
        this.xIn = xIn;
        this.yIn = yIn;
        this.headingDeg = headingDeg;
    }

    public Pose toPose() {
        return new Pose(xIn, yIn, Math.toRadians(headingDeg));
    }

    public static Pose custom(double xIn, double yIn, double headingDeg) {
        return new Pose(xIn, yIn, Math.toRadians(headingDeg));
    }

    public static PedroStartPose fromName(String name, PedroStartPose fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }

        for (PedroStartPose pose : values()) {
            if (pose.name().equalsIgnoreCase(name.trim())) {
                return pose;
            }
        }

        return fallback;
    }
}
