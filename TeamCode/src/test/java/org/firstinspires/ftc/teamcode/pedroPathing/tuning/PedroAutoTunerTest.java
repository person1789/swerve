package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PedroAutoTunerTest {
    @Test
    void summarizeComputesReasonableErrors() {
        List<PedroAutoTuner.Sample> samples = Arrays.asList(
                new PedroAutoTuner.Sample("forward", 0.0, 24.0, 0.0, 0.0, 20.0, 1.0, 2.0, 10.0, 0.0, 5.0,
                        4.0, 0.70, false, 13.2, true, 0, 20.0, 1.0, 2.0),
                new PedroAutoTuner.Sample("forward", 0.5, 24.0, 0.0, 0.0, 23.0, 0.5, 1.0, 8.0, 0.0, 2.0,
                        1.5, 0.86, true, 13.0, true, 0, 23.0, 0.5, 1.0),
                new PedroAutoTuner.Sample("forward", 1.0, 24.0, 0.0, 0.0, 24.0, 0.2, 0.5, 2.0, 0.0, 1.0,
                        0.2, 0.95, true, 12.9, false, 1, Double.NaN, Double.NaN, Double.NaN));

        PedroAutoTuner.PhaseSummary summary = PedroAutoTuner.summarize("forward", samples);
        assertEquals(3, summary.sampleCount);
        assertTrue(summary.rmsTranslationErrorIn > 0.0);
        assertTrue(summary.maxHeadingErrorDeg >= 1.0);
        assertTrue(summary.translationSettlingTimeSec >= 0.0);
        assertTrue(summary.integratedAbsoluteTranslationError >= 0.0);
    }

    @Test
    void recommendRaisesTranslationPWhenErrorsAreLarge() {
        PedroAutoTuner.PhaseSummary summary = new PedroAutoTuner.PhaseSummary(
                "forward", 10, 2.0,
                3.5, 5.0, 1.0,
                7.0, 12.0, 2.0,
                12.0, 20.0, 3,
                5.0, 24.0, 0.52, 0.18, 0.20, 12.8,
                1.5, 4.0, 1.2, 0.8, 5.0, 6.0);

        PedroAutoTuner.TuneRecommendations recommendations =
                PedroAutoTuner.recommend(Arrays.asList(summary));

        assertTrue(recommendations.primaryTranslationP > PedroPrimaryTranslationTuning.P);
        assertTrue(recommendations.primaryHeadingP > PedroPrimaryHeadingTuning.P);
        assertTrue(recommendations.primaryDriveP > PedroPrimaryDriveTuning.P);
    }

    @Test
    void recursiveSearchImprovesSyntheticScore() {
        PedroAutoTuner.PidVector start = new PedroAutoTuner.PidVector(0.10, 0.0, 0.01);
        PedroAutoTuner.SearchConfig config = new PedroAutoTuner.SearchConfig(3, 0.1, 0.01, 0.05, false);

        PedroAutoTuner.SearchResult result = PedroAutoTuner.recursiveSearch(start, config, new PedroAutoTuner.Evaluator() {
            @Override
            public double evaluate(PedroAutoTuner.PidVector pid) {
                return Math.pow(pid.p - 0.25, 2.0) + Math.pow(pid.d - 0.08, 2.0);
            }
        });

        assertTrue(result.bestScore < 0.01);
        assertTrue(result.bestPid.p > start.p);
        assertTrue(result.bestPid.d > start.d);
        assertTrue(result.evaluations > 1);
    }
}
