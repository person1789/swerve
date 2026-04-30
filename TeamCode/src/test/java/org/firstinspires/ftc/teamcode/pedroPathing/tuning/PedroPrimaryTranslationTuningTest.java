package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PedroPrimaryTranslationTuningTest {
    @Test
    void snapshotReflectsPrimaryTranslationValues() {
        PedroTuneSnapshots.PidfSnapshot snapshot = PedroPrimaryTranslationTuning.snapshot();
        assertTrue(snapshot.enabled);
        assertEquals(PedroPrimaryTranslationTuning.P, snapshot.p, 1e-9);
        assertEquals(PedroPrimaryTranslationTuning.SWITCH, snapshot.switchThreshold, 1e-9);
    }
}
