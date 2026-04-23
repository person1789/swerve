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
}
