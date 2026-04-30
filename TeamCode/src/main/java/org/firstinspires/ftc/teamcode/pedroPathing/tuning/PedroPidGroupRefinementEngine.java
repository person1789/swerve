package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockRouteBuilder;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

import java.util.ArrayList;
import java.util.List;

public final class PedroPidGroupRefinementEngine {
    private final LinearOpMode opMode;
    private final Telemetry telemetry;
    private final PedroSwerveFactory.PedroRobot robot;
    private final Follower follower;
    private final Pose centerStartPose = new Pose(0.0, 0.0, 0.0);

    public interface GroupSpec {
        String name();
        PedroAutoTuner.PidVector currentPid();
        void applyPid(PedroAutoTuner.PidVector pid);
        PedroBlockCommand[] commands();
        double score(PedroAutoTuner.PhaseSummary summary);
        void prepare();
        void cleanup();
    }

    public PedroPidGroupRefinementEngine(LinearOpMode opMode, Telemetry telemetry,
                                         PedroSwerveFactory.PedroRobot robot, Follower follower) {
        this.opMode = opMode;
        this.telemetry = telemetry;
        this.robot = robot;
        this.follower = follower;
    }

    public PedroAutoTuner.SearchResult refine(GroupSpec spec, double velocityScale, int searchDepth,
                                              boolean tuneI, double extraSettleSec, double telemetryHz) {
        robot.stack.setPedroVelocityConstraintScale(velocityScale);
        spec.prepare();
        PedroAutoTuner.PidVector start = spec.currentPid();
        PedroAutoTuner.SearchConfig searchConfig = PedroAutoTuner.defaultSearch(start, tuneI);
        searchConfig = new PedroAutoTuner.SearchConfig(
                searchDepth,
                searchConfig.pStep,
                searchConfig.iStep,
                searchConfig.dStep,
                tuneI);

        try {
            return PedroAutoTuner.recursiveSearch(start, searchConfig, new PedroAutoTuner.Evaluator() {
                @Override
                public double evaluate(PedroAutoTuner.PidVector pid) {
                    spec.applyPid(pid);
                    PedroSwerveFactory.applyLiveTuning(follower);
                    PedroAutoTuner.PhaseSummary summary = runEvaluationPass(spec.name(), spec.commands(), extraSettleSec, telemetryHz);
                    double score = spec.score(summary);
                    emitTelemetry(spec.name(), velocityScale, pid, summary, score);
                    return score;
                }
            });
        } finally {
            spec.cleanup();
            robot.stack.setPedroVelocityConstraintScale(1.0);
            robot.stack.stopPedroDrive();
        }
    }

    private PedroAutoTuner.PhaseSummary runEvaluationPass(String name, PedroBlockCommand[] commands,
                                                          double extraSettleSec, double telemetryHz) {
        robot.stack.resetForStart();
        follower.setStartingPose(centerStartPose);
        follower.setPose(centerStartPose);
        robot.stack.setPedroPose(centerStartPose);

        PathChain path = PedroBlockRouteBuilder.build(follower, centerStartPose, commands);
        follower.followPath(path, true);

        List<PedroAutoTuner.Sample> samples = new ArrayList<PedroAutoTuner.Sample>();
        ElapsedTime timer = new ElapsedTime();
        double nextTelemetry = 0.0;
        Pose targetPose = commands[commands.length - 1].endPose();

        while (opMode.opModeIsActive() && follower.isBusy()) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();
            PedroAutoTuner.Sample sample = captureSample(name, timer.seconds(), targetPose);
            samples.add(sample);
            if (timer.seconds() >= nextTelemetry) {
                telemetry.addData("Auto Telemetry/Refine Group", name);
                telemetry.addData("Auto Telemetry/Distance Remaining", sample.distanceRemaining);
                telemetry.addData("Auto Telemetry/Translation Authority", sample.translationAuthority);
                telemetry.update();
                nextTelemetry = timer.seconds() + (1.0 / telemetryHz);
            }
        }

        double settleStart = timer.seconds();
        while (opMode.opModeIsActive() && timer.seconds() - settleStart < extraSettleSec) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();
            samples.add(captureSample(name, timer.seconds(), targetPose));
        }

        follower.breakFollowing();
        robot.stack.stopPedroDrive();
        return PedroAutoTuner.summarize(name, samples);
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

    private void emitTelemetry(String name, double velocityScale, PedroAutoTuner.PidVector pid,
                               PedroAutoTuner.PhaseSummary summary, double score) {
        telemetry.addData("Auto Telemetry/Refine Group", name);
        telemetry.addData("Auto Telemetry/Velocity Scale", velocityScale);
        telemetry.addData("Auto Telemetry/Candidate P", pid.p);
        telemetry.addData("Auto Telemetry/Candidate I", pid.i);
        telemetry.addData("Auto Telemetry/Candidate D", pid.d);
        telemetry.addData("Auto Telemetry/Candidate Score", score);
        telemetry.addData("Auto Telemetry/Translation Overshoot In", summary.translationOvershootIn);
        telemetry.addData("Auto Telemetry/Translation Settle Sec", summary.translationSettlingTimeSec);
        telemetry.addData("Auto Telemetry/Heading Overshoot Deg", summary.headingOvershootDeg);
        telemetry.addData("Auto Telemetry/Heading Settle Sec", summary.headingSettlingTimeSec);
        telemetry.addData("Auto Telemetry/IAE Translation", summary.integratedAbsoluteTranslationError);
        telemetry.addData("Auto Telemetry/IAE Heading", summary.integratedAbsoluteHeadingError);
        telemetry.update();
    }
}
