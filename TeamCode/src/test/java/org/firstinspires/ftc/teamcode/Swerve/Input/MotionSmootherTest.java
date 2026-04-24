package org.firstinspires.ftc.teamcode.Swerve.Input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MotionSmootherTest {

    private final double originalMaxSpeed = SwerveConfig.MAX_LINEAR_SPEED_IN_S;
    private final double originalMaxOmega = SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
    private final double originalRedirectAngle = SwerveConfig.TRANSLATION_REDIRECT_ANGLE_RAD;
    private final double originalRedirectRelease = SwerveConfig.TRANSLATION_REDIRECT_RELEASE_SPEED_FRACTION;
    private final double originalRedirectMultiplier = SwerveConfig.TRANSLATION_REDIRECT_DECEL_MULTIPLIER;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.MAX_LINEAR_SPEED_IN_S = originalMaxSpeed;
        SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S = originalMaxOmega;
        SwerveConfig.TRANSLATION_REDIRECT_ANGLE_RAD = originalRedirectAngle;
        SwerveConfig.TRANSLATION_REDIRECT_RELEASE_SPEED_FRACTION = originalRedirectRelease;
        SwerveConfig.TRANSLATION_REDIRECT_DECEL_MULTIPLIER = originalRedirectMultiplier;
    }

    @Test
    void setLimitsConstrainsAccelerationDuringRampUp() {
        // Passes if, once the smoother is already moving in the same direction, a tighter acceleration limit caps further velocity growth to maxAccel * dt.
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(1.0, 1000.0);
        Vector first = smoother.smooth(new Vector(0.5, 0.0, 0.0), 0.1);

        Vector result = smoother.smooth(new Vector(1.0, 0.0, 0.0), 0.1);

        assertTrue(result.x() > first.x());
        assertTrue(result.x() - first.x() <= 0.1 + 1e-9);
        assertEquals(0.0, result.y(), 1e-9);
        assertEquals(0.0, result.omega(), 1e-9);
    }

    @Test
    void brakingSnapsDirectlyToReducedTarget() {
        // Passes if commanding a smaller target after ramp-up immediately drops the output to that target instead of coasting past it.
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(5.0, 1000.0);
        smoother.smooth(new Vector(1.0, 0.0, 0.0), 0.1);

        Vector result = smoother.smooth(new Vector(0.0, 0.0, 0.0), 0.1);

        assertEquals(0.0, result.x(), 1e-9);
    }

    @Test
    void resetClearsAccumulatedVelocityAndAcceleration() {
        // Passes if reset returns the smoother to a fresh state so the next command behaves the same as the very first command from rest.
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(2.0, 1000.0);
        Vector first = smoother.smooth(new Vector(1.0, 0.0, 0.0), 0.1);
        smoother.reset();

        Vector result = smoother.smooth(new Vector(1.0, 0.0, 0.0), 0.1);

        assertEquals(first.x(), result.x(), 1e-9);
    }

    @Test
    void rotationalInputIsSmoothedIndependently() {
        // Passes if a pure rotational command changes omega while leaving translation untouched.
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(2.0, 1000.0);

        Vector result = smoother.smooth(new Vector(0.0, 0.0, 1.0), 0.1);

        assertEquals(0.0, result.x(), 1e-9);
        assertEquals(0.0, result.y(), 1e-9);
        assertTrue(result.omega() > 0.0);
    }

    @Test
    void fullScaleInputMapsToConfiguredPhysicalLimits() {
        // Passes if full stick commands are converted into the configured physical translation and rotation limits before smoothing.
        SwerveConfig.MAX_LINEAR_SPEED_IN_S = 2.0 / 0.0254;
        SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S = 5.0;
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(1000.0, 1000.0);

        Vector result = smoother.smooth(new Vector(1.0, 0.0, 1.0), 0.1);

        assertEquals(2.0, result.x(), 1e-9);
        assertEquals(0.0, result.y(), 1e-9);
        assertEquals(5.0, result.omega(), 1e-9);
    }

    @Test
    void largeTranslationDirectionChangesBrakeBeforeRedirecting() {
        SwerveConfig.MAX_LINEAR_SPEED_IN_S = 1.0 / 0.0254;
        SwerveConfig.TRANSLATION_REDIRECT_ANGLE_RAD = Math.toRadians(45.0);
        SwerveConfig.TRANSLATION_REDIRECT_RELEASE_SPEED_FRACTION = 0.05;
        SwerveConfig.TRANSLATION_REDIRECT_DECEL_MULTIPLIER = 2.0;

        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(1.0, 1000.0);

        smoother.smooth(new Vector(1.0, 0.0, 0.0), 0.1);
        Vector redirected = smoother.smooth(new Vector(0.0, 1.0, 0.0), 0.1);

        assertEquals(0.0, redirected.y(), 1e-9);
        assertTrue(redirected.x() < 0.11);
    }
}
