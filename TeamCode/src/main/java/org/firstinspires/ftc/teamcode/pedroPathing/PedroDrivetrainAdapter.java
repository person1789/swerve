package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.drivetrain.CustomDrivetrain;

/**
 * Pedro drivetrain adapter backed by the team's custom swerve pipeline.
 */
public class PedroDrivetrainAdapter extends CustomDrivetrain {
    private final PedroUnifiedSwerveStack stack;
    private double xVelocity = 0.0;
    private double yVelocity = 0.0;

    public PedroDrivetrainAdapter(PedroUnifiedSwerveStack stack) {
        this.stack = stack;
    }

    @Override
    public void arcadeDrive(double forward, double strafe, double rotation) {
        xVelocity = forward;
        yVelocity = strafe;
        stack.drivePedroRobotCentric(forward, strafe, rotation);
    }

    @Override
    public void updateConstants() {
        // The underlying drivetrain is configured through SwerveConfig.
    }

    @Override
    public void breakFollowing() {
        xVelocity = 0.0;
        yVelocity = 0.0;
        stack.stopPedroDrive();
    }

    @Override
    public void runDrive(double[] drivePowers) {
        if (drivePowers == null || drivePowers.length < 3) {
            breakFollowing();
            return;
        }
        arcadeDrive(drivePowers[0], drivePowers[1], drivePowers[2]);
    }

    @Override
    public void startTeleopDrive() {
        breakFollowing();
    }

    @Override
    public void startTeleopDrive(boolean brakeMode) {
        breakFollowing();
    }

    @Override
    public double xVelocity() {
        return xVelocity;
    }

    @Override
    public double yVelocity() {
        return yVelocity;
    }

    @Override
    public void setXVelocity(double xVelocity) {
        this.xVelocity = xVelocity;
    }

    @Override
    public void setYVelocity(double yVelocity) {
        this.yVelocity = yVelocity;
    }

    @Override
    public double getVoltage() {
        return stack.getDrivetrain().getBatteryVoltage();
    }

    @Override
    public String debugString() {
        return String.format(
                "state=%s authority=%.3f steerReady=%s dtMs=%.2f",
                stack.getDrivetrain().getState(),
                stack.getDrivetrain().getLastTranslationAuthority(),
                stack.getDrivetrain().isSteerReadyForDrive(),
                stack.getLastDtSec() * 1000.0);
    }
}
