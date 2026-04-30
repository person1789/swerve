package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockRouteBuilder;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

import java.util.ArrayList;
import java.util.List;

@Config
public abstract class PedroPidGroupAutoTuneBase extends LinearOpMode {
    public static boolean APPLY_BEST_PID = true;
    public static boolean TUNE_I_TERM = true;
    public static int SEARCH_DEPTH = 3;
    public static double VELOCITY_SCALE = 0.5;
    public static double EXTRA_SETTLE_SEC = 0.35;
    public static double LOOP_TELEMETRY_HZ = 8.0;

    protected PedroSwerveFactory.PedroRobot robot;
    protected Follower follower;
    protected final Pose centerStartPose = new Pose(0.0, 0.0, 0.0);

    @Override
    public final void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        follower = robot.follower;
        PedroSwerveFactory.applyLiveTuning(follower);

        robot.stack.resetForStart();
        follower.setStartingPose(centerStartPose);
        follower.setPose(centerStartPose);
        robot.stack.setPedroPose(centerStartPose);
        robot.stack.setPedroVelocityConstraintScale(VELOCITY_SCALE);

        telemetry.addLine(getTunerName() + " ready");
        telemetry.addData("Auto Telemetry/Velocity Scale", VELOCITY_SCALE);
        telemetry.addData("Auto Telemetry/Search Depth", SEARCH_DEPTH);
        telemetry.addData("Auto Telemetry/Tune I", TUNE_I_TERM);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        prepareGroupForTuning();
        PedroAutoTuner.PidVector startingPid = getCurrentPid();
        PedroAutoTuner.SearchConfig searchConfig = PedroAutoTuner.defaultSearch(startingPid, TUNE_I_TERM);
        searchConfig = new PedroAutoTuner.SearchConfig(
                SEARCH_DEPTH,
                searchConfig.pStep,
                searchConfig.iStep,
                searchConfig.dStep,
                TUNE_I_TERM);

        PedroAutoTuner.SearchResult result;
        final PedroAutoTuner.PhaseSummary[] lastSummaryHolder = new PedroAutoTuner.PhaseSummary[1];
        try {
            result = PedroAutoTuner.recursiveSearch(startingPid, searchConfig, new PedroAutoTuner.Evaluator() {
                @Override
                public double evaluate(PedroAutoTuner.PidVector pid) {
                    applyPid(pid);
                    PedroSwerveFactory.applyLiveTuning(follower);
                    PedroAutoTuner.PhaseSummary summary = runEvaluationPass(pid);
                    lastSummaryHolder[0] = summary;
                    double score = scoreSummary(summary);
                    emitCandidateSummaryTelemetry(pid, summary, score);
                    return score;
                }
            });
        } finally {
            cleanupGroupAfterTuning();
        }

        if (APPLY_BEST_PID) {
            applyPid(result.bestPid);
        } else {
            applyPid(startingPid);
        }
        PedroSwerveFactory.applyLiveTuning(follower);
        robot.stack.setPedroVelocityConstraintScale(1.0);
        robot.stack.stopPedroDrive();

        telemetry.addLine(getTunerName() + " complete");
        telemetry.addData("Auto Telemetry/Best P", result.bestPid.p);
        telemetry.addData("Auto Telemetry/Best I", result.bestPid.i);
        telemetry.addData("Auto Telemetry/Best D", result.bestPid.d);
        telemetry.addData("Auto Telemetry/Best Score", result.bestScore);
        telemetry.addData("Auto Telemetry/Evaluations", result.evaluations);
        telemetry.addData("Auto Telemetry/Applied", APPLY_BEST_PID);
        telemetry.addData("Auto Telemetry/Velocity Scale", VELOCITY_SCALE);
        if (lastSummaryHolder[0] != null) {
            emitFinalSummaryTelemetry(lastSummaryHolder[0]);
        }
        telemetry.update();

        sleep(1500);
    }

    protected abstract String getTunerName();

    protected abstract PedroAutoTuner.PidVector getCurrentPid();

    protected abstract void applyPid(PedroAutoTuner.PidVector pid);

    protected abstract PedroBlockCommand[] buildTestCommands();

    protected abstract double scoreSummary(PedroAutoTuner.PhaseSummary summary);

    protected void prepareGroupForTuning() {
    }

    protected void cleanupGroupAfterTuning() {
    }

    protected PedroAutoTuner.PhaseSummary runEvaluationPass(PedroAutoTuner.PidVector pid) {
        robot.stack.resetForStart();
        follower.setStartingPose(centerStartPose);
        follower.setPose(centerStartPose);
        robot.stack.setPedroPose(centerStartPose);

        PathChain path = PedroBlockRouteBuilder.build(follower, centerStartPose, buildTestCommands());
        follower.followPath(path, true);

        List<PedroAutoTuner.Sample> samples = new ArrayList<PedroAutoTuner.Sample>();
        ElapsedTime timer = new ElapsedTime();
        double nextTelemetryTime = 0.0;
        Pose targetPose = buildTestCommands()[buildTestCommands().length - 1].endPose();

        while (opModeIsActive() && follower.isBusy()) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();
            PedroAutoTuner.Sample sample = captureSample(getTunerName(), timer.seconds(), targetPose);
            samples.add(sample);

            if (timer.seconds() >= nextTelemetryTime) {
                emitLoopTelemetry(pid, sample);
                nextTelemetryTime = timer.seconds() + (1.0 / LOOP_TELEMETRY_HZ);
            }
        }

        double settleStart = timer.seconds();
        while (opModeIsActive() && timer.seconds() - settleStart < EXTRA_SETTLE_SEC) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();
            samples.add(captureSample(getTunerName(), timer.seconds(), targetPose));
        }

        follower.breakFollowing();
        robot.stack.stopPedroDrive();
        return PedroAutoTuner.summarize(getTunerName(), samples);
    }

    private PedroAutoTuner.Sample captureSample(String phaseName, double timeSec, Pose targetPose) {
        Pose currentPose = follower.getPose();
        Pose currentVelocity = robot.stack.getPedroVelocityPose();
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector rawPinpoint = robot.stack.getLocalizer().getRawPinpointPose();
        return new PedroAutoTuner.Sample(
                phaseName,
                timeSec,
                targetPose.getX(),
                targetPose.getY(),
                Math.toDegrees(targetPose.getHeading()),
                currentPose.getX(),
                currentPose.getY(),
                Math.toDegrees(currentPose.getHeading()),
                currentVelocity.getX(),
                currentVelocity.getY(),
                Math.toDegrees(currentVelocity.getHeading()),
                follower.getDistanceRemaining(),
                robot.stack.getDrivetrain().getLastTranslationAuthority(),
                robot.stack.getDrivetrain().isSteerReadyForDrive(),
                robot.stack.getDrivetrain().getBatteryVoltage(),
                robot.stack.getLocalizer().isUsingPinpoint(),
                robot.stack.getLocalizer().getConsecutiveInvalidPinpointLoops(),
                rawPinpoint.x(),
                rawPinpoint.y(),
                Math.toDegrees(rawPinpoint.omega()));
    }

    private void emitLoopTelemetry(PedroAutoTuner.PidVector pid, PedroAutoTuner.Sample sample) {
        telemetry.addData("Auto Telemetry/Tuner", getTunerName());
        telemetry.addData("Auto Telemetry/P", pid.p);
        telemetry.addData("Auto Telemetry/I", pid.i);
        telemetry.addData("Auto Telemetry/D", pid.d);
        telemetry.addData("Auto Telemetry/Velocity Scale", robot.stack.getPedroVelocityConstraintScale());
        telemetry.addData("Auto Telemetry/Error In",
                Math.hypot(sample.targetXIn - sample.actualXIn, sample.targetYIn - sample.actualYIn));
        telemetry.addData("Auto Telemetry/Heading Error Deg",
                angleErrorDeg(sample.targetHeadingDeg, sample.actualHeadingDeg));
        telemetry.addData("Auto Telemetry/Distance Remaining", sample.distanceRemaining);
        telemetry.addData("Auto Telemetry/Translation Authority", sample.translationAuthority);
        telemetry.addData("Auto Telemetry/Steer Ready", sample.steerReady);
        telemetry.addData("Auto Telemetry/Pinpoint Used", sample.usingPinpoint);
        telemetry.addData("Auto Telemetry/Pinpoint Invalid Loops", sample.invalidPinpointLoops);
        telemetry.update();
    }

    private void emitCandidateSummaryTelemetry(PedroAutoTuner.PidVector pid, PedroAutoTuner.PhaseSummary summary, double score) {
        telemetry.addData("Auto Telemetry/Candidate P", pid.p);
        telemetry.addData("Auto Telemetry/Candidate I", pid.i);
        telemetry.addData("Auto Telemetry/Candidate D", pid.d);
        telemetry.addData("Auto Telemetry/Candidate Score", score);
        telemetry.addData("Auto Telemetry/RMS Translation In", summary.rmsTranslationErrorIn);
        telemetry.addData("Auto Telemetry/Final Translation In", summary.finalTranslationErrorIn);
        telemetry.addData("Auto Telemetry/Translation Overshoot In", summary.translationOvershootIn);
        telemetry.addData("Auto Telemetry/Translation Settle Sec", summary.translationSettlingTimeSec);
        telemetry.addData("Auto Telemetry/IAE Translation", summary.integratedAbsoluteTranslationError);
        telemetry.addData("Auto Telemetry/RMS Heading Deg", summary.rmsHeadingErrorDeg);
        telemetry.addData("Auto Telemetry/Final Heading Deg", summary.finalHeadingErrorDeg);
        telemetry.addData("Auto Telemetry/Heading Overshoot Deg", summary.headingOvershootDeg);
        telemetry.addData("Auto Telemetry/Heading Settle Sec", summary.headingSettlingTimeSec);
        telemetry.addData("Auto Telemetry/IAE Heading", summary.integratedAbsoluteHeadingError);
        telemetry.addData("Auto Telemetry/Peak Speed In S", summary.peakSpeedInS);
        telemetry.addData("Auto Telemetry/Mean Speed In S", summary.meanSpeedInS);
        telemetry.addData("Auto Telemetry/Mean Authority", summary.meanTranslationAuthority);
        telemetry.addData("Auto Telemetry/Min Authority", summary.minTranslationAuthority);
        telemetry.addData("Auto Telemetry/Pinpoint Invalid Ratio", summary.pinpointInvalidRatio);
        telemetry.addData("Auto Telemetry/Heading Sign Changes", summary.headingSignChanges);
        telemetry.update();
    }

    private void emitFinalSummaryTelemetry(PedroAutoTuner.PhaseSummary summary) {
        telemetry.addData("Auto Telemetry/Best RMS Translation In", summary.rmsTranslationErrorIn);
        telemetry.addData("Auto Telemetry/Best Final Translation In", summary.finalTranslationErrorIn);
        telemetry.addData("Auto Telemetry/Best Translation Overshoot In", summary.translationOvershootIn);
        telemetry.addData("Auto Telemetry/Best Translation Settle Sec", summary.translationSettlingTimeSec);
        telemetry.addData("Auto Telemetry/Best IAE Translation", summary.integratedAbsoluteTranslationError);
        telemetry.addData("Auto Telemetry/Best RMS Heading Deg", summary.rmsHeadingErrorDeg);
        telemetry.addData("Auto Telemetry/Best Final Heading Deg", summary.finalHeadingErrorDeg);
        telemetry.addData("Auto Telemetry/Best Heading Overshoot Deg", summary.headingOvershootDeg);
        telemetry.addData("Auto Telemetry/Best Heading Settle Sec", summary.headingSettlingTimeSec);
        telemetry.addData("Auto Telemetry/Best IAE Heading", summary.integratedAbsoluteHeadingError);
        telemetry.addData("Auto Telemetry/Best Mean Speed In S", summary.meanSpeedInS);
        telemetry.addData("Auto Telemetry/Best Peak Speed In S", summary.peakSpeedInS);
        telemetry.addData("Auto Telemetry/Best Mean Authority", summary.meanTranslationAuthority);
        telemetry.addData("Auto Telemetry/Best Pinpoint Invalid Ratio", summary.pinpointInvalidRatio);
    }

    private static double angleErrorDeg(double targetDeg, double actualDeg) {
        double error = targetDeg - actualDeg;
        while (error > 180.0) {
            error -= 360.0;
        }
        while (error < -180.0) {
            error += 360.0;
        }
        return error;
    }
}
