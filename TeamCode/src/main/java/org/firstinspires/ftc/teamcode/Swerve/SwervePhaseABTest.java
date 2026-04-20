package org.firstinspires.ftc.teamcode.Swerve;

import org.firstinspires.ftc.teamcode.Swerve.Drive.SwerveAuditor;
import org.firstinspires.ftc.teamcode.Swerve.Drive.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Drive.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Drive.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Geo.MathUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase A (MathUtil, SwerveModuleState) and
 * Phase B (SwerveKinematics, SwerveAuditor).
 *
 * These tests run on the host JVM without Android hardware.
 */
public class SwervePhaseABTest {

    private static final double EPSILON = 1e-6;

    // ─────────────────────────────────────────────────────────────────────────
    // Phase A — MathUtil
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void normalizeAngle_alreadyInRange() {
        assertEquals(0.5, MathUtil.normalizeAngle(0.5), EPSILON);
        assertEquals(-0.5, MathUtil.normalizeAngle(-0.5), EPSILON);
    }

    @Test
    public void normalizeAngle_wrapAbovePi() {
        // 3π/2 → -π/2
        assertEquals(-Math.PI / 2, MathUtil.normalizeAngle(3 * Math.PI / 2), EPSILON);
    }

    @Test
    public void normalizeAngle_wrapBelowNegPi() {
        // -3π/2 → π/2
        assertEquals(Math.PI / 2, MathUtil.normalizeAngle(-3 * Math.PI / 2), EPSILON);
    }

    @Test
    public void normalizeAngle_exactPi() {
        double result = MathUtil.normalizeAngle(Math.PI);
        assertTrue(result > -Math.PI && result <= Math.PI,
                   "normalizeAngle(π) out of range: " + result);
    }

    @Test
    public void normalizeAngle_largePositive() {
        assertEquals(Math.PI, MathUtil.normalizeAngle(7 * Math.PI), EPSILON);
    }

    @Test
    public void angleError_shortestPath() {
        double current = Math.toRadians(170);
        double target  = Math.toRadians(-170);
        double error   = MathUtil.angleError(current, target);
        assertEquals(Math.toRadians(-20), error, 1e-4);
    }

    @Test
    public void clampPower_withinRange() {
        assertEquals(0.5, MathUtil.clampPower(0.5), EPSILON);
        assertEquals(-0.75, MathUtil.clampPower(-0.75), EPSILON);
    }

    @Test
    public void applyDeadband_belowThreshold() {
        assertEquals(0.0, MathUtil.applyDeadband(0.04, 0.05), EPSILON);
    }

    @Test
    public void applyDeadband_aboveThreshold_rescaled() {
        assertEquals(1.0, MathUtil.applyDeadband(1.0, 0.05), EPSILON);
    }

    @Test
    public void maxAbs_returnsLargestMagnitude() {
        assertEquals(3.0, MathUtil.maxAbs(1.0, -3.0, 2.0), EPSILON);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase A — SwerveModuleState
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void moduleState_defaultConstructor() {
        SwerveModuleState s = new SwerveModuleState();
        assertEquals(0.0, s.speedMetersPerSecond, EPSILON);
        assertEquals(0.0, s.angleRadians, EPSILON);
    }

    @Test
    public void moduleState_angleNormalized() {
        SwerveModuleState s = new SwerveModuleState(1.0, 3 * Math.PI / 2);
        assertEquals(-Math.PI / 2, s.angleRadians, EPSILON);
    }

    @Test
    public void moduleState_fromVector_forward() {
        SwerveModuleState s = SwerveModuleState.fromVector(1.0, 0.0);
        assertEquals(0.0, s.angleRadians, EPSILON);
        assertEquals(1.0, s.speedMetersPerSecond, EPSILON);
    }

    @Test
    public void moduleState_copy_isIndependent() {
        SwerveModuleState original = new SwerveModuleState(0.5, 1.0);
        SwerveModuleState copy     = original.copy();
        copy.speedMetersPerSecond = 0.9;
        assertEquals(0.5, original.speedMetersPerSecond, EPSILON);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase B — SwerveKinematics
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void kinematics_pureForward_allModulesPointForward() {
        SwerveKinematics ik = new SwerveKinematics();
        SwerveModuleState[] states = ik.toModuleStates(1.0, 0.0, 0.0);
        assertEquals(4, states.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(0.0, states[i].angleRadians, 1e-4);
            assertTrue(states[i].speedMetersPerSecond > 0);
        }
    }

    @Test
    public void kinematics_pureForward_allSpeedsEqual() {
        SwerveKinematics ik = new SwerveKinematics();
        SwerveModuleState[] states = ik.toModuleStates(1.0, 0.0, 0.0);
        double speed0 = states[0].speedMetersPerSecond;
        for (SwerveModuleState s : states) {
            assertEquals(speed0, s.speedMetersPerSecond, 1e-4);
        }
    }

    @Test
    public void kinematics_pureRotation_noTwoModulesIdentical() {
        SwerveKinematics ik = new SwerveKinematics();
        SwerveModuleState[] states = ik.toModuleStates(0.0, 0.0, 1.0);
        for (int i = 0; i < 4; i++) {
            assertTrue(Math.abs(states[i].speedMetersPerSecond) > 1e-4);
        }
        assertNotEquals(states[0].angleRadians, states[1].angleRadians, 1e-3);
    }

    @Test
    public void kinematics_normalizeModuleSpeeds_scalesDownWhenAboveMax() {
        SwerveModuleState[] states = new SwerveModuleState[]{
            new SwerveModuleState(2.0, 0),
            new SwerveModuleState(3.0, 0),
            new SwerveModuleState(1.5, 0),
            new SwerveModuleState(1.0, 0)
        };
        SwerveKinematics.normalizeModuleSpeeds(states, 1.0);
        for (SwerveModuleState s : states) {
            assertTrue(Math.abs(s.speedMetersPerSecond) <= 1.0 + EPSILON);
        }
        assertEquals(1.0, states[1].speedMetersPerSecond, EPSILON);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase B — SwerveAuditor (flip optimization)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void auditor_flipOptimize_noFlipWhenErrorUnder90deg() {
        double currentAngle = 0.0;
        SwerveModuleState desired = new SwerveModuleState(1.0, Math.toRadians(45));
        SwerveModuleState result = SwerveAuditor.optimizeSingle(desired.copy(), currentAngle);
        assertEquals(1.0, result.speedMetersPerSecond, EPSILON);
        assertEquals(Math.toRadians(45), result.angleRadians, 1e-4);
    }

    @Test
    public void auditor_flipOptimize_flipWhenErrorOver90deg() {
        double currentAngle = 0.0;
        SwerveModuleState desired = new SwerveModuleState(1.0, Math.toRadians(200));
        SwerveModuleState result = SwerveAuditor.optimizeSingle(desired.copy(), currentAngle);
        assertEquals(-1.0, result.speedMetersPerSecond, EPSILON);
        assertEquals(Math.toRadians(20), result.angleRadians, 1e-3);
    }

    @Test
    public void auditor_normalizeByMax_scalesAllProportionally() {
        SwerveModuleState[] states = new SwerveModuleState[]{
            new SwerveModuleState(4.0, 0),
            new SwerveModuleState(2.0, 0),
            new SwerveModuleState(3.0, 0),
            new SwerveModuleState(1.0, 0)
        };
        SwerveAuditor.normalizeByMax(states, 1.0);
        assertEquals(1.0,  states[0].speedMetersPerSecond, EPSILON);
        assertEquals(0.5,  states[1].speedMetersPerSecond, EPSILON);
        assertEquals(0.75, states[2].speedMetersPerSecond, EPSILON);
        assertEquals(0.25, states[3].speedMetersPerSecond, EPSILON);
    }
}