package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.ftc.drivetrains.SwervePod;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;

/**
 * Thin adapter from the team's existing SwerveModule implementation to Pedro's SwervePod API.
 */
public class PedroSwervePod implements SwervePod {
    private static final double FIRST_LOOP_DT_SEC = 0.020;

    private final SwerveModule module;
    private final DcMotorEx driveMotor;
    private final com.pedropathing.geometry.Pose offset;
    private final ElapsedTime loopTimer = new ElapsedTime();

    private boolean firstMove = true;

    public PedroSwervePod(SwerveModule module, DcMotorEx driveMotor, com.pedropathing.geometry.Pose offset) {
        this.module = module;
        this.driveMotor = driveMotor;
        this.offset = offset;
        this.driveMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    @Override
    public com.pedropathing.geometry.Pose getOffset() {
        return offset;
    }

    @Override
    public double getAngle() {
        return Math.toDegrees(module.getCurrentRotation());
    }

    @Override
    public double adjustThetaForEncoder(double wheelTheta) {
        return MathUtil.normalizeAngle(wheelTheta);
    }

    @Override
    public void move(double targetAngleRad, double drivePower, boolean ignoreAngleChanges) {
        double dt = firstMove ? FIRST_LOOP_DT_SEC : Math.max(loopTimer.seconds(), 1e-3);
        loopTimer.reset();
        firstMove = false;

        double commandedAngle = ignoreAngleChanges ? module.getCurrentRotation() : targetAngleRad;
        double driveSpeedMps = drivePower * SwerveConfig.getMaxLinearSpeedMPS();
        module.update(commandedAngle, driveSpeedMps, dt);
    }

    @Override
    public void setToFloat() {
        driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    @Override
    public void setToBreak() {
        driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    @Override
    public String debugString() {
        return String.format(
                "angleDeg=%.2f targetDeg=%.2f drivePower=%.3f velMps=%.3f",
                getAngle(),
                Math.toDegrees(module.getLastTargetAngleRad()),
                module.getLastDrivePower(),
                module.getVelocityMps());
    }
}
