package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveDrivetrainTest {

    private final boolean originalEnableIdleXStance = SwerveConfig.ENABLE_IDLE_X_STANCE;
    private final double originalLockDelay = SwerveConfig.LOCK_DELAY_MS;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.ENABLE_IDLE_X_STANCE = originalEnableIdleXStance;
        SwerveConfig.LOCK_DELAY_MS = originalLockDelay;
    }

    @Test
    void idleXStanceCanBeDisabledToPreserveNeutralDriverIntent() {
        // Passes if zero input keeps the drivetrain in normal driving state when idle X-stance is disabled.
        SwerveConfig.ENABLE_IDLE_X_STANCE = false;
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.05);

        assertEquals(SwerveDrivetrain.States.DRIVING, drivetrain.getState());
    }

    @Test
    void idleXStanceCanStillLockWhenExplicitlyEnabled() {
        // Passes if repeated zero-input updates advance the drivetrain into LOCKED once the configured idle delay elapses.
        SwerveConfig.ENABLE_IDLE_X_STANCE = true;
        SwerveConfig.LOCK_DELAY_MS = 20.0;
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.01);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);

        assertEquals(SwerveDrivetrain.States.LOCKED, drivetrain.getState());
    }

    @Test
    void constructorSetsDriveModeOnAllModules() {
        // Passes if drivetrain construction configures each module into RUN_WITHOUT_ENCODER mode for observer-backed open-loop driving.
        TestModuleIO[] ios = {
                new TestModuleIO(), new TestModuleIO(), new TestModuleIO(), new TestModuleIO()
        };

        new SwerveDrivetrain(createModules(ios), () -> 12.0, null);

        for (TestModuleIO io : ios) {
            assertEquals(DcMotor.RunMode.RUN_WITHOUT_ENCODER, io.lastMode);
        }
    }

    private SwerveModule[] createModules() {
        return createModules(new TestModuleIO(), new TestModuleIO(), new TestModuleIO(), new TestModuleIO());
    }

    private SwerveModule[] createModules(TestModuleIO... ios) {
        return new SwerveModule[] {
                new SwerveModule(ios[0], 0.0, false, null),
                new SwerveModule(ios[1], 0.0, false, null),
                new SwerveModule(ios[2], 0.0, false, null),
                new SwerveModule(ios[3], 0.0, false, null)
        };
    }

    private static class TestModuleIO implements SwerveModuleIO {
        private DcMotor.RunMode lastMode;
        private double drivePower;

        @Override
        public double getCurrentRotationRadians() {
            return 0.0;
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
            this.drivePower = power;
        }

        @Override
        public void setSteerPower(double power) {
        }

        @Override
        public void setDriveMode(DcMotor.RunMode mode) {
            this.lastMode = mode;
        }
    }
}
