package org.firstinspires.ftc.teamcode.Swerve.Sim;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModuleIO;

class MockSwerveModuleIO implements SwerveModuleIO {
    private static final double MAX_STEER_RATE_RAD_PER_SEC = Math.PI * 1.6;
    private static final double DRIVE_RESPONSE_GAIN = 10.0;

    private final String name;
    private final double moduleX;
    private final double moduleY;

    private double drivePower;
    private double steerPower;
    private double currentRotationRadians;
    private double driveVelocityMetersPerSecond;
    private double driveCurrentAmps;

    MockSwerveModuleIO(String name, double moduleX, double moduleY) {
        this.name = name;
        this.moduleX = moduleX;
        this.moduleY = moduleY;
    }

    @Override
    public double getCurrentRotationRadians() {
        return currentRotationRadians;
    }

    @Override
    public double getDriveVelocityMetersPerSecond() {
        return driveVelocityMetersPerSecond;
    }

    @Override
    public double getDriveCurrentAmps() {
        return driveCurrentAmps;
    }

    @Override
    public void setDrivePower(double power) {
        this.drivePower = MathUtil.clampPower(power);
    }

    @Override
    public void setSteerPower(double power) {
        this.steerPower = MathUtil.clampPower(power);
    }

    void step(double dtSeconds) {
        currentRotationRadians = MathUtil.normalizeAngle(
                currentRotationRadians + steerPower * MAX_STEER_RATE_RAD_PER_SEC * dtSeconds);

        double targetVelocity = drivePower * SwerveConfig.getMaxLinearSpeedMPS();
        double blend = MathUtil.clamp(DRIVE_RESPONSE_GAIN * dtSeconds, 0.0, 1.0);
        driveVelocityMetersPerSecond += (targetVelocity - driveVelocityMetersPerSecond) * blend;
        driveCurrentAmps = Math.abs(drivePower) * 6.0
                + Math.abs(targetVelocity - driveVelocityMetersPerSecond) * 2.0;
    }

    String getName() {
        return name;
    }

    double getModuleX() {
        return moduleX;
    }

    double getModuleY() {
        return moduleY;
    }

    double getDrivePower() {
        return drivePower;
    }

    double getSteerPower() {
        return steerPower;
    }
}
