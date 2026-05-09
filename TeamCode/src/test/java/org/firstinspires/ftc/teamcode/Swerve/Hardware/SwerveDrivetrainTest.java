package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveDrivetrainTest {

    private final boolean originalEnableIdleXStance = SwerveConfig.ENABLE_IDLE_X_STANCE;
    private final double originalLockDelay = SwerveConfig.LOCK_DELAY_MS;
    private final boolean originalRequireSteerReadyForDrive = SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE;
    private final double originalSteerReadyTolerance = SwerveConfig.STEER_READY_ANGLE_TOLERANCE_RAD;
    private final double originalSteerDriveFullAuthority = SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD;
    private final double originalSteerDriveHardCutoff = SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD;
    private final double originalSteerDriveMinAuthority = SwerveConfig.STEER_DRIVE_MIN_AUTHORITY;
    private final double originalSteerDriveMinCosineFactor = SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR;
    private final double[] originalLockedStanceAngles = SwerveConfig.LOCKED_STANCE_ANGLES_RAD.clone();

    @AfterEach
    void restoreConfig() {
        SwerveConfig.ENABLE_IDLE_X_STANCE = originalEnableIdleXStance;
        SwerveConfig.LOCK_DELAY_MS = originalLockDelay;
        SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE = originalRequireSteerReadyForDrive;
        SwerveConfig.STEER_READY_ANGLE_TOLERANCE_RAD = originalSteerReadyTolerance;
        SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD = originalSteerDriveFullAuthority;
        SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD = originalSteerDriveHardCutoff;
        SwerveConfig.STEER_DRIVE_MIN_AUTHORITY = originalSteerDriveMinAuthority;
        SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR = originalSteerDriveMinCosineFactor;
        SwerveConfig.LOCKED_STANCE_ANGLES_RAD = originalLockedStanceAngles.clone();
    }

    @Test
    void drivetrainStateStillTransitionsWhenIdleXStanceIsDisabled() {
        // Passes if the drivetrain can still report an idle/settled state even when X-stance is disabled.
        SwerveConfig.ENABLE_IDLE_X_STANCE = false;
        SwerveConfig.LOCK_DELAY_MS = 20.0;
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.01);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);

        assertEquals(SwerveDrivetrain.States.LOCKED, drivetrain.getState());
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

    @Test
    void driveIsHeldUntilAllModulesReachSteeringTargets() {
        SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE = true;
        SwerveConfig.STEER_READY_ANGLE_TOLERANCE_RAD = Math.toRadians(5.0);
        TestModuleIO[] ios = {
                new TestModuleIO(), new TestModuleIO(), new TestModuleIO(), new TestModuleIO()
        };
        ios[0].currentRotationRadians = Math.toRadians(30.0);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(ios), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(1.0, 0.0, 0.0), 0.02);

        assertEquals(false, drivetrain.isSteerReadyForDrive());
        for (TestModuleIO io : ios) {
            assertEquals(0.0, io.drivePower, 1e-9);
        }
    }

    @Test
    void driveIsStronglyReducedAtLargeSteerErrorWithoutFullGate() {
        // Passes if a misaligned module still receives some drive power, but less than the aligned modules under the same chassis request.
        SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE = false;
        SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD = Math.toRadians(5.0);
        SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD = Math.toRadians(35.0);
        SwerveConfig.STEER_DRIVE_MIN_AUTHORITY = 0.12;
        SwerveConfig.STEER_DRIVE_MIN_COSINE_FACTOR = 0.35;
        TestModuleIO[] ios = {
                new TestModuleIO(), new TestModuleIO(), new TestModuleIO(), new TestModuleIO()
        };
        ios[0].currentRotationRadians = Math.toRadians(25.0);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(ios), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(1.0, 0.0, 0.0), 0.02);

        assertTrue(Math.abs(ios[0].drivePower) > 0.0);
        assertTrue(Math.abs(ios[0].drivePower) < Math.abs(ios[1].drivePower));
    }

    @Test
    void lockedStateUsesConfiguredRotateReadyStance() {
        SwerveConfig.ENABLE_IDLE_X_STANCE = true;
        SwerveConfig.LOCK_DELAY_MS = 20.0;
        SwerveConfig.LOCKED_STANCE_ANGLES_RAD = new double[] {
                Math.toRadians(135.0),
                Math.toRadians(45.0),
                Math.toRadians(-45.0),
                Math.toRadians(-135.0)
        };
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(createModules(), () -> 12.0, null);

        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.01);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);
        drivetrain.setVelocity(new Vector(0.0, 0.0, 0.0), 0.02);

        assertEquals(SwerveDrivetrain.States.LOCKED, drivetrain.getState());
        assertEquals(Math.toRadians(135.0), drivetrain.getLastOptimizedStates()[0].angleRadians, 1e-9);
        assertEquals(Math.toRadians(45.0), drivetrain.getLastOptimizedStates()[1].angleRadians, 1e-9);
        assertEquals(Math.toRadians(-45.0), drivetrain.getLastOptimizedStates()[2].angleRadians, 1e-9);
        assertEquals(Math.toRadians(-135.0), drivetrain.getLastOptimizedStates()[3].angleRadians, 1e-9);
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
        private double currentRotationRadians;

        @Override
        public double getCurrentRotationRadians() {
            return currentRotationRadians;
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
