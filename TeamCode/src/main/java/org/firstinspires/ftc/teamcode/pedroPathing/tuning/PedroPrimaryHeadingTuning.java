package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroPrimaryHeadingTuning {
    public static boolean ENABLED = true;
    public static double P = 1.2;
    public static double I = 0.0;
    public static double D = 0.03;
    public static double F = 0.0;
    public static double SWITCH_RAD = Math.toRadians(15.0);

    private PedroPrimaryHeadingTuning() {
    }

    public static PedroTuneSnapshots.PidfSnapshot snapshot() {
        return new PedroTuneSnapshots.PidfSnapshot(ENABLED, P, I, D, F, SWITCH_RAD);
    }
}
