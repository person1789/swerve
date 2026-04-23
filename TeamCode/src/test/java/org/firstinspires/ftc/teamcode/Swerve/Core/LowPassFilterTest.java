package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LowPassFilterTest {

    @Test
    void calculateAppliesExponentialMovingAverage() {
        // Passes if each output is the weighted blend of the new input and the previous output.
        LowPassFilter filter = new LowPassFilter(0.25);

        assertEquals(2.5, filter.calculate(10.0), 1e-9);
        assertEquals(4.375, filter.calculate(10.0), 1e-9);
    }

    @Test
    void resetAndSetAlphaImmediatelyAffectFutureSamples() {
        // Passes if reset seeds the internal state and a new alpha changes subsequent blending behavior.
        LowPassFilter filter = new LowPassFilter(0.5);
        filter.reset(8.0);
        filter.setAlpha(1.0);

        assertEquals(3.0, filter.calculate(3.0), 1e-9);
    }
}
