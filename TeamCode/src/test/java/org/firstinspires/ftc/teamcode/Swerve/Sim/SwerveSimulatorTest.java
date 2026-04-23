package org.firstinspires.ftc.teamcode.Swerve.Sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SwerveSimulatorTest {

    @Test
    void forwardInputProducesNonZeroModuleVelocityAndDrivePower() {
        // Passes if a sustained forward command through the simulator pipeline causes at least one module to report non-zero target velocity and drive power.
        SwerveSimulator simulator = new SwerveSimulator();
        BrowserGamepadState input = new BrowserGamepadState();
        input.leftY = -1.0;
        input.connected = true;

        simulator.updateInput(input);
        for (int i = 0; i < 10; i++) {
            simulator.step(0.02);
        }

        Map<String, Object> snapshot = simulator.snapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) snapshot.get("modules");

        boolean anyMoving = modules.stream().anyMatch(module ->
                Math.abs(((Number) module.get("targetVelocityMps")).doubleValue()) > 1e-6
                        && Math.abs(((Number) module.get("drivePower")).doubleValue()) > 1e-6);

        assertTrue(anyMoving);
    }
}
