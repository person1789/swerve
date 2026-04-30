package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

import java.util.ArrayList;
import java.util.List;

@Config
@Autonomous(name = "Pedro Bootstrap Auto Tune", group = "Pedro Tuning")
public class PedroBootstrapAutoTune extends LinearOpMode {
    public static boolean APPLY_SEEDS = true;
    public static double VELOCITY_SCALE = 0.5;
    public static double STEP_DURATION_SEC = 1.20;
    public static double RELEASE_DURATION_SEC = 0.80;
    public static double TELEMETRY_HZ = 10.0;

    private PedroSwerveFactory.PedroRobot robot;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        robot.stack.resetForStart();
        robot.stack.setPedroVelocityConstraintScale(VELOCITY_SCALE);

        telemetry.addLine("Pedro bootstrap auto tune ready");
        telemetry.addData("Auto Telemetry/Velocity Scale", VELOCITY_SCALE);
        telemetry.addData("Auto Telemetry/Apply Seeds", APPLY_SEEDS);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        List<PedroBootstrapIdentifier.MotionSample> forwardSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();
        List<PedroBootstrapIdentifier.MotionSample> lateralSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();
        List<PedroBootstrapIdentifier.MotionSample> turnSamples = new ArrayList<PedroBootstrapIdentifier.MotionSample>();

        runPhase("ForwardStep", VELOCITY_SCALE, 0.0, 0.0, STEP_DURATION_SEC, forwardSamples);
        runPhase("ForwardRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, forwardSamples);
        runPhase("LateralStep", 0.0, VELOCITY_SCALE, 0.0, STEP_DURATION_SEC, lateralSamples);
        runPhase("LateralRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, lateralSamples);
        runPhase("TurnStep", 0.0, 0.0, VELOCITY_SCALE, STEP_DURATION_SEC, turnSamples);
        runPhase("TurnRelease", 0.0, 0.0, 0.0, RELEASE_DURATION_SEC, turnSamples);

        PedroBootstrapIdentifier.AxisSummary forward = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.FORWARD, forwardSamples);
        PedroBootstrapIdentifier.AxisSummary lateral = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.LATERAL, lateralSamples);
        PedroBootstrapIdentifier.AxisSummary turn = PedroBootstrapIdentifier.summarize(PedroBootstrapIdentifier.Axis.TURN, turnSamples);
        PedroBootstrapIdentifier.BootstrapSeeds seeds = PedroBootstrapIdentifier.seedFromIdentification(forward, lateral, turn);

        if (APPLY_SEEDS) {
            PedroBootstrapIdentifier.applySeeds(seeds);
        }
        PedroSwerveFactory.applyLiveTuning(robot.follower);
        robot.stack.stopPedroDrive();
        robot.stack.setPedroVelocityConstraintScale(1.0);

        emitSummaryTelemetry(forward, lateral, turn, seeds);
        telemetry.update();
        sleep(2000);
    }

    private void runPhase(String phaseName, double forward, double strafe, double turn, double durationSec,
                          List<PedroBootstrapIdentifier.MotionSample> sink) {
        ElapsedTime timer = new ElapsedTime();
        double nextTelemetry = 0.0;
        while (opModeIsActive() && timer.seconds() < durationSec) {
            robot.stack.drivePedroRobotCentric(forward, strafe, turn);
            com.pedropathing.geometry.Pose measuredVelocity = robot.stack.getPedroVelocityPose();
            double now = timer.seconds();
            sink.add(new PedroBootstrapIdentifier.MotionSample(
                    phaseName,
                    now,
                    forward,
                    strafe,
                    turn,
                    measuredVelocity.getX(),
                    measuredVelocity.getY(),
                    measuredVelocity.getHeading()));

            if (now >= nextTelemetry) {
                telemetry.addData("Auto Telemetry/Bootstrap Phase", phaseName);
                telemetry.addData("Auto Telemetry/Bootstrap Time Sec", now);
                telemetry.addData("Auto Telemetry/Cmd Forward", forward);
                telemetry.addData("Auto Telemetry/Cmd Strafe", strafe);
                telemetry.addData("Auto Telemetry/Cmd Turn", turn);
                telemetry.addData("Auto Telemetry/Vx In S", measuredVelocity.getX());
                telemetry.addData("Auto Telemetry/Vy In S", measuredVelocity.getY());
                telemetry.addData("Auto Telemetry/Omega Rad S", measuredVelocity.getHeading());
                telemetry.addData("Auto Telemetry/Pinpoint Used", robot.stack.getLocalizer().isUsingPinpoint());
                telemetry.addData("Auto Telemetry/Pinpoint Invalid Loops", robot.stack.getLocalizer().getConsecutiveInvalidPinpointLoops());
                telemetry.update();
                nextTelemetry = now + (1.0 / TELEMETRY_HZ);
            }
        }
    }

    private void emitSummaryTelemetry(PedroBootstrapIdentifier.AxisSummary forward,
                                      PedroBootstrapIdentifier.AxisSummary lateral,
                                      PedroBootstrapIdentifier.AxisSummary turn,
                                      PedroBootstrapIdentifier.BootstrapSeeds seeds) {
        telemetry.addLine("Bootstrap identification complete");
        emitAxisTelemetry("Forward", forward);
        emitAxisTelemetry("Lateral", lateral);
        emitAxisTelemetry("Turn", turn);

        telemetry.addData("Auto Telemetry/Seed Forward Zero Power Accel", seeds.forwardZeroPowerAcceleration);
        telemetry.addData("Auto Telemetry/Seed Lateral Zero Power Accel", seeds.lateralZeroPowerAcceleration);
        telemetry.addData("Auto Telemetry/Seed Primary Translation P", seeds.primaryTranslationP);
        telemetry.addData("Auto Telemetry/Seed Primary Translation I", seeds.primaryTranslationI);
        telemetry.addData("Auto Telemetry/Seed Primary Translation D", seeds.primaryTranslationD);
        telemetry.addData("Auto Telemetry/Seed Primary Heading P", seeds.primaryHeadingP);
        telemetry.addData("Auto Telemetry/Seed Primary Heading I", seeds.primaryHeadingI);
        telemetry.addData("Auto Telemetry/Seed Primary Heading D", seeds.primaryHeadingD);
        telemetry.addData("Auto Telemetry/Seed Primary Drive P", seeds.primaryDriveP);
        telemetry.addData("Auto Telemetry/Seed Primary Drive I", seeds.primaryDriveI);
        telemetry.addData("Auto Telemetry/Seed Primary Drive D", seeds.primaryDriveD);
        telemetry.addData("Auto Telemetry/Seed Cntrptl", seeds.centripetalScaling);
        telemetry.addData("Auto Telemetry/Seeds Applied", APPLY_SEEDS);
    }

    private void emitAxisTelemetry(String label, PedroBootstrapIdentifier.AxisSummary summary) {
        telemetry.addData("Auto Telemetry/" + label + " Deadzone Cmd", summary.deadzoneCommand);
        telemetry.addData("Auto Telemetry/" + label + " Max Velocity", summary.maxVelocity);
        telemetry.addData("Auto Telemetry/" + label + " Accel Rate", summary.accelRate);
        telemetry.addData("Auto Telemetry/" + label + " Brake Rate", summary.brakeRate);
        telemetry.addData("Auto Telemetry/" + label + " Rise Time Sec", summary.riseTimeSec);
    }
}
