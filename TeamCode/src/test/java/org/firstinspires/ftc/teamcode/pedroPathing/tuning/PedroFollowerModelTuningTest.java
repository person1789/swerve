package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PedroFollowerModelTuningTest {
    @Test
    void snapshotReflectsModelValues() {
        PedroTuneSnapshots.ModelSnapshot snapshot = PedroFollowerModelTuning.snapshot();
        assertEquals(PedroFollowerModelTuning.MASS, snapshot.mass, 1e-9);
        assertEquals(PedroFollowerModelTuning.FORWARD_ZERO_POWER_ACCELERATION, snapshot.forwardZeroPowerAcceleration, 1e-9);
        assertEquals(PedroFollowerModelTuning.LATERAL_ZERO_POWER_ACCELERATION, snapshot.lateralZeroPowerAcceleration, 1e-9);
    }
}
