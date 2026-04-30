package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PedroPathControlTuningTest {
    @Test
    void snapshotReflectsPathControlValues() {
        PedroTuneSnapshots.PathControlSnapshot snapshot = PedroPathControlTuning.snapshot();
        assertEquals(PedroPathControlTuning.CENTRIPETAL_SCALING, snapshot.centripetalScaling, 1e-9);
        assertEquals(PedroPathControlTuning.BEZIER_CURVE_SEARCH_LIMIT, snapshot.bezierCurveSearchLimit);
    }
}
