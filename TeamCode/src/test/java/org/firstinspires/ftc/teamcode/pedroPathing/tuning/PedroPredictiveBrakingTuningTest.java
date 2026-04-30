package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PedroPredictiveBrakingTuningTest {
    @Test
    void snapshotReflectsPredictiveBrakingValues() {
        PedroTuneSnapshots.PredictiveBrakingSnapshot snapshot = PedroPredictiveBrakingTuning.snapshot();
        assertFalse(snapshot.enabled);
        assertEquals(PedroPredictiveBrakingTuning.MAX_POWER, snapshot.maxPower, 1e-9);
    }
}
