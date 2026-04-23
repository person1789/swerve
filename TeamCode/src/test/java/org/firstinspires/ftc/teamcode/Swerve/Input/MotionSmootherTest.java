package org.firstinspires.ftc.teamcode.Swerve.Input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.Test;

class MotionSmootherTest {

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
}
