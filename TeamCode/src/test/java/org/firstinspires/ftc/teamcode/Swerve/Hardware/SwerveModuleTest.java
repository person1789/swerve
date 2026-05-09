package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveModuleTest {

    private final double originalTicksPerRev = SwerveConfig.DRIVE_TICKS_PER_REV;
    private final double originalGearRatio = SwerveConfig.DRIVE_GEAR_RATIO;
    private final double originalWheelRadius = SwerveConfig.WHEEL_RADIUS_METERS;
    private final double originalFlipThreshold = SwerveConfig.FLIP_THRESHOLD;
    private final double originalFullAuthority = SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD;
    private final double originalHardCutoff = SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD;
    private final double originalMinAuthority = SwerveConfig.STEER_DRIVE_MIN_AUTHORITY;
    private final double originalMinCosineFactor = SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.DRIVE_TICKS_PER_REV = originalTicksPerRev;
        SwerveConfig.DRIVE_GEAR_RATIO = originalGearRatio;
        SwerveConfig.WHEEL_RADIUS_METERS = originalWheelRadius;
        SwerveConfig.FLIP_THRESHOLD = originalFlipThreshold;
        SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD = originalFullAuthority;
        SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD = originalHardCutoff;
        SwerveConfig.STEER_DRIVE_MIN_AUTHORITY = originalMinAuthority;
        SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR = originalMinCosineFactor;
    }

    @Test
    void driveTickConversionUsesConfiguredWheelRadius() {
        SwerveConfig.DRIVE_TICKS_PER_REV = 28.0;
        SwerveConfig.DRIVE_GEAR_RATIO = 1.0;
        SwerveConfig.WHEEL_RADIUS_METERS = 0.0245;

        double metersPerSecond = SwerveModule.driveTicksPerSecondToMetersPerSecond(28.0);

        assertEquals(2.0 * Math.PI * 0.0245, metersPerSecond, 1e-9);
    }

    @Test
    void moduleFlipsLargeSteeringErrorsInsteadOfTakingLongWayAround() {
        // Passes if a near-180-degree request becomes negative drive power at the short steering angle.
        TestModuleIO io = new TestModuleIO();
        SwerveModule module = new SwerveModule(io, 0.0, false, null);

        module.update(new SwerveModuleState(1.0, Math.PI), 0.02);

        assertEquals(0.0, module.getLastTargetAngleRad(), 1e-9);
        assertTrue(module.getLastTargetVelocityMps() < 0.0);
        assertTrue(io.drivePower < 0.0);
    }

    @Test
    void moduleReducesDriveAuthorityWhenSteeringErrorIsLarge() {
        // Passes if a large steering error keeps some drive output but derates it below full authority.
        SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD = Math.toRadians(5.0);
        SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD = Math.toRadians(35.0);
        SwerveConfig.STEER_DRIVE_MIN_AUTHORITY = 0.12;
        SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR = 0.35;

        TestModuleIO io = new TestModuleIO();
        io.currentRotation = Math.toRadians(25.0);
        SwerveModule module = new SwerveModule(io, 0.0, false, null);

        module.update(new SwerveModuleState(SwerveConfig.getMaxLinearSpeedMPS(), 0.0), 0.02);

        assertTrue(Math.abs(io.drivePower) > 0.0);
        assertTrue(Math.abs(io.drivePower) < 0.5);
    }

    private static class TestModuleIO implements SwerveModuleIO {
        double currentRotation;
        double drivePower;

        @Override
        public double getCurrentRotationRadians() {
            return currentRotation;
        }

        @Override
        public double getDriveVelocityMetersPerSecond() {
            return 0.0;
        }

        @Override
        public double getDriveCurrentAmps() {
            return 0.0;
        }

        @Override
        public void setDrivePower(double power) {
            drivePower = power;
        }

        @Override
        public void setSteerPower(double power) {
        }

        @Override
        public void setDriveMode(DcMotor.RunMode mode) {
        }
    }
}
