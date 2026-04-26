package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.pedropathing.localization.Localizer;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;

class SimPedroLocalizer implements Localizer {
    private final SwerveSimulator sim;
    private double totalHeading;
    private double previousHeading;

    SimPedroLocalizer(SwerveSimulator sim) {
        this.sim = sim;
        this.previousHeading = sim.getPoseInches().heading;
        this.totalHeading = previousHeading;
    }

    @Override
    public com.pedropathing.geometry.Pose getPose() {
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose pose = sim.getPoseInches();
        return new com.pedropathing.geometry.Pose(pose.x, pose.y, pose.heading);
    }

    @Override
    public com.pedropathing.geometry.Pose getVelocity() {
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose velocity = sim.getVelocityInches();
        return new com.pedropathing.geometry.Pose(velocity.x, velocity.y, velocity.heading);
    }

    @Override
    public com.pedropathing.math.Vector getVelocityVector() {
        return getVelocity().getAsVector();
    }

    @Override
    public void setStartPose(com.pedropathing.geometry.Pose setStart) {
        setPose(setStart);
    }

    @Override
    public void setPose(com.pedropathing.geometry.Pose setPose) {
        sim.setPoseInches(new org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose(
                setPose.getX(),
                setPose.getY(),
                setPose.getHeading()));
        previousHeading = setPose.getHeading();
        totalHeading = setPose.getHeading();
    }

    @Override
    public void update() {
        double heading = getPose().getHeading();
        totalHeading += MathUtil.angleError(previousHeading, heading);
        previousHeading = heading;
    }

    @Override
    public double getTotalHeading() {
        return totalHeading;
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
        com.pedropathing.geometry.Pose pose = getPose();
        setPose(new com.pedropathing.geometry.Pose(pose.getX(), pose.getY(), 0.0));
    }

    @Override
    public double getIMUHeading() {
        return getPose().getHeading();
    }

    @Override
    public boolean isNAN() {
        com.pedropathing.geometry.Pose pose = getPose();
        return !Double.isFinite(pose.getX()) || !Double.isFinite(pose.getY()) || !Double.isFinite(pose.getHeading());
    }
}
