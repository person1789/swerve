package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.pedropathing.drivetrain.CustomDrivetrain;

class SimPedroDrivetrain extends CustomDrivetrain {
    private final SwerveSimulator sim;
    private double xVelocity;
    private double yVelocity;

    SimPedroDrivetrain(SwerveSimulator sim) {
        this.sim = sim;
        this.maxPowerScaling = 1.0;
        this.nominalVoltage = 12.0;
    }

    @Override
    public void arcadeDrive(double forward, double strafe, double rotation) {
        xVelocity = forward;
        yVelocity = strafe;
        sim.setDirectDriveCommand(forward, strafe, rotation);
    }

    @Override
    public void updateConstants() {
    }

    @Override
    public void breakFollowing() {
        xVelocity = 0.0;
        yVelocity = 0.0;
        sim.clearDirectDriveCommand();
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
    public void setXVelocity(double xMovement) {
        xVelocity = xMovement;
    }

    @Override
    public void setYVelocity(double yMovement) {
        yVelocity = yMovement;
    }

    @Override
    public double getVoltage() {
        return 12.0;
    }

    @Override
    public String debugString() {
        return String.format("simPedro x=%.3f y=%.3f", xVelocity, yVelocity);
    }
}
