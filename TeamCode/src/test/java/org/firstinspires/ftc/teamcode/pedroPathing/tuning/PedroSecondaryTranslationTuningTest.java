package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PedroSecondaryTranslationTuningTest {
    @Test
    void snapshotReflectsSecondaryTranslationValues() {
        PedroTuneSnapshots.PidfSnapshot snapshot = PedroSecondaryTranslationTuning.snapshot();
        assertFalse(snapshot.enabled);
        assertEquals(PedroSecondaryTranslationTuning.D, snapshot.d, 1e-9);
    }
}
