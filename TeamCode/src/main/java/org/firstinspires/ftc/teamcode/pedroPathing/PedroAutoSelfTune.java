package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroAutoTuner;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPathControlTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryDriveTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryHeadingTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroPrimaryTranslationTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryDriveTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryHeadingTuning;
import org.firstinspires.ftc.teamcode.pedroPathing.tuning.PedroSecondaryTranslationTuning;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Config
@Autonomous(name = "Pedro Auto Self Tune", group = "Pedro")
public class PedroAutoSelfTune extends LinearOpMode {
    public static boolean APPLY_RECOMMENDATIONS = true;
    public static boolean CSV_LOGGING_ENABLED = true;
    public static boolean USE_CENTER_START = true;
    public static double FORWARD_TEST_IN = 24.0;
    public static double STRAFE_TEST_IN = 24.0;
    public static double CURVE_TEST_IN = 24.0;
    public static double TELEMETRY_UPDATE_HZ = 10.0;

    private File csvFile;
    private FileWriter csvWriter;
    private final List<PedroAutoTuner.Sample> samples = new ArrayList<PedroAutoTuner.Sample>();
    private PedroSwerveFactory.PedroRobot robot;
    private Follower follower;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        follower = robot.follower;
        PedroSwerveFactory.applyLiveTuning(follower);

        Pose startPose = USE_CENTER_START
                ? new Pose(0.0, 0.0, 0.0)
                : PedroDecodeRoute.startPose(PedroStartPose.RED_BASE_CORNER);
        robot.stack.resetForStart();
        follower.setStartingPose(startPose);
        follower.setPose(startPose);
        robot.stack.setPedroPose(startPose);

        telemetry.addLine("Pedro Auto Self Tune ready");
        telemetry.addData("Assumed Start", "Center field, facing driver");
        telemetry.addData("Start Pose", startPose);
        telemetry.addData("Apply Recommendations", APPLY_RECOMMENDATIONS);
        telemetry.addData("CSV Logging", CSV_LOGGING_ENABLED);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        startCsv();
        try {
            runPhase("Forward24", startPose,
                    PedroBlockCommand.straight(FORWARD_TEST_IN, 0.0, 0.0, 1.0));

            runPhase("ReturnCenterX", new Pose(FORWARD_TEST_IN, 0.0, 0.0),
                    PedroBlockCommand.straight(0.0, 0.0, 0.0, 1.0));

            runPhase("Strafe24", new Pose(0.0, 0.0, 0.0),
                    PedroBlockCommand.straight(0.0, STRAFE_TEST_IN, 0.0, 1.0));

            runPhase("ReturnCenterY", new Pose(0.0, STRAFE_TEST_IN, 0.0),
                    PedroBlockCommand.straight(0.0, 0.0, 0.0, 1.0));

            runPhase("Curve90", new Pose(0.0, 0.0, 0.0),
                    PedroBlockCommand.curved(CURVE_TEST_IN, CURVE_TEST_IN, 90.0,
                            CURVE_TEST_IN, 0.0, 45.0, 1.0, 1.0));

            List<PedroAutoTuner.PhaseSummary> summaries = PedroAutoTuner.splitAndSummarize(samples);
            PedroAutoTuner.TuneRecommendations recommendations = PedroAutoTuner.recommend(summaries);
            if (APPLY_RECOMMENDATIONS) {
                PedroAutoTuner.apply(recommendations);
            }

            for (PedroAutoTuner.PhaseSummary summary : summaries) {
                telemetry.addData("Auto Telemetry/" + summary.phaseName + "/RMS Translation In", summary.rmsTranslationErrorIn);
                telemetry.addData("Auto Telemetry/" + summary.phaseName + "/RMS Heading Deg", summary.rmsHeadingErrorDeg);
            }

            telemetry.addData("Auto Telemetry/Primary Translation P", recommendations.primaryTranslationP);
            telemetry.addData("Auto Telemetry/Primary Translation D", recommendations.primaryTranslationD);
            telemetry.addData("Auto Telemetry/Primary Heading P", recommendations.primaryHeadingP);
            telemetry.addData("Auto Telemetry/Primary Heading D", recommendations.primaryHeadingD);
            telemetry.addData("Auto Telemetry/Primary Drive P", recommendations.primaryDriveP);
            telemetry.addData("Auto Telemetry/Primary Drive D", recommendations.primaryDriveD);
            telemetry.addData("Auto Telemetry/Centripetal", recommendations.centripetalScaling);
            telemetry.addData("Auto Telemetry/Applied", APPLY_RECOMMENDATIONS);
            telemetry.update();

            writeSummary(summaries, recommendations);
            sleep(2000);
        } finally {
            closeCsv();
            robot.stack.stopPedroDrive();
        }
    }

    private void runPhase(String phaseName, Pose phaseStart, PedroBlockCommand... commands) {
        PathChain path = PedroBlockRouteBuilder.build(follower, phaseStart, commands);
        Pose targetPose = extractTargetPose(commands[commands.length - 1]);
        follower.setStartingPose(phaseStart);
        follower.setPose(phaseStart);
        robot.stack.setPedroPose(phaseStart);
        follower.followPath(path, true);

        ElapsedTime timer = new ElapsedTime();
        double nextTelemetryTime = 0.0;
        while (opModeIsActive() && follower.isBusy()) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();

            Pose currentPose = follower.getPose();
            Pose currentVelocity = robot.stack.getPedroVelocityPose();
            org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector rawPinpoint = robot.stack.getLocalizer().getRawPinpointPose();
            double now = timer.seconds();

            PedroAutoTuner.Sample sample = new PedroAutoTuner.Sample(
                    phaseName,
                    now,
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
            samples.add(sample);
            writeSample(sample);

            if (now >= nextTelemetryTime) {
                telemetry.addData("Auto Telemetry/Phase", phaseName);
                telemetry.addData("Auto Telemetry/Time Sec", now);
                telemetry.addData("Auto Telemetry/Loop Dt Sec", robot.stack.getLastDtSec());
                telemetry.addData("Auto Telemetry/Distance Remaining", follower.getDistanceRemaining());
                telemetry.addData("Auto Telemetry/Target X In", targetPose.getX());
                telemetry.addData("Auto Telemetry/Target Y In", targetPose.getY());
                telemetry.addData("Auto Telemetry/Target Heading Deg", Math.toDegrees(targetPose.getHeading()));
                telemetry.addData("Auto Telemetry/Actual X In", currentPose.getX());
                telemetry.addData("Auto Telemetry/Actual Y In", currentPose.getY());
                telemetry.addData("Auto Telemetry/Actual Heading Deg", Math.toDegrees(currentPose.getHeading()));
                telemetry.addData("Auto Telemetry/Error In", Math.hypot(targetPose.getX() - currentPose.getX(), targetPose.getY() - currentPose.getY()));
                telemetry.addData("Auto Telemetry/Heading Error Deg", angleErrorDeg(Math.toDegrees(targetPose.getHeading()), Math.toDegrees(currentPose.getHeading())));
                telemetry.addData("Auto Telemetry/Vx In S", currentVelocity.getX());
                telemetry.addData("Auto Telemetry/Vy In S", currentVelocity.getY());
                telemetry.addData("Auto Telemetry/Omega Deg S", Math.toDegrees(currentVelocity.getHeading()));
                telemetry.addData("Auto Telemetry/Speed In S", Math.hypot(currentVelocity.getX(), currentVelocity.getY()));
                telemetry.addData("Auto Telemetry/Translation Authority", robot.stack.getDrivetrain().getLastTranslationAuthority());
                telemetry.addData("Auto Telemetry/Steer Ready", robot.stack.getDrivetrain().isSteerReadyForDrive());
                telemetry.addData("Auto Telemetry/Drive State", robot.stack.getDrivetrain().getState());
                telemetry.addData("Auto Telemetry/Battery V", robot.stack.getDrivetrain().getBatteryVoltage());
                telemetry.addData("Auto Telemetry/Pinpoint Used", robot.stack.getLocalizer().isUsingPinpoint());
                telemetry.addData("Auto Telemetry/Pinpoint Invalid Loops", robot.stack.getLocalizer().getConsecutiveInvalidPinpointLoops());
                telemetry.addData("Auto Telemetry/Raw Pinpoint X In", rawPinpoint.x());
                telemetry.addData("Auto Telemetry/Raw Pinpoint Y In", rawPinpoint.y());
                telemetry.addData("Auto Telemetry/Raw Pinpoint Heading Deg", Math.toDegrees(rawPinpoint.omega()));
                telemetry.addData("Auto Telemetry/Primary Translation P", PedroPrimaryTranslationTuning.P);
                telemetry.addData("Auto Telemetry/Primary Translation D", PedroPrimaryTranslationTuning.D);
                telemetry.addData("Auto Telemetry/Primary Heading P", PedroPrimaryHeadingTuning.P);
                telemetry.addData("Auto Telemetry/Primary Heading D", PedroPrimaryHeadingTuning.D);
                telemetry.addData("Auto Telemetry/Primary Drive P", PedroPrimaryDriveTuning.P);
                telemetry.addData("Auto Telemetry/Primary Drive D", PedroPrimaryDriveTuning.D);
                telemetry.addData("Auto Telemetry/Secondary Translation P", PedroSecondaryTranslationTuning.P);
                telemetry.addData("Auto Telemetry/Secondary Heading P", PedroSecondaryHeadingTuning.P);
                telemetry.addData("Auto Telemetry/Secondary Drive P", PedroSecondaryDriveTuning.P);
                telemetry.addData("Auto Telemetry/Centripetal", PedroPathControlTuning.CENTRIPETAL_SCALING);
                telemetry.update();
                nextTelemetryTime = now + (1.0 / TELEMETRY_UPDATE_HZ);
            }
        }
    }

    private Pose extractTargetPose(PedroBlockCommand command) {
        return command.endPose();
    }

    private void startCsv() {
        if (!CSV_LOGGING_ENABLED) {
            return;
        }
        csvFile = AppUtil.getInstance().getSettingsFile("PedroAutoSelfTune-" + System.currentTimeMillis() + ".csv");
        try {
            csvWriter = new FileWriter(csvFile);
            csvWriter.write("phase,time_sec,target_x_in,target_y_in,target_heading_deg,actual_x_in,actual_y_in,actual_heading_deg,actual_vx_in_s,actual_vy_in_s,actual_omega_deg_s,distance_remaining,translation_authority,steer_ready,battery_v,pinpoint_used,pinpoint_invalid_loops,raw_pinpoint_x_in,raw_pinpoint_y_in,raw_pinpoint_heading_deg\n");
            csvWriter.flush();
        } catch (IOException e) {
            telemetry.addData("Auto Telemetry/CSV Error", e.getMessage());
            csvWriter = null;
        }
    }

    private void writeSample(PedroAutoTuner.Sample sample) {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.write(sample.phaseName + ","
                    + sample.timeSec + ","
                    + sample.targetXIn + ","
                    + sample.targetYIn + ","
                    + sample.targetHeadingDeg + ","
                    + sample.actualXIn + ","
                    + sample.actualYIn + ","
                    + sample.actualHeadingDeg + ","
                    + sample.actualVxInS + ","
                    + sample.actualVyInS + ","
                    + sample.actualOmegaDegS + ","
                    + sample.distanceRemaining + ","
                    + sample.translationAuthority + ","
                    + sample.steerReady + ","
                    + sample.batteryVoltage + ","
                    + sample.usingPinpoint + ","
                    + sample.invalidPinpointLoops + ","
                    + sample.rawPinpointXIn + ","
                    + sample.rawPinpointYIn + ","
                    + sample.rawPinpointHeadingDeg + "\n");
        } catch (IOException e) {
            telemetry.addData("Auto Telemetry/CSV Write Error", e.getMessage());
        }
    }

    private void writeSummary(List<PedroAutoTuner.PhaseSummary> summaries, PedroAutoTuner.TuneRecommendations recommendations) {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.write("\nsummary_phase,rms_translation_in,max_translation_in,final_translation_in,rms_heading_deg,max_heading_deg,final_heading_deg,mean_speed_in_s,peak_speed_in_s,heading_sign_changes,mean_distance_remaining,max_distance_remaining,mean_translation_authority,min_translation_authority,pinpoint_invalid_ratio,mean_battery_v\n");
            for (PedroAutoTuner.PhaseSummary summary : summaries) {
                csvWriter.write(summary.phaseName + ","
                        + summary.rmsTranslationErrorIn + ","
                        + summary.maxTranslationErrorIn + ","
                        + summary.finalTranslationErrorIn + ","
                        + summary.rmsHeadingErrorDeg + ","
                        + summary.maxHeadingErrorDeg + ","
                        + summary.finalHeadingErrorDeg + ","
                        + summary.meanSpeedInS + ","
                        + summary.peakSpeedInS + ","
                        + summary.headingSignChanges + ","
                        + summary.meanDistanceRemainingIn + ","
                        + summary.maxDistanceRemainingIn + ","
                        + summary.meanTranslationAuthority + ","
                        + summary.minTranslationAuthority + ","
                        + summary.pinpointInvalidRatio + ","
                        + summary.meanBatteryVoltage + "\n");
            }
            csvWriter.write("\nrecommendation,key,value\n");
            csvWriter.write("recommendation,primary_translation_p," + recommendations.primaryTranslationP + "\n");
            csvWriter.write("recommendation,primary_translation_d," + recommendations.primaryTranslationD + "\n");
            csvWriter.write("recommendation,secondary_translation_p," + recommendations.secondaryTranslationP + "\n");
            csvWriter.write("recommendation,secondary_translation_d," + recommendations.secondaryTranslationD + "\n");
            csvWriter.write("recommendation,primary_heading_p," + recommendations.primaryHeadingP + "\n");
            csvWriter.write("recommendation,primary_heading_d," + recommendations.primaryHeadingD + "\n");
            csvWriter.write("recommendation,secondary_heading_p," + recommendations.secondaryHeadingP + "\n");
            csvWriter.write("recommendation,secondary_heading_d," + recommendations.secondaryHeadingD + "\n");
            csvWriter.write("recommendation,primary_drive_p," + recommendations.primaryDriveP + "\n");
            csvWriter.write("recommendation,primary_drive_d," + recommendations.primaryDriveD + "\n");
            csvWriter.write("recommendation,secondary_drive_p," + recommendations.secondaryDriveP + "\n");
            csvWriter.write("recommendation,secondary_drive_d," + recommendations.secondaryDriveD + "\n");
            csvWriter.write("recommendation,centripetal_scaling," + recommendations.centripetalScaling + "\n");
            csvWriter.flush();
        } catch (IOException e) {
            telemetry.addData("Auto Telemetry/CSV Summary Error", e.getMessage());
        }
    }

    private void closeCsv() {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException ignored) {
        } finally {
            csvWriter = null;
        }
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
