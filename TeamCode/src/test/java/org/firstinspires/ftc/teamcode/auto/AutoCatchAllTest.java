package org.firstinspires.ftc.teamcode.auto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.junit.jupiter.api.Test;

public class AutoCatchAllTest {

    @Test
    public void testFullAutoPipelineExecution() {
        // This catch-all test verifies that the entire Auto pipeline:
        // DriveScheduler -> MoveToPoseCommand -> AutoMath -> SwerveDrivetrain -> Hardware
        // can instantiate and run without null pointers or index out of bounds exceptions.
        
        TestModuleHardware[] hardware = new TestModuleHardware[] {
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware(),
                new TestModuleHardware()
        };
        
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(new SwerveModule[] {
                new SwerveModule(hardware[0], 0.0, false),
                new SwerveModule(hardware[1], 0.0, false),
                new SwerveModule(hardware[2], 0.0, false),
                new SwerveModule(hardware[3], 0.0, false)
        });

        MockPoseProvider pose = new MockPoseProvider();
        DriveContext context = new DriveContext(drivetrain, pose, null);
        
        // Command the robot to drive diagonally and turn simultaneously
        DriveScheduler scheduler = new DriveScheduler(2)
                .addMoveToPose(24.0, 24.0, Math.PI / 2.0, false);
                
        // Tick 1: Initialize
        scheduler.tick(context, 0.02);
        drivetrain.write(0.02);
        
        // Assert command started but isn't finished
        assertFalse(scheduler.isFinished(), "Scheduler should not finish instantly on a long move");
        
        // Simulate robot moving halfway
        pose.x = 12.0;
        pose.y = 12.0;
        pose.heading = Math.PI / 4.0;
        
        // Tick 2: Mid-move
        scheduler.tick(context, 0.02);
        drivetrain.write(0.02);
        assertFalse(scheduler.isFinished());

        // Simulate robot reaching target
        pose.x = 24.0;
        pose.y = 24.0;
        pose.heading = Math.PI / 2.0;

        // Tick 3: Inside tolerance
        scheduler.tick(context, 0.02);
        drivetrain.write(0.02);
        
        // Tick 4+: Wait for settle delay
        for(int i = 0; i < 50; i++) {
            scheduler.tick(context, 0.02);
            drivetrain.write(0.02);
        }
        
        // Assert command completed normally
        assertTrue(scheduler.isFinished(), "Scheduler should finish after reaching pose and settling");
        assertFalse(scheduler.wasAborted(), "Scheduler should not abort on successful move");
    }

    private static class MockPoseProvider implements AutoPoseProvider {
        double x = 0.0;
        double y = 0.0;
        double heading = 0.0;

        @Override
        public void update() {}

        @Override
        public double getXInches() { return x; }

        @Override
        public double getYInches() { return y; }

        @Override
        public double getHeadingRadians() {
            return heading;
        }

        @Override
        public void setPose(double xInches, double yInches, double headingRadians) {
            this.x = xInches;
            this.y = yInches;
            this.heading = headingRadians;
        }

        @Override
        public boolean isReady() { return true; }
    }

    private static class TestModuleHardware implements SwerveModule.HardwareAdapter {
        double rotationRadians = 0.0;
        double drivePower = 0.0;

        @Override
        public void read() {}

        @Override
        public double getRotationRadians() { return rotationRadians; }

        @Override
        public double getDriveVelocityTicksPerSecond() { return 0.0; }

        @Override
        public void setDrivePower(double power) { drivePower = power; }

        @Override
        public void setSteerPower(double power) {}

        @Override
        public void setDriveMode(DcMotor.RunMode mode) {}
    }
}
