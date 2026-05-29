package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveModuleTest {

    private final double originalTicksPerRev = SwerveConfig.DRIVE_TICKS_PER_REV;
    private final double originalGearRatio = SwerveConfig.DRIVE_GEAR_RATIO;
    private final double originalWheelRadius = SwerveConfig.WHEEL_RADIUS_METERS;
    private final double originalFlipThreshold = SwerveConfig.FLIP_THRESHOLD;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.DRIVE_TICKS_PER_REV = originalTicksPerRev;
        SwerveConfig.DRIVE_GEAR_RATIO = originalGearRatio;
        SwerveConfig.WHEEL_RADIUS_METERS = originalWheelRadius;
        SwerveConfig.FLIP_THRESHOLD = originalFlipThreshold;
    }

    @Test
    void driveTickConversionUsesConfiguredWheelRadius() {
        // Passes if one wheel rev per second converts to wheel circumference per second.
        SwerveConfig.DRIVE_TICKS_PER_REV = 28.0;
        SwerveConfig.DRIVE_GEAR_RATIO = 1.0;
        SwerveConfig.WHEEL_RADIUS_METERS = 0.0245;

        double metersPerSecond = SwerveModule.driveTicksPerSecondToMetersPerSecond(28.0);

        assertEquals(2.0 * Math.PI * 0.0245, metersPerSecond, 1e-9);
    }

    @Test
    void moduleFlipsLargeSteeringErrorsInsteadOfTakingLongWayAround() {
        // Passes if a 180-degree target keeps the module angle near zero and reverses drive power.
        TestModuleHardware hardware = new TestModuleHardware();
        SwerveModule module = new SwerveModule(hardware, 0.0, false);

        module.update(new SwerveModuleState(SwerveConfig.getMaxLinearSpeedMPS(), Math.PI), 0.02);

        assertEquals(0.0, module.getLastTargetAngleRadians(), 1e-9);
        assertTrue(module.getLastDrivePower() < 0.0);
        assertTrue(hardware.drivePower < 0.0);
    }

    @Test
    void readAppliesOffsetAndInversion() {
        // Passes if cached rotation reflects encoder offset and inversion after a single read.
        TestModuleHardware hardware = new TestModuleHardware();
        hardware.rotationRadians = Math.PI / 2.0;
        SwerveModule module = new SwerveModule(hardware, Math.PI / 4.0, true);

        module.read();

        assertEquals(-Math.PI / 4.0, module.getCurrentRotation(), 1e-9);
    }

    private static class TestModuleHardware implements SwerveModule.HardwareAdapter {
        private double rotationRadians;
        private double drivePower;

        @Override
        public void read() {
        }

        @Override
        public double getRotationRadians() {
            return rotationRadians;
        }

        @Override
        public double getDriveVelocityTicksPerSecond() {
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
