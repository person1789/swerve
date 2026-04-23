package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveModuleTest {

    private final double originalTicksPerRev = SwerveConfig.DRIVE_TICKS_PER_REV;
    private final double originalGearRatio = SwerveConfig.DRIVE_GEAR_RATIO;
    private final double originalWheelRadius = SwerveConfig.WHEEL_RADIUS_METERS;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.DRIVE_TICKS_PER_REV = originalTicksPerRev;
        SwerveConfig.DRIVE_GEAR_RATIO = originalGearRatio;
        SwerveConfig.WHEEL_RADIUS_METERS = originalWheelRadius;
    }

    @Test
    void driveTickConversionUsesConfiguredWheelRadius() {
        SwerveConfig.DRIVE_TICKS_PER_REV = 28.0;
        SwerveConfig.DRIVE_GEAR_RATIO = 1.0;
        SwerveConfig.WHEEL_RADIUS_METERS = 0.0245;

        double metersPerSecond = SwerveModule.driveTicksPerSecondToMetersPerSecond(28.0);

        assertEquals(2.0 * Math.PI * 0.0245, metersPerSecond, 1e-9);
    }
}
