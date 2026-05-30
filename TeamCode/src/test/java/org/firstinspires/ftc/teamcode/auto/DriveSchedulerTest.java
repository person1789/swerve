package org.firstinspires.ftc.teamcode.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DriveSchedulerTest {
    private final double originalAzimuthTolerance = SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD;
    private final double originalAzimuthTimeout = SwerveConfig.AUTO_AZIMUTH_TIMEOUT_MS;
    private final boolean originalAbortOnAzimuthTimeout = SwerveConfig.AUTO_ABORT_ON_AZIMUTH_TIMEOUT;
    private final double originalTranslationTolerance = SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN;
    private final double originalHeadingTolerance = SwerveConfig.AUTO_HEADING_TOLERANCE_RAD;
    private final double originalTranslationDeadband = SwerveConfig.AUTO_TRANSLATION_DEADBAND;
    private final double originalMoveTimeout = SwerveConfig.AUTO_MOVE_TIMEOUT_MS;
    private final double originalAutoYP = SwerveConfig.AUTO_Y_P;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD = originalAzimuthTolerance;
        SwerveConfig.AUTO_AZIMUTH_TIMEOUT_MS = originalAzimuthTimeout;
        SwerveConfig.AUTO_ABORT_ON_AZIMUTH_TIMEOUT = originalAbortOnAzimuthTimeout;
        SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN = originalTranslationTolerance;
        SwerveConfig.AUTO_HEADING_TOLERANCE_RAD = originalHeadingTolerance;
        SwerveConfig.AUTO_TRANSLATION_DEADBAND = originalTranslationDeadband;
        SwerveConfig.AUTO_MOVE_TIMEOUT_MS = originalMoveTimeout;
        SwerveConfig.AUTO_Y_P = originalAutoYP;
    }

    @Test
    void schedulerRunsQueuedCommandsInOrderAndEndsOnce() {
        // Passes if each command initializes, ticks, and ends once before the scheduler advances.
        ManualPoseProvider pose = new ManualPoseProvider();
        DriveContext context = new DriveContext(createDrivetrain(createHardware()), pose, null);
        CountingCommand first = new CountingCommand("first");
        CountingCommand second = new CountingCommand("second");
        DriveScheduler scheduler = new DriveScheduler(2).add(first).add(second);

        scheduler.tick(context, 0.02);
        scheduler.tick(context, 0.02);

        assertTrue(scheduler.isFinished());
        assertEquals(1, first.initCount);
        assertEquals(1, first.tickCount);
        assertEquals(1, first.endCount);
        assertEquals(1, second.initCount);
        assertEquals(1, second.tickCount);
        assertEquals(1, second.endCount);
    }

    @Test
    void addMoveToPoseInsertsAzimuthWaitBeforeMovement() {
        // Passes if a move with azimuth wait starts with the wait command before the pose command.
        DriveScheduler scheduler = new DriveScheduler(3).addMoveToPose(24.0, 0.0, 0.0, true);

        assertEquals("wait_azimuth", scheduler.getCurrentCommandName());
    }

    @Test
    void azimuthWaitHoldsDrivePowerAtZeroUntilReady() {
        // Passes if steering-only wait produces zero drive power while modules are still turning.
        TestModuleHardware[] hardware = createHardware();
        ManualPoseProvider pose = new ManualPoseProvider();
        SwerveDrivetrain drivetrain = createDrivetrain(hardware);
        DriveContext context = new DriveContext(drivetrain, pose, null);
        WaitForAzimuthCommand wait = new WaitForAzimuthCommand(24.0, 0.0, 0.0,
                Math.toRadians(1.0), 1000.0, true);

        wait.init(context);
        wait.tick(context, 0.02);
        drivetrain.write(0.02);

        for (TestModuleHardware module : hardware) {
            assertEquals(0.0, module.drivePower, 1e-9);
        }
        assertFalse(wait.isFinished(context));
    }

    @Test
    void azimuthTimeoutRequestsAbortByDefault() {
        // Passes if a timed-out azimuth wait aborts the scheduler and commands zero drive.
        SwerveConfig.AUTO_Y_P = 1.0; // Ensure Y power is non-zero so target angle is non-zero
        
        TestModuleHardware[] hardware = createHardware();
        ManualPoseProvider pose = new ManualPoseProvider();
        SwerveDrivetrain drivetrain = createDrivetrain(hardware);
        DriveContext context = new DriveContext(drivetrain, pose, null);
        DriveScheduler scheduler = new DriveScheduler(2)
                // Command a strafe (Y=24) instead of forward (X=24) so target angle is non-zero 
                // and the dummy modules (which report 0.0) are forced to wait and timeout.
                .add(new WaitForAzimuthCommand(0.0, 24.0, 0.0, Math.toRadians(1.0), 10.0, true))
                .add(new CountingCommand("never"));

        scheduler.tick(context, 0.02);
        drivetrain.write(0.02);
        scheduler.tick(context, 0.02);

        assertTrue(scheduler.wasAborted());
        assertTrue(scheduler.isFinished());
    }

    @Test
    void moveToPoseFinishesAfterSettlingInsideTolerance() {
        // Passes if a pose command waits for the configured settle delay before finishing.
        SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN = 1.0;
        SwerveConfig.AUTO_HEADING_TOLERANCE_RAD = Math.toRadians(2.0);
        ManualPoseProvider pose = new ManualPoseProvider();
        pose.x = 24.0;
        DriveContext context = new DriveContext(createDrivetrain(createHardware()), pose, null);
        MoveToPoseCommand command = new MoveToPoseCommand(24.0, 0.0, 0.0, 40.0, 1000.0);

        command.init(context);
        command.tick(context, 0.02);
        assertFalse(command.isFinished(context));
        command.tick(context, 0.02);

        assertTrue(command.isFinished(context));
    }

    @Test
    void kookyDeadbandZerosTinyTranslationPower() {
        // Passes if Kooky-style pose math drops tiny translation commands to zero.
        SwerveConfig.AUTO_TRANSLATION_DEADBAND = 0.01;
        ManualPoseProvider pose = new ManualPoseProvider();
        double[] output = new double[3];

        AutoMath.calculateInitialPowers(pose, 0.1, 0.0, 0.0, output);

        assertEquals(0.0, output[0], 1e-9);
        assertEquals(0.0, output[1], 1e-9);
    }

    private static SwerveDrivetrain createDrivetrain(TestModuleHardware[] hardware) {
        return new SwerveDrivetrain(new SwerveModule[] {
                new SwerveModule(hardware[0], 0.0, false),
                new SwerveModule(hardware[1], 0.0, false),
                new SwerveModule(hardware[2], 0.0, false),
                new SwerveModule(hardware[3], 0.0, false)
        });
    }

    private static TestModuleHardware[] createHardware() {
        return new TestModuleHardware[] {
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware()
        };
    }

    private static class ManualPoseProvider implements AutoPoseProvider {
        double x;
        double y;
        double h;

        @Override
        public void update() {
        }

        @Override
        public double getXInches() {
            return x;
        }

        @Override
        public double getYInches() {
            return y;
        }

        @Override
        public double getHeadingRadians() {
            return h;
        }

        @Override
        public void setPose(double xInches, double yInches, double headingRadians) {
            this.x = xInches;
            this.y = yInches;
            this.h = headingRadians;
        }

        @Override
        public boolean isReady() {
            return true;
        }
    }

    private static class CountingCommand implements DriveCommand {
        final String name;
        int initCount;
        int tickCount;
        int endCount;

        CountingCommand(String name) {
            this.name = name;
        }

        @Override
        public void init(DriveContext context) {
            initCount++;
        }

        @Override
        public void tick(DriveContext context, double dt) {
            tickCount++;
        }

        @Override
        public boolean isFinished(DriveContext context) {
            return tickCount > 0;
        }

        @Override
        public void end(DriveContext context, boolean interrupted) {
            endCount++;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static class TestModuleHardware implements SwerveModule.HardwareAdapter {
        double rotationRadians;
        double drivePower;

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
