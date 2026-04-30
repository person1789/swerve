package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;

@Autonomous(name = "Pedro Tune Primary Heading", group = "Pedro Tuning")
public class PedroPrimaryHeadingAutoTune extends PedroPidGroupAutoTuneBase {
    @Override
    protected String getTunerName() {
        return "PrimaryHeading";
    }

    @Override
    protected PedroAutoTuner.PidVector getCurrentPid() {
        return new PedroAutoTuner.PidVector(
                PedroPrimaryHeadingTuning.P,
                PedroPrimaryHeadingTuning.I,
                PedroPrimaryHeadingTuning.D);
    }

    @Override
    protected void applyPid(PedroAutoTuner.PidVector pid) {
        PedroPrimaryHeadingTuning.P = pid.p;
        PedroPrimaryHeadingTuning.I = pid.i;
        PedroPrimaryHeadingTuning.D = pid.d;
    }

    @Override
    protected PedroBlockCommand[] buildTestCommands() {
        return new PedroBlockCommand[] {
                PedroBlockCommand.straight(0.0, 0.0, 90.0, 1.0)
        };
    }

    @Override
    protected double scoreSummary(PedroAutoTuner.PhaseSummary summary) {
        return summary.rmsHeadingErrorDeg * 2.5
                + Math.abs(summary.finalHeadingErrorDeg) * 5.0
                + summary.headingOvershootDeg * 3.0
                + summary.headingSettlingTimeSec * 2.0
                + summary.integratedAbsoluteHeadingError * 0.2
                + summary.headingSignChanges * 2.0
                + summary.pinpointInvalidRatio * 20.0;
    }
}
