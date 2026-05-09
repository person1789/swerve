package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
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
    void feasibleProjectionReducesUnsupportedStrafeWhenAllModulesPointForward() {
        SwerveKinematics kinematics = new SwerveKinematics();

        Vector feasible = kinematics.projectToCurrentAngleFeasibleVelocity(
                new Vector(0.0, 1.0, 0.0),
                new double[] {0.0, 0.0, 0.0, 0.0},
                12.0);

        assertTrue(Math.abs(feasible.y()) < 0.25);
    }
}
