package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroSecondaryTranslationTuning {
    public static boolean ENABLED = false;
    public static double P = 0.04;
    public static double I = 0.0;
    public static double D = 0.002;
    public static double F = 0.0;

    private PedroSecondaryTranslationTuning() {
    }

    public static PedroTuneSnapshots.PidfSnapshot snapshot() {
        return new PedroTuneSnapshots.PidfSnapshot(ENABLED, P, I, D, F, 0.0);
    }
}
