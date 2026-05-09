package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.LoopTimeEstimator;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.auto.AutoPoseStep;
import org.firstinspires.ftc.teamcode.auto.KookyAutoController;
import org.firstinspires.ftc.teamcode.auto.SimpleAutoSequence;

@Config
@Autonomous(name = "SimplePoseAuto")
public class SimplePoseAuto extends LinearOpMode {
    public static double START_X_IN = 0.0;
    public static double START_Y_IN = 0.0;
    public static double START_HEADING_RAD = 0.0;

    private final ElapsedTime timer = new ElapsedTime();
    private final LoopTimeEstimator loopTimeEstimator = new LoopTimeEstimator();
    private int loopCounter = 0;
    private int lastReportedStep = -1;

    @Override
    public void runOpMode() throws InterruptedException {
        if (org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }

        Logger logger = new Logger(telemetry);
        HWMap hwMap = new HWMap(hardwareMap);
        SwerveLocalizer localizer = new SwerveLocalizer(hwMap);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap, logger);
        localizer.setPose(new Vector(START_X_IN, START_Y_IN, START_HEADING_RAD));

        SimpleAutoSequence sequence = new SimpleAutoSequence(
                new KookyAutoController(),
                new AutoPoseStep(new Pose(24.0, 0.0, 0.0), 0.20, 3.0),
                new AutoPoseStep(new Pose(24.0, 24.0, Math.PI / 2.0), 0.20, 3.0),
                new AutoPoseStep(new Pose(0.0, 24.0, Math.PI), 0.20, 3.0));
        double[] command = new double[3];

        waitForStart();
        timer.reset();
        loopTimeEstimator.reset();

        while (opModeIsActive()) {
            hwMap.clearBulkCache();
            drivetrain.refreshSensors();
            localizer.refreshSensors();
            double dt = loopTimeEstimator.update(timer.seconds());
            timer.reset();
            localizer.update(drivetrain.getActualVelocity(), dt);

            Vector currentPoseVector = localizer.getPose();
            double currentX = currentPoseVector.x();
            double currentY = currentPoseVector.y();
            double currentHeading = currentPoseVector.omega();
            sequence.update(currentX, currentY, currentHeading, dt, command);
            drivetrain.setVelocity(command[0], command[1], command[2], dt);

            Pose targetPose = sequence.getCurrentTargetPose();
            loopCounter++;
            int currentStep = sequence.getCurrentStepIndex();
            boolean stepChanged = currentStep != lastReportedStep;
            if (stepChanged || shouldEmitTelemetry(loopCounter, SwerveConfig.AUTO_TELEMETRY_INTERVAL_LOOPS)) {
                logger.log("Auto Step", currentStep, Logger.LogLevels.PRODUCTION);
                logger.log("Auto X (in)", currentX, Logger.LogLevels.PRODUCTION);
                logger.log("Auto Y (in)", currentY, Logger.LogLevels.PRODUCTION);
                logger.log("Auto H (deg)", Math.toDegrees(currentHeading), Logger.LogLevels.PRODUCTION);
                if (targetPose != null) {
                    logger.log("Target X (in)", targetPose.x, Logger.LogLevels.PRODUCTION);
                    logger.log("Target Y (in)", targetPose.y, Logger.LogLevels.PRODUCTION);
                }
                telemetry.update();
            }
            lastReportedStep = currentStep;
        }
    }

    private boolean shouldEmitTelemetry(int currentLoop, int intervalLoops) {
        return currentLoop % Math.max(1, intervalLoops) == 0;
    }
}
