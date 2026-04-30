package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Secondary Drive", group = "Pedro Tuning")
public class PedroSecondaryDriveAutoTune extends PedroPidGroupAutoTuneBase {
    private double originalSwitch;

    @Override
    protected String getTunerName() {
        return "SecondaryDrive";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroSecondaryDriveTuning.P,
                PedroSecondaryDriveTuning.I,
                PedroSecondaryDriveTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroSecondaryDriveTuning.ENABLED = true;
        PedroSecondaryDriveTuning.P = pid.p;
        PedroSecondaryDriveTuning.I = pid.i;
        PedroSecondaryDriveTuning.D = pid.d;
    }

    @Override
    protected void prepareGroupForTuning() {
        originalSwitch = PedroPrimaryDriveTuning.SWITCH;
        PedroSecondaryDriveTuning.ENABLED = true;
        PedroPrimaryDriveTuning.SWITCH = 999.0;
    }

    @Override
    protected void cleanupGroupAfterTuning() {
        PedroPrimaryDriveTuning.SWITCH = originalSwitch;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(10.0, 0.0, 0.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return summary.finalTranslationErrorIn * 6.0
                + summary.meanDistanceRemainingIn * 1.5
                + summary.rmsTranslationErrorIn * 1.5
                + summary.translationOvershootIn * 2.0
                + summary.translationSettlingTimeSec * 2.0
                + summary.pinpointInvalidRatio * 20.0;
    }
}
