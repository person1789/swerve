package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroPredictiveBrakingTuning {
    public static boolean ENABLED = false;
    public static double LINEAR = 1.0;
    public static double QUADRATIC_FRICTION = 0.0;
    public static double P = 1.0;
    public static double MAX_POWER = 1.0;

    private PedroPredictiveBrakingTuning() {
    }

    public static PedroTuneSnapshots.PredictiveBrakingSnapshot snapshot() {
        return new PedroTuneSnapshots.PredictiveBrakingSnapshot(ENABLED, LINEAR, QUADRATIC_FRICTION, P, MAX_POWER);
    }
}
