package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Primary Drive", group = "Pedro Tuning")
public class PedroPrimaryDriveAutoTune extends PedroPidGroupAutoTuneBase {
    @Override
    protected String getTunerName() {
        return "PrimaryDrive";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroPrimaryDriveTuning.P,
                PedroPrimaryDriveTuning.I,
                PedroPrimaryDriveTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroPrimaryDriveTuning.P = pid.p;
        PedroPrimaryDriveTuning.I = pid.i;
        PedroPrimaryDriveTuning.D = pid.d;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(36.0, 0.0, 0.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return summary.rmsTranslationErrorIn * 2.0
                + summary.finalTranslationErrorIn * 4.0
                + summary.translationOvershootIn * 2.0
                + summary.translationSettlingTimeSec * 1.5
                + (summary.meanSpeedInS <= 0.0 ? 100.0 : 20.0 / summary.meanSpeedInS)
                + summary.pinpointInvalidRatio * 20.0;
    }
}
