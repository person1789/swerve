package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PIDControllerTest {

    @Test
    void calculateUsesProportionalAndIntegralTerms() {
        // Passes if repeated calls accumulate integral error while preserving the expected proportional contribution.
        PIDController controller = new PIDController(2.0, 1.0, 0.0);
        controller.setSetpoint(1.0);

        double first = controller.calculate(0.0, 0.1);
        double second = controller.calculate(0.0, 0.1);

        assertEquals(2.1, first, 1e-9);
        assertEquals(2.2, second, 1e-9);
    }

    @Test
    void derivativeOnMeasurementAvoidsKickWhenOnlySetpointChanges() {
        // Passes if changing the setpoint without changing the measurement does not create derivative-only output.
        PIDController controller = new PIDController(0.0, 0.0, 5.0);

        controller.setSetpoint(0.0);
        controller.calculate(0.0, 0.02);
        controller.setSetpoint(1.0);

        assertEquals(0.0, controller.calculate(0.0, 0.02), 1e-9);
    }

    @Test
    void calculateFromErrorHonorsWrappedErrorDirection() {
        // Passes if a pre-wrapped positive error yields positive output and the next zero-error sample decays rather than spiking.
        PIDController controller = new PIDController(1.0, 0.0, 0.5);

        double first = controller.calculateFromError(0.2, 0.02);
        double second = controller.calculateFromError(0.0, 0.02);

        assertTrue(first > 0.2);
        assertTrue(second < first);
    }

    @Test
    void resetClearsStoredIntegratorAndDerivativeState() {
        // Passes if reset removes accumulated state so the next identical sample behaves like a fresh controller.
        PIDController controller = new PIDController(1.0, 1.0, 1.0);
        controller.setSetpoint(1.0);
        controller.calculate(0.0, 0.1);
        controller.reset();

        assertEquals(1.1, controller.calculate(0.0, 0.1), 1e-9);
    }

    @Test
    void feedforwardIsAddedToOutput() {
        PIDController controller = new PIDController(1.0, 0.0, 0.0, 2.0); // kF = 2.0
        controller.setSetpoint(5.0);
        
        // Error is 5.0 - 0.0 = 5.0
        // P = 1.0 * 5.0 = 5.0
        // F = 2.0 * 5.0 = 10.0
        // Total = 15.0
        assertEquals(15.0, controller.calculate(0.0, 0.1), 1e-9);
    }

    @Test
    void continuousInputWrapsCorrectly() {
        PIDController controller = new PIDController(1.0, 0.0, 0.0);
        controller.enableContinuousInput(-180, 180);
        
        controller.setSetpoint(-170);
        
        // Current is 170. Error is -170 - 170 = -340. Wrapped error = 20.
        // P = 1.0 * 20 = 20.0
        assertEquals(20.0, controller.calculate(170, 0.1), 1e-9);
    }

    @Test
    void integralZonePreventsWindupOutsideOfZone() {
        PIDController controller = new PIDController(0.0, 1.0, 0.0);
        controller.setIZone(2.0);
        controller.setSetpoint(5.0);
        
        // Error = 5.0. This is outside I-Zone (2.0), so integral should not accumulate.
        double first = controller.calculate(0.0, 0.1);
        assertEquals(0.0, first, 1e-9);
        
        // Now error is 1.0. This is inside I-Zone.
        double second = controller.calculate(4.0, 0.1);
        assertEquals(0.1, second, 1e-9); // I = 1.0 * (1.0 * 0.1) = 0.1
    }

    @Test
    void outputIsClampedToMinAndMax() {
        PIDController controller = new PIDController(1.0, 0.0, 0.0);
        controller.setOutputRange(-0.5, 0.5);
        controller.setSetpoint(10.0);
        
        // Error is 10.0. P = 10.0. Clamped to 0.5.
        assertEquals(0.5, controller.calculate(0.0, 0.1), 1e-9);
        
        controller.setSetpoint(-10.0);
        // Error is -10.0. P = -10.0. Clamped to -0.5.
        assertEquals(-0.5, controller.calculate(0.0, 0.1), 1e-9);
    }

    @Test
    void atSetpointWorksWithTolerances() {
        PIDController controller = new PIDController(1.0, 0.0, 0.0);
        controller.setTolerance(0.5); // Position tolerance only for this test, velocity default is infinite
        
        controller.setSetpoint(10.0);
        controller.calculate(0.0, 0.1);
        assertTrue(!controller.atSetpoint(), "Should not be at setpoint when far away");
        
        controller.calculate(9.6, 0.1); // Error is 0.4
        assertTrue(controller.atSetpoint(), "Should be at setpoint when within tolerance");
    }
}
