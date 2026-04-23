package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoopTimeEstimatorTest {

    @Test
    void firstLoopUsesConfiguredApproximation() {
        LoopTimeEstimator estimator = new LoopTimeEstimator(4);

        double dt = estimator.update(0.037);

        assertEquals(SwerveConfig.LOOP_TIME_SEC, dt, 1e-9);
    }

    @Test
    void laterLoopsUseRollingAverage() {
        LoopTimeEstimator estimator = new LoopTimeEstimator(4);

        estimator.update(0.030);
        double dt2 = estimator.update(0.030);
        double dt3 = estimator.update(0.010);

        assertEquals((0.020 + 0.030) / 2.0, dt2, 1e-9);
        assertEquals((0.020 + 0.030 + 0.010) / 3.0, dt3, 1e-9);
    }

    @Test
    void invalidMeasurementsReuseExistingAverage() {
        LoopTimeEstimator estimator = new LoopTimeEstimator(4);

        estimator.update(0.030);
        estimator.update(0.030);
        double dt = estimator.update(0.0);

        assertEquals((0.020 + 0.030 + 0.025) / 3.0, dt, 1e-9);
    }

    @Test
    void outlierMeasurementsAreIgnored() {
        LoopTimeEstimator estimator = new LoopTimeEstimator(4);

        estimator.update(0.020);
        estimator.update(0.020);
        double dt = estimator.update(0.200);

        assertEquals(0.020, dt, 1e-9);
    }
}
