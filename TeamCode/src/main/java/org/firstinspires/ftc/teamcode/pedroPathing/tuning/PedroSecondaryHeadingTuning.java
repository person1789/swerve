package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroSecondaryHeadingTuning {
    public static boolean ENABLED = false;
    public static double P = 0.6;
    public static double I = 0.0;
    public static double D = 0.02;
    public static double F = 0.0;

    private PedroSecondaryHeadingTuning() {
    }

    public static PedroTuneSnapshots.PidfSnapshot snapshot() {
        return new PedroTuneSnapshots.PidfSnapshot(ENABLED, P, I, D, F, 0.0);
    }
}
