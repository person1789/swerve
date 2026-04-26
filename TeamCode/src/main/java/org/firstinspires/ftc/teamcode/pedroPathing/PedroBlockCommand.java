package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.geometry.Pose;

/**
 * One autonomous route block: move to a field endpoint with a target heading,
 * either by a straight line or a curved segment.
 */
public final class PedroBlockCommand {
    public final double endXIn;
    public final double endYIn;
    public final double endHeadingDeg;
    public final boolean curved;
    public final double controlScale;
    public final double headingInterpolationWeight;
    public final Double controlXIn;
    public final Double controlYIn;
    public final Double controlHeadingDeg;

    private PedroBlockCommand(
            double endXIn,
            double endYIn,
            double endHeadingDeg,
            boolean curved,
            double controlScale,
            double headingInterpolationWeight,
            Double controlXIn,
            Double controlYIn,
            Double controlHeadingDeg) {
        this.endXIn = endXIn;
        this.endYIn = endYIn;
        this.endHeadingDeg = endHeadingDeg;
        this.curved = curved;
        this.controlScale = controlScale;
        this.headingInterpolationWeight = headingInterpolationWeight;
        this.controlXIn = controlXIn;
        this.controlYIn = controlYIn;
        this.controlHeadingDeg = controlHeadingDeg;
    }

    public Pose endPose() {
        return new Pose(endXIn, endYIn, Math.toRadians(endHeadingDeg));
    }

    public static PedroBlockCommand straight(
            double endXIn,
            double endYIn,
            double endHeadingDeg,
            double headingInterpolationWeight) {
        return new PedroBlockCommand(
                endXIn,
                endYIn,
                endHeadingDeg,
                false,
                1.0,
                headingInterpolationWeight,
                null,
                null,
                null);
    }

    public static PedroBlockCommand curved(
            double endXIn,
            double endYIn,
            double endHeadingDeg,
            double controlXIn,
            double controlYIn,
            double controlHeadingDeg,
            double controlScale,
            double headingInterpolationWeight) {
        return new PedroBlockCommand(
                endXIn,
                endYIn,
                endHeadingDeg,
                true,
                controlScale,
                headingInterpolationWeight,
                controlXIn,
                controlYIn,
                controlHeadingDeg);
    }
}
