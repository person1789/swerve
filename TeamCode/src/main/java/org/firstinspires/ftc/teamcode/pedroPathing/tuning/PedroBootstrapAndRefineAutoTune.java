package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

import java.util.ArrayList;
import java.util.List;

@Config
@Autonomous(name = "Pedro Bootstrap And Refine", group = "Pedro Tuning")
public class PedroBootstrapAndRefineAutoTune extends LinearOpMode {
    public static boolean APPLY_SEEDS = true;
    public static boolean APPLY_REFINED = true;
    public static boolean TUNE_I_TERM = true;
    public static boolean RUN_PRIMARY_TRANSLATION = true;
    public static boolean RUN_SECONDARY_TRANSLATION = true;
    public static boolean RUN_PRIMARY_HEADING = true;
    public static boolean RUN_SECONDARY_HEADING = true;
    public static boolean RUN_PRIMARY_DRIVE = true;
    public static boolean RUN_SECONDARY_DRIVE = true;
    public static double VELOCITY_SCALE = 0.5;
    public static double STEP_DURATION_SEC = 1.20;
    public static double RELEASE_DURATION_SEC = 0.80;
    public static int SEARCH_DEPTH = 3;
    public static double EXTRA_SETTLE_SEC = 0.35;
    public static double TELEMETRY_HZ = 8.0;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        PedroSwerveFactory.PedroRobot robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        Follower follower = robot.follower;
        PedroSwerveFactory.applyLiveTuning(follower);
        robot.stack.resetForStart();
        robot.stack.setPedroVelocityConstraintScale(VELOCITY_SCALE);

        telemetry.addLine("Pedro bootstrap + refine ready");
        telemetry.addData("Auto Telemetry/Velocity Scale", VELOCITY_SCALE);
        telemetry.addData("Auto Telemetry/Search Depth", SEARCH_DEPTH);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        PedroBootstrapIdentifier.BootstrapSeeds seeds = runBootstrap(robot);
        if (APPLY_SEEDS) {
            PedroBootstrapIdentifier.applySeeds(seeds);
            PedroSwerveFactory.applyLiveTuning(follower);
        }

        PedroPidGroupRefinementEngine engine = new PedroPidGroupRefinementEngine(this, telemetry, robot, follower);
        List<String> completedGroups = new ArrayList<String>();

        if (RUN_PRIMARY_TRANSLATION) {
            runRefinement(engine, primaryTranslationSpec(), completedGroups);
        }
        if (RUN_SECONDARY_TRANSLATION) {
            runRefinement(engine, secondaryTranslationSpec(), completedGroups);
        }
        if (RUN_PRIMARY_HEADING) {
            runRefinement(engine, primaryHeadingSpec(), completedGroups);
        }
        if (RUN_SECONDARY_HEADING) {
            runRefinement(engine, secondaryHeadingSpec(), completedGroups);
        }
        if (RUN_PRIMARY_DRIVE) {
            runRefinement(engine, primaryDriveSpec(), completedGroups);
        }
        if (RUN_SECONDARY_DRIVE) {
            runRefinement(engine, secondaryDriveSpec(), completedGroups);
        }

        if (!APPLY_REFINED) {
            PedroBootstrapIdentifier.applySeeds(seeds);
            PedroSwerveFactory.applyLiveTuning(follower);
        }

        robot.stack.setPedroVelocityConstraintScale(1.0);
        robot.stack.stopPedroDrive();
        telemetry.addLine("Pedro bootstrap + refine complete");
        telemetry.addData("Auto Telemetry/Completed Groups", completedGroups.toString());
        telemetry.addData("Auto Telemetry/Applied Seeds", APPLY_SEEDS);
        telemetry.addData("Auto Telemetry/Applied Refined", APPLY_REFINED);
        telemetry.update();
        sleep(2000);
    }

    private PedroBootstrapIdentifier.BootstrapSeeds runBootstrap(PedroSwerveFactory.PedroRobot robot) {
        List<PedroBootstrapIdentifier.MotionSample> forwardSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();
        List<PedroBootstrapIdentifier.MotionSample> lateralSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();
        List<PedroBootstrapIdentifier.MotionSample> turnSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();

        runBootstrapPhase(robot, "ForwardStep", VELOCITY_SCALE, 0.0, 0.0, STEP_DURATION_SEC, forwardSamples);
        runBootstrapPhase(robot, "ForwardRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, forwardSamples);
        runBootstrapPhase(robot, "LateralStep", 0.0, VELOCITY_SCALE, 0.0, STEP_DURATION_SEC, lateralSamples);
        runBootstrapPhase(robot, "LateralRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, lateralSamples);
        runBootstrapPhase(robot, "TurnStep", 0.0, 0.0, VELOCITY_SCALE, STEP_DURATION_SEC, turnSamples);
        runBootstrapPhase(robot, "TurnRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, turnSamples);

        PedroBootstrapIdentifier.AxisSummary forward = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.FORWARD, forwardSamples);
        PedroBootstrapIdentifier.AxisSummary lateral = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.LATERAL, lateralSamples);
        PedroBootstrapIdentifier.AxisSummary turn = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.TURN, turnSamples);
        PedroBootstrapIdentifier.BootstrapSeeds seeds = PedroBootstrapIdentifier.seedFromIdentification(forward, lateral, turn);

        telemetry.addData("Auto Telemetry/Seed Primary Translation P", seeds.primaryTranslationP);
        telemetry.addData("Auto Telemetry/Seed Primary Heading P", seeds.primaryHeadingP);
        telemetry.addData("Auto Telemetry/Seed Primary Drive P", seeds.primaryDriveP);
        telemetry.update();
        return seeds;
    }

    private void runBootstrapPhase(PedroSwerveFactory.PedroRobot robot, String name,
                                   double forward, double strafe, double turn, double durationSec,
                                   List<PedroBootstrapIdentifier.MotionSample> sink) {
        com.qualcomm.robotcore.util.ElapsedTime timer = new com.qualcomm.robotcore.util.ElapsedTime();
        double nextTelemetry = 0.0;
        while (opModeIsActive() && timer.seconds() < durationSec) {
            robot.stack.drivePedroRobotCentric(forward, strafe, turn);
            com.pedropathing.geometry.Pose measuredVelocity = robot.stack.getPedroVelocityPose();
            double now = timer.seconds();
            sink.add(new PedroBootstrapIdentifier.MotionSample(
                    name, now, forward, strafe, turn,
                    measuredVelocity.getX(), measuredVelocity.getY(), measuredVelocity.getHeading()));
            if (now >= nextTelemetry) {
                telemetry.addData("Auto Telemetry/Bootstrap Phase", name);
                telemetry.addData("Auto Telemetry/Vx In S", measuredVelocity.getX());
                telemetry.addData("Auto Telemetry/Vy In S", measuredVelocity.getY());
                telemetry.addData("Auto Telemetry/Omega Rad S", measuredVelocity.getHeading());
                telemetry.update();
                nextTelemetry = now + (1.0 / TELEMETRY_HZ);
            }
        }
    }

    private void runRefinement(PedroPidGroupRefinementEngine engine,
                               PedroPidGroupRefinementEngine.GroupSpec spec,
                               List<String> completedGroups) {
        PedroAutoTuner.SearchResult result = engine.refine(
                spec, VELOCITY_SCALE, SEARCH_DEPTH, TUNE_I_TERM, EXTRA_SETTLE_SEC, TELEMETRY_HZ);
        if (!APPLY_REFINED) {
            spec.cleanup();
        }
        telemetry.addData("Auto Telemetry/Refined " + spec.name(), result.bestScore);
        telemetry.update();
        completedGroups.add(spec.name());
    }

    private PedroPidGroupRefinementEngine.GroupSpec primaryTranslationSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            @Override public String name() { return "PrimaryTranslation"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroPrimaryTranslationTuning.P, PedroPrimaryTranslationTuning.I, PedroPrimaryTranslationTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroPrimaryTranslationTuning.P = pid.p; PedroPrimaryTranslationTuning.I = pid.i; PedroPrimaryTranslationTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() {
                return new PedroBlockCommand[] {
                        PedroBlockCommand.straight(24.0, 0.0, 0.0, 1.0),
                        PedroBlockCommand.straight(24.0, 24.0, 0.0, 1.0)
                };
            }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return s.rmsTranslationErrorIn * 3.0 + s.finalTranslationErrorIn * 5.0
                        + s.translationOvershootIn * 3.0 + s.translationSettlingTimeSec * 2.0
                        + s.integratedAbsoluteTranslationError * 0.35 + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() {}
            @Override public void cleanup() {}
        };
    }

    private PedroPidGroupRefinementEngine.GroupSpec secondaryTranslationSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            private double originalSwitch;
            @Override public String name() { return "SecondaryTranslation"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroSecondaryTranslationTuning.P, PedroSecondaryTranslationTuning.I, PedroSecondaryTranslationTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroSecondaryTranslationTuning.ENABLED = true;
                PedroSecondaryTranslationTuning.P = pid.p; PedroSecondaryTranslationTuning.I = pid.i; PedroSecondaryTranslationTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() { return new PedroBlockCommand[] { PedroBlockCommand.straight(8.0, 0.0, 0.0, 1.0) }; }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return s.finalTranslationErrorIn * 8.0 + s.rmsTranslationErrorIn * 2.0
                        + s.translationOvershootIn * 2.5 + s.translationSettlingTimeSec * 3.0
                        + s.meanDistanceRemainingIn + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() { originalSwitch = PedroPrimaryTranslationTuning.SWITCH; PedroSecondaryTranslationTuning.ENABLED = true; PedroPrimaryTranslationTuning.SWITCH = 999.0; }
            @Override public void cleanup() { PedroPrimaryTranslationTuning.SWITCH = originalSwitch; }
        };
    }

    private PedroPidGroupRefinementEngine.GroupSpec primaryHeadingSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            @Override public String name() { return "PrimaryHeading"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroPrimaryHeadingTuning.P, PedroPrimaryHeadingTuning.I, PedroPrimaryHeadingTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroPrimaryHeadingTuning.P = pid.p; PedroPrimaryHeadingTuning.I = pid.i; PedroPrimaryHeadingTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() { return new PedroBlockCommand[] { PedroBlockCommand.straight(0.0, 0.0, 90.0, 1.0) }; }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return s.rmsHeadingErrorDeg * 2.5 + Math.abs(s.finalHeadingErrorDeg) * 5.0
                        + s.headingOvershootDeg * 3.0 + s.headingSettlingTimeSec * 2.0
                        + s.integratedAbsoluteHeadingError * 0.2 + s.headingSignChanges * 2.0
                        + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() {}
            @Override public void cleanup() {}
        };
    }

    private PedroPidGroupRefinementEngine.GroupSpec secondaryHeadingSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            private double originalSwitchRad;
            @Override public String name() { return "SecondaryHeading"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroSecondaryHeadingTuning.P, PedroSecondaryHeadingTuning.I, PedroSecondaryHeadingTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroSecondaryHeadingTuning.ENABLED = true;
                PedroSecondaryHeadingTuning.P = pid.p; PedroSecondaryHeadingTuning.I = pid.i; PedroSecondaryHeadingTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() { return new PedroBlockCommand[] { PedroBlockCommand.straight(0.0, 0.0, 20.0, 1.0) }; }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return Math.abs(s.finalHeadingErrorDeg) * 7.0 + s.rmsHeadingErrorDeg * 2.0
                        + s.headingOvershootDeg * 3.5 + s.headingSettlingTimeSec * 2.5
                        + s.headingSignChanges * 3.0 + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() { originalSwitchRad = PedroPrimaryHeadingTuning.SWITCH_RAD; PedroSecondaryHeadingTuning.ENABLED = true; PedroPrimaryHeadingTuning.SWITCH_RAD = Math.toRadians(180.0); }
            @Override public void cleanup() { PedroPrimaryHeadingTuning.SWITCH_RAD = originalSwitchRad; }
        };
    }

    private PedroPidGroupRefinementEngine.GroupSpec primaryDriveSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            @Override public String name() { return "PrimaryDrive"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroPrimaryDriveTuning.P, PedroPrimaryDriveTuning.I, PedroPrimaryDriveTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroPrimaryDriveTuning.P = pid.p; PedroPrimaryDriveTuning.I = pid.i; PedroPrimaryDriveTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() { return new PedroBlockCommand[] { PedroBlockCommand.straight(36.0, 0.0, 0.0, 1.0) }; }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return s.rmsTranslationErrorIn * 2.0 + s.finalTranslationErrorIn * 4.0
                        + s.translationOvershootIn * 2.0 + s.translationSettlingTimeSec * 1.5
                        + (s.meanSpeedInS <= 0.0 ? 100.0 : 20.0 / s.meanSpeedInS) + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() {}
            @Override public void cleanup() {}
        };
    }

    private PedroPidGroupRefinementEngine.GroupSpec secondaryDriveSpec() {
        return new PedroPidGroupRefinementEngine.GroupSpec() {
            private double originalSwitch;
            @Override public String name() { return "SecondaryDrive"; }
            @Override public PedroAutoTuner.PidVector currentPid() {
                return new PedroAutoTuner.PidVector(PedroSecondaryDriveTuning.P, PedroSecondaryDriveTuning.I, PedroSecondaryDriveTuning.D);
            }
            @Override public void applyPid(PedroAutoTuner.PidVector pid) {
                PedroSecondaryDriveTuning.ENABLED = true;
                PedroSecondaryDriveTuning.P = pid.p; PedroSecondaryDriveTuning.I = pid.i; PedroSecondaryDriveTuning.D = pid.d;
            }
            @Override public PedroBlockCommand[] commands() { return new PedroBlockCommand[] { PedroBlockCommand.straight(10.0, 0.0, 0.0, 1.0) }; }
            @Override public double score(PedroAutoTuner.PhaseSummary s) {
                return s.finalTranslationErrorIn * 6.0 + s.meanDistanceRemainingIn * 1.5
                        + s.rmsTranslationErrorIn * 1.5 + s.translationOvershootIn * 2.0
                        + s.translationSettlingTimeSec * 2.0 + s.pinpointInvalidRatio * 20.0;
            }
            @Override public void prepare() { originalSwitch = PedroPrimaryDriveTuning.SWITCH; PedroSecondaryDriveTuning.ENABLED = true; PedroPrimaryDriveTuning.SWITCH = 999.0; }
            @Override public void cleanup() { PedroPrimaryDriveTuning.SWITCH = originalSwitch; }
        };
    }
}
