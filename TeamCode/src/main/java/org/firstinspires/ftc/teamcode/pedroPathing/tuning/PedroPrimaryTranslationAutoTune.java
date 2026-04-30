package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Primary Translation", group = "Pedro Tuning")
public class PedroPrimaryTranslationAutoTune extends PedroPidGroupAutoTuneBase {
    @Override
    protected String getTunerName() {
        return "PrimaryTranslation";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroPrimaryTranslationTuning.P,
                PedroPrimaryTranslationTuning.I,
                PedroPrimaryTranslationTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroPrimaryTranslationTuning.P = pid.p;
        PedroPrimaryTranslationTuning.I = pid.i;
        PedroPrimaryTranslationTuning.D = pid.d;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(24.0, 0.0, 0.0, 1.0),
                PedroBlockCommand.straight(24.0, 24.0, 0.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return summary.rmsTranslationErrorIn * 3.0
                + summary.finalTranslationErrorIn * 5.0
                + summary.translationOvershootIn * 3.0
                + summary.translationSettlingTimeSec * 2.0
                + summary.integratedAbsoluteTranslationError * 0.35
                + summary.pinpointInvalidRatio * 20.0;
    }
}
