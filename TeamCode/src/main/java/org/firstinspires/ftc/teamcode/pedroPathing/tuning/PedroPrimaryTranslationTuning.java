package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroPrimaryTranslationTuning {
    public static boolean ENABLED = true;
    public static double P = 0.10;
    public static double I = 0.0;
    public static double D = 0.01;
    public static double F = 0.0;
    public static double SWITCH = 3.0;

    private PedroPrimaryTranslationTuning() {
    }

    public static PedroTuneSnapshots.PidfSnapshot snapshot() {
        return new PedroTuneSnapshots.PidfSnapshot(ENABLED, P, I, D, F, SWITCH);
    }
}
