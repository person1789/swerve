package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PedroBootstrapIdentifierTest {
    @Test
    void summarizeForwardExtractsBasicMotionFeatures() {
        List<PedroBootstrapIdentifier.MotionSample> samples = Arrays.asList(
                new PedroBootstrapIdentifier.MotionSample("step", 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("step", 0.1, 0.5, 0.0, 0.0, 4.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("step", 0.2, 0.5, 0.0, 0.0, 9.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("step", 0.3, 0.5, 0.0, 0.0, 12.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("release", 0.4, 0.0, 0.0, 0.0, 8.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("release", 0.5, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0),
                new PedroBootstrapIdentifier.MotionSample("release", 0.6, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0));

        PedroBootstrapIdentifier.AxisSummary summary =
                PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.FORWARD, samples);

        assertTrue(summary.maxVelocity >= 12.0);
        assertTrue(summary.accelRate > 0.0);
        assertTrue(summary.brakeRate > 0.0);
        assertTrue(summary.riseTimeSec >= 0.0);
    }

    @Test
    void seedFromIdentificationProducesNonZeroStartingGains() {
        PedroBootstrapIdentifier.AxisSummary forward =
                new PedroBootstrapIdentifier.AxisSummary(PedroBootstrapIdentifier.Axis.FORWARD, 0.1, 24.0, 40.0, 32.0, 0.2);
        PedroBootstrapIdentifier.AxisSummary lateral =
                new PedroBootstrapIdentifier.AxisSummary(PedroBootstrapIdentifier.Axis.LATERAL, 0.1, 20.0, 34.0, 28.0, 0.25);
        PedroBootstrapIdentifier.AxisSummary turn =
                new PedroBootstrapIdentifier.AxisSummary(PedroBootstrapIdentifier.Axis.TURN, 0.08, 2.5, 6.0, 4.5, 0.18);

        PedroBootstrapIdentifier.BootstrapSeeds seeds =
                PedroBootstrapIdentifier.seedFromIdentification(forward, lateral, turn);

        assertTrue(seeds.primaryTranslationP > 0.0);
        assertTrue(seeds.primaryTranslationD > 0.0);
        assertTrue(seeds.primaryHeadingP > 0.0);
        assertTrue(seeds.primaryDriveP > 0.0);
        assertTrue(seeds.centripetalScaling > 0.0);
    }
}
