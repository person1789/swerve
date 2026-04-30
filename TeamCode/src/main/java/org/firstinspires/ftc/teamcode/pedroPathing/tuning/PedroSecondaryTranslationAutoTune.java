package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Secondary Translation", group = "Pedro Tuning")
public class PedroSecondaryTranslationAutoTune extends PedroPidGroupAutoTuneBase {
    private double originalSwitch;

    @Override
    protected String getTunerName() {
        return "SecondaryTranslation";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroSecondaryTranslationTuning.P,
                PedroSecondaryTranslationTuning.I,
                PedroSecondaryTranslationTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroSecondaryTranslationTuning.ENABLED = true;
        PedroSecondaryTranslationTuning.P = pid.p;
        PedroSecondaryTranslationTuning.I = pid.i;
        PedroSecondaryTranslationTuning.D = pid.d;
    }

    @Override
    protected void prepareGroupForTuning() {
        originalSwitch = PedroPrimaryTranslationTuning.SWITCH;
        PedroSecondaryTranslationTuning.ENABLED = true;
        PedroPrimaryTranslationTuning.SWITCH = 999.0;
    }

    @Override
    protected void cleanupGroupAfterTuning() {
        PedroPrimaryTranslationTuning.SWITCH = originalSwitch;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(8.0, 0.0, 0.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return summary.finalTranslationErrorIn * 8.0
                + summary.rmsTranslationErrorIn * 2.0
                + summary.translationOvershootIn * 2.5
                + summary.translationSettlingTimeSec * 3.0
                + summary.meanDistanceRemainingIn
                + summary.pinpointInvalidRatio * 20.0;
    }
}
