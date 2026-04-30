package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PedroBootstrapAndRefineScoringTest {
    @Test
    void betterSettlingScoresLowerThanWorseSettlingForTranslationLikeSummary() {
        PedroAutoTuner.PhaseSummary better = new PedroAutoTuner.PhaseSummary(
                "PrimaryTranslation", 10, 1.0,
                1.0, 2.0, 0.4,
                1.0, 2.0, 0.3,
                10.0, 12.0, 0,
                1.0, 4.0, 0.9, 0.6, 0.0, 12.5,
                0.3, 0.5, 0.4, 0.3, 0.8, 0.6);

        PedroAutoTuner.PhaseSummary worse = new PedroAutoTuner.PhaseSummary(
                "PrimaryTranslation", 10, 1.0,
                2.5, 5.0, 1.8,
                2.0, 5.0, 1.5,
                8.0, 10.0, 1,
                3.0, 8.0, 0.6, 0.2, 0.1, 12.2,
                1.8, 2.5, 1.6, 1.4, 4.0, 2.5);

        double betterScore = better.rmsTranslationErrorIn * 3.0 + better.finalTranslationErrorIn * 5.0
                + better.translationOvershootIn * 3.0 + better.translationSettlingTimeSec * 2.0
                + better.integratedAbsoluteTranslationError * 0.35 + better.pinpointInvalidRatio * 20.0;
        double worseScore = worse.rmsTranslationErrorIn * 3.0 + worse.finalTranslationErrorIn * 5.0
                + worse.translationOvershootIn * 3.0 + worse.translationSettlingTimeSec * 2.0
                + worse.integratedAbsoluteTranslationError * 0.35 + worse.pinpointInvalidRatio * 20.0;

        assertTrue(betterScore < worseScore);
    }
}
