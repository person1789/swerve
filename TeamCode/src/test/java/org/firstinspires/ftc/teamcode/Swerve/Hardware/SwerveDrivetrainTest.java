package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveDrivetrainTest {

    private final double originalLockDelay = SwerveConfig.LOCK_DELAY_MS;
    private final double[] originalLockedAngles = SwerveConfig.LOCKED_STANCE_ANGLES_RAD.clone();

    @AfterEach
    void restoreConfig() {
        SwerveConfig.LOCK_DELAY_MS = originalLockDelay;
        SwerveConfig.LOCKED_STANCE_ANGLES_RAD = originalLockedAngles.clone();
    }

    @Test
    void constructorSetsDriveMotorsToRunWithoutEncoder() {
        // Passes if every module is put in open-loop drive mode while encoder reads remain available.
        TestModuleHardware[] hardware = createHardware();

        new SwerveDrivetrain(createModules(hardware));

        for (TestModuleHardware moduleHardware : hardware) {
            assertEquals(DcMotor.RunMode.RUN_WITHOUT_ENCODER, moduleHardware.lastMode);
        }
    }

    @Test
    void driverInputCommandsSecondOrderModuleStates() {
        // Passes if nonzero input keeps the FSM driving and produces positive module speed commands.
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(createHardware()));

        drivetrain.set(1.0, 0.0, 0.0, 0.02);

        assertEquals(SwerveDrivetrain.States.DRIVING, drivetrain.getState());
        for (SwerveModuleState state : drivetrain.getCommandedStates()) {
            assertTrue(state.speedMetersPerSecond > 0.0);
            assertEquals(0.0, state.angleRadians, 1e-9);
        }
    }

    @Test
    void fsmLocksAfterIdleDelay() {
        // Passes if repeated zero-input loops advance DRIVING -> WAITING_TO_LOCK -> LOCKED.
        SwerveConfig.LOCK_DELAY_MS = 20.0;
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(createHardware()));

        drivetrain.set(0.0, 0.0, 0.0, 0.01);
        drivetrain.set(0.0, 0.0, 0.0, 0.02);

        assertEquals(SwerveDrivetrain.States.LOCKED, drivetrain.getState());
    }

    @Test
    void lockedStateUsesConfiguredModuleAngles() {
        // Passes if LOCKED writes the configured X-stance angles with zero wheel speed.
        SwerveConfig.LOCK_DELAY_MS = 0.0;
        SwerveConfig.LOCKED_STANCE_ANGLES_RAD = new double[] {
                Math.toRadians(135.0),
                Math.toRadians(45.0),
                Math.toRadians(-45.0),
                Math.toRadians(-135.0)
        };
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(createHardware()));

        drivetrain.set(0.0, 0.0, 0.0, 0.02);
        drivetrain.set(0.0, 0.0, 0.0, 0.02);

        SwerveModuleState[] states = drivetrain.getCommandedStates();
        assertEquals(0.0, states[0].speedMetersPerSecond, 1e-9);
        assertEquals(Math.toRadians(135.0), states[0].angleRadians, 1e-9);
        assertEquals(Math.toRadians(45.0), states[1].angleRadians, 1e-9);
        assertEquals(Math.toRadians(-45.0), states[2].angleRadians, 1e-9);
        assertEquals(Math.toRadians(-135.0), states[3].angleRadians, 1e-9);
    }

    private static SwerveModule[] createModules(TestModuleHardware[] hardware) {
        return new SwerveModule[] {
                new SwerveModule(hardware[0], 0.0, false),
                new SwerveModule(hardware[1], 0.0, false),
                new SwerveModule(hardware[2], 0.0, false),
                new SwerveModule(hardware[3], 0.0, false)
        };
    }

    private static TestModuleHardware[] createHardware() {
        return new TestModuleHardware[] {
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware()
        };
    }

    private static class TestModuleHardware implements SwerveModule.HardwareAdapter {
        private DcMotor.RunMode lastMode;

        @Override
        public void read() {
        }

        @Override
        public double getRotationRadians() {
            return 0.0;
        }

        @Override
        public double getDriveVelocityTicksPerSecond() {
            return 0.0;
        }

        @Override
        public void setDrivePower(double power) {
        }

        @Override
        public void setSteerPower(double power) {
        }

        @Override
        public void setDriveMode(DcMotor.RunMode mode) {
            lastMode = mode;
        }
    }
}
