package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.Test;

class SwerveKinematicsAndAuditorTest {

    @Test
    void pureTranslationProducesMatchingModuleStatesAndRoundTrips() {
        // Passes if pure forward translation gives each module the same state and forward kinematics reconstructs the chassis command.
        SwerveKinematics kinematics = new SwerveKinematics();

        SwerveModuleState[] states = kinematics.inverseKinematics(new Vector(1.0, 0.0, 0.0));
        Vector chassis = kinematics.forwardKinematics(states);

        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, 1e-9);
            assertEquals(0.0, state.angleRadians, 1e-9);
        }
        assertEquals(1.0, chassis.x(), 1e-9);
        assertEquals(0.0, chassis.y(), 1e-9);
        assertEquals(0.0, chassis.omega(), 1e-9);
    }

    @Test
    void compatibilityAliasesUseSameUnderlyingKinematics() {
        // Passes if the alias APIs return the same module states and chassis speeds as the primary APIs.
        SwerveKinematics kinematics = new SwerveKinematics();

        SwerveModuleState[] states = kinematics.toModuleStates(0.0, 1.0, 0.0);
        Pose chassis = kinematics.toChassisSpeeds(states);

        assertEquals(Math.PI / 2.0, states[0].angleRadians, 1e-9);
        assertEquals(0.0, chassis.x, 1e-9);
        assertEquals(1.0, chassis.y, 1e-9);
        assertEquals(0.0, chassis.heading, 1e-9);
    }

    @Test
    void auditorFlipsLargeAngleErrorsAndDesaturatesSpeeds() {
        // Passes if over-limit speeds are desaturated and states beyond the flip threshold are re-aimed to the shorter steering angle.
        SwerveAuditor auditor = new SwerveAuditor();
        SwerveModuleState[] desired = {
                new SwerveModuleState(3.0, Math.toRadians(100.0)),
                new SwerveModuleState(3.0, 0.0),
                new SwerveModuleState(3.0, 0.0),
                new SwerveModuleState(3.0, 0.0)
        };
        double[] currentAngles = {0.0, 0.0, 0.0, 0.0};

        SwerveModuleState[] optimized = auditor.optimize(desired, currentAngles);

        assertEquals(Math.toRadians(-80.0), optimized[0].angleRadians, 1e-9);
        double maxLinearSpeedMps = org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig.getMaxLinearSpeedMetersPerSecond();
        for (int i = 0; i < optimized.length; i++) {
            assertTrue(Math.abs(optimized[i].speedMetersPerSecond) <= maxLinearSpeedMps + 1e-9);
        }
    }

    @Test
    void auditorSanitizesNanAndAppliesCosineScaling() {
        // Passes if NaN inputs are replaced with zeros and perpendicular steering commands zero the drive speed through cosine scaling.
        SwerveAuditor auditor = new SwerveAuditor();
        SwerveModuleState[] desired = {
                new SwerveModuleState(Double.NaN, Double.NaN),
                new SwerveModuleState(1.0, Math.PI / 2.0),
                new SwerveModuleState(0.5, 0.0),
                new SwerveModuleState(0.5, 0.0)
        };
        double[] currentAngles = {0.0, 0.0, 0.0, 0.0};

        SwerveModuleState[] optimized = auditor.optimize(desired, currentAngles);

        assertEquals(0.0, optimized[0].speedMetersPerSecond, 1e-9);
        assertEquals(0.0, optimized[0].angleRadians, 1e-9);
        assertEquals(0.0, optimized[1].speedMetersPerSecond, 1e-9);
    }

    @Test
    void auditorFlipPreservesDriveIntentByInvertingSpeed() {
        // Passes if a near-180-degree steering request becomes a negative wheel-speed command at the shorter steering angle instead of collapsing to zero speed.
        SwerveAuditor auditor = new SwerveAuditor();
        SwerveModuleState[] desired = {
                new SwerveModuleState(1.0, Math.PI),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0)
        };
        double[] currentAngles = {0.0, 0.0, 0.0, 0.0};

        SwerveModuleState[] optimized = auditor.optimize(desired, currentAngles);

        assertEquals(0.0, optimized[0].angleRadians, 1e-9);
        assertEquals(-1.0, optimized[0].speedMetersPerSecond, 1e-9);
    }
}
