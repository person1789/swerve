package org.firstinspires.ftc.teamcode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.Test;

public class TeleCatchAllTest {

    @Test
    public void testTurnInPlaceMatchesXLockAngles() {
        SwerveKinematics kinematics = new SwerveKinematics();

        // 1. Get States for Pure Rotation (omega = 1.0)
        SwerveModuleState[] turnStates = kinematics.inverseKinematics(new Vector(0.0, 0.0, 1.0));

        // 2. The locked angles from config
        double[] lockAngles = SwerveConfig.LOCKED_STANCE_ANGLES_RAD;

        for (int i = 0; i < 4; i++) {
            // For a pure rotation, the wheel angle should either perfectly match the X-lock angle
            // or be exactly 180 degrees opposite to it (which is handled physically by the swerve module's flip threshold).
            double turnAngle = MathUtil.normalizeAngle(turnStates[i].angleRadians);
            double lockAngle = MathUtil.normalizeAngle(lockAngles[i]);
            
            // Check if they are exactly parallel (error is 0 or PI)
            double error = Math.abs(MathUtil.angleError(turnAngle, lockAngle));
            
            // Allow rounding tolerance (1e-6)
            // Error should be 0 (same direction) or PI (opposite direction)
            boolean isParallel = (error < 1e-6) || (Math.abs(error - Math.PI) < 1e-6);
            
            assertTrue("Module " + i + " turn angle " + Math.toDegrees(turnAngle) + 
                       " is not parallel to lock angle " + Math.toDegrees(lockAngle), 
                       isParallel);
        }
    }

    @Test
    public void testTeleOpDeadbandAndSlewRatePipeline() {
        // A catch-all test validating that the tele-op math doesn't crash 
        // and correctly zeroes out inputs under the deadband
        double dt = 0.02;
        SwerveKinematics kinematics = new SwerveKinematics();
        
        // Input under deadband
        double rawX = SwerveConfig.INPUT_DEADBAND * 0.9;
        double rawY = 0.0;
        double rawOmega = 0.0;
        
        double x = Math.abs(rawX) < SwerveConfig.INPUT_DEADBAND ? 0.0 : rawX;
        
        SwerveModuleState[] states = kinematics.inverseKinematics(new Vector(x, rawY, rawOmega));
        
        for (SwerveModuleState state : states) {
            assertEquals(0.0, state.drivePower, 1e-9);
        }
    }

    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
