package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.geometry.Pose;
import com.pedropathing.localization.Localizer;

/**
 * Pedro Localizer backed by the team's fail-safe SwerveLocalizer.
 */
public class PedroLocalizerAdapter implements Localizer {
    private final PedroUnifiedSwerveStack stack;

    public PedroLocalizerAdapter(PedroUnifiedSwerveStack stack) {
        this.stack = stack;
    }

    @Override
    public Pose getPose() {
        return stack.getPedroPose();
    }

    @Override
    public Pose getVelocity() {
        return stack.getPedroVelocityPose();
    }

    @Override
    public com.pedropathing.math.Vector getVelocityVector() {
        return stack.getPedroVelocityVector();
    }

    @Override
    public void setStartPose(Pose setStart) {
        stack.setPedroPose(setStart);
    }

    @Override
    public void setPose(Pose setPose) {
        stack.setPedroPose(setPose);
    }

    @Override
    public void update() {
        stack.advancePedroLoop();
    }

    @Override
    public double getTotalHeading() {
        return stack.getTotalHeadingRad();
    }

    @Override
    public double getForwardMultiplier() {
        return 1.0;
    }

    @Override
    public double getLateralMultiplier() {
        return 1.0;
    }

    @Override
    public double getTurningMultiplier() {
        return 1.0;
    }

    @Override
    public void resetIMU() throws InterruptedException {
        stack.resetHeadingForTeleOp();
    }

    @Override
    public double getIMUHeading() {
        return stack.getLocalizer().getHeading();
    }

    @Override
    public boolean isNAN() {
        return !stack.isPoseFinite();
    }
}
