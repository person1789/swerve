package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Secondary Heading", group = "Pedro Tuning")
public class PedroSecondaryHeadingAutoTune extends PedroPidGroupAutoTuneBase {
    private double originalSwitchRad;

    @Override
    protected String getTunerName() {
        return "SecondaryHeading";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroSecondaryHeadingTuning.P,
                PedroSecondaryHeadingTuning.I,
                PedroSecondaryHeadingTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroSecondaryHeadingTuning.ENABLED = true;
        PedroSecondaryHeadingTuning.P = pid.p;
        PedroSecondaryHeadingTuning.I = pid.i;
        PedroSecondaryHeadingTuning.D = pid.d;
    }

    @Override
    protected void prepareGroupForTuning() {
        originalSwitchRad = PedroPrimaryHeadingTuning.SWITCH_RAD;
        PedroSecondaryHeadingTuning.ENABLED = true;
        PedroPrimaryHeadingTuning.SWITCH_RAD = Math.toRadians(180.0);
    }

    @Override
    protected void cleanupGroupAfterTuning() {
        PedroPrimaryHeadingTuning.SWITCH_RAD = originalSwitchRad;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(0.0, 0.0, 20.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return Math.abs(summary.finalHeadingErrorDeg) * 7.0
                + summary.rmsHeadingErrorDeg * 2.0
                + summary.headingOvershootDeg * 3.5
                + summary.headingSettlingTimeSec * 2.5
                + summary.headingSignChanges * 3.0
                + summary.pinpointInvalidRatio * 20.0;
    }
}
