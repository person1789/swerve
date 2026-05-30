package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

import org.firstinspires.ftc.teamcode.auto.DriveContext;
import org.firstinspires.ftc.teamcode.auto.DriveScheduler;
import org.firstinspires.ftc.teamcode.auto.MoveToPoseCommand;
import org.firstinspires.ftc.teamcode.auto.PinpointLocalizer;
import org.firstinspires.ftc.teamcode.auto.StopDriveCommand;
import org.firstinspires.ftc.teamcode.auto.WaitForAzimuthCommand;

@Config
@Autonomous(name = "Auto Tuner")
public class AutoTuner extends LinearOpMode {

    // ── Select which constant group to tune ──────────────────────────────────
    // TRANSLATION_P  : tunes AUTO_X_P / AUTO_Y_P
    // TRANSLATION_D  : tunes AUTO_X_D / AUTO_Y_D
    // HEADING        : tunes AUTO_HEADING_P / AUTO_HEADING_D
    // TOLERANCES     : tunes translation + heading tolerance and settle delay
    // AZIMUTH        : tunes azimuth wait before a real move
    // EIGHT_WAY_TEST : runs an 8-way strafe star pattern plus rotations to test everything
    public static Mode TUNE_MODE = Mode.TRANSLATION_P;

    public enum Mode {
        TRANSLATION_P,
        TRANSLATION_D,
        HEADING,
        TOLERANCES,
        AZIMUTH,
        EIGHT_WAY_TEST
    }

    // ── Shared move target (change from Dashboard per mode) ──────────────────
    public static double TARGET_X_IN  = 48.0;
    public static double TARGET_Y_IN  =  0.0;
    public static double TARGET_HDG_DEG = 0.0;

    // ── For HEADING mode, a pure rotation target ──────────────────────────────
    public static double ROTATE_ONLY_DEG = 90.0;

    // ── Lets you reset the localizer pose mid-run from Dashboard ─────────────
    public static boolean RESET_POSE = false;

    // ── Configurable pause between legs to prevent tipping ───────────────────
    public static int PAUSE_BETWEEN_LEGS_MS = 1000;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        HWMap hwMap = new HWMap(hardwareMap);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap);
        PinpointLocalizer localizer  = new PinpointLocalizer(hwMap);
        localizer.setPose(0, 0, 0);

        ElapsedTime timer = new ElapsedTime();

        // Pre-start: show what mode we'll run and pre-steer modules
        while (!isStarted() && !isStopRequested()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            double dt = Math.max(1e-3, timer.seconds());
            timer.reset();

            drivetrain.pointModulesForCommand(1.0, 0.0, 0.0, dt);
            drivetrain.write(dt);

            telemetry.addData("MODE", TUNE_MODE);
            telemetry.addData("pinpointReady", localizer.isReady());
            telemetry.addData("azimuthReady",
                drivetrain.areModulesAzimuthReady(SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD));
            telemetry.update();
        }

        if (!opModeIsActive()) return;

        timer.reset();

        switch (TUNE_MODE) {
            case TRANSLATION_P: runTranslationP(hwMap, drivetrain, localizer, timer); break;
            case TRANSLATION_D: runTranslationD(hwMap, drivetrain, localizer, timer); break;
            case HEADING:       runHeading      (hwMap, drivetrain, localizer, timer); break;
            case TOLERANCES:    runTolerances   (hwMap, drivetrain, localizer, timer); break;
            case AZIMUTH:       runAzimuth      (hwMap, drivetrain, localizer, timer); break;
            case EIGHT_WAY_TEST:runEightWayTest (hwMap, drivetrain, localizer, timer); break;
        }
    }

    // ── TRANSLATION_P ────────────────────────────────────────────────────────
    // What you're watching: does the robot accelerate firmly toward the target?
    // Is it sluggish from far away? Does it crawl the last few inches?
    //
    // How to use:
    //   Set TARGET_X_IN = 48, TARGET_Y_IN = 0.
    //   Robot drives 48" forward then 48" back, repeating forever.
    //   Raise AUTO_X_P until the robot moves purposefully.
    //   Lower if it oscillates past the target before stopping.
    //   AUTO_X_D should be 0 while doing this so D doesn't hide P problems.
    //   Repeat with TARGET_Y_IN = 48, TARGET_X_IN = 0 for Y axis.
    //
    // What "good" looks like:
    //   Robot reaches target in ~1.5s from 48", stops cleanly, no oscillation.
    private void runTranslationP(HWMap hwMap, SwerveDrivetrain drivetrain,
                                  PinpointLocalizer localizer, ElapsedTime timer) {

        // Force D to zero so only P is acting
        double savedXD = SwerveConfig.AUTO_X_D;
        double savedYD = SwerveConfig.AUTO_Y_D;
        SwerveConfig.AUTO_X_D = 0.0;
        SwerveConfig.AUTO_Y_D = 0.0;

        // Give it enough time to settle — we're watching P, not timeout
        SwerveConfig.AUTO_MOVE_TIMEOUT_MS = 5000.0;
        SwerveConfig.AUTO_SETTLE_DELAY_MS = 200.0;

        boolean goingOut = true;

        while (opModeIsActive()) {
            double targetX = goingOut ? TARGET_X_IN : 0.0;
            double targetY = goingOut ? TARGET_Y_IN : 0.0;

            DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
            MoveToPoseCommand move = new MoveToPoseCommand(targetX, targetY, 0.0,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
            move.init(context);

            while (opModeIsActive() && !move.isFinished(context)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();

                handleReset(localizer);

                move.tick(context, dt);
                drivetrain.write(dt);

                double xErr = targetX - localizer.getXInches();
                double yErr = targetY - localizer.getYInches();

                // Raw P contribution so you can see what P alone is doing
                double rawXPow = SwerveConfig.AUTO_X_P * xErr;
                double rawYPow = SwerveConfig.AUTO_Y_P * yErr;

                telemetry.addData("── TRANSLATION P ──", "");
                telemetry.addData("AUTO_X_P", SwerveConfig.AUTO_X_P);
                telemetry.addData("AUTO_Y_P", SwerveConfig.AUTO_Y_P);
                telemetry.addData("target", "%.1f, %.1f", targetX, targetY);
                telemetry.addData("pose",   "%.2f, %.2f", localizer.getXInches(), localizer.getYInches());
                telemetry.addData("xError_in", "%.2f", xErr);
                telemetry.addData("yError_in", "%.2f", yErr);
                telemetry.addData("rawXPow (P*err)", "%.3f", rawXPow);
                telemetry.addData("rawYPow (P*err)", "%.3f", rawYPow);
                telemetry.addData("leg", goingOut ? "outbound" : "return");
                telemetry.update();
            }

            move.end(context, false);
            goingOut = !goingOut;
            sleep(PAUSE_BETWEEN_LEGS_MS); // brief pause between legs
        }

        SwerveConfig.AUTO_X_D = savedXD;
        SwerveConfig.AUTO_Y_D = savedYD;
    }

    // ── TRANSLATION_D ────────────────────────────────────────────────────────
    // What you're watching: does the robot stop cleanly without overshooting?
    //
    // How to use:
    //   Keep AUTO_X_P at your tuned value from the previous step.
    //   Start AUTO_X_D = 0, watch how far past the target the robot travels.
    //   Raise AUTO_X_D until overshoot disappears.
    //   Stop raising when deceleration feels jerky or it starts oscillating again.
    //
    // What "good" looks like:
    //   Robot glides to a stop exactly at the target. No bounce, no creep.
    private void runTranslationD(HWMap hwMap, SwerveDrivetrain drivetrain,
                                  PinpointLocalizer localizer, ElapsedTime timer) {

        SwerveConfig.AUTO_MOVE_TIMEOUT_MS  = 5000.0;
        SwerveConfig.AUTO_SETTLE_DELAY_MS  = 300.0;

        boolean goingOut = true;

        while (opModeIsActive()) {
            double targetX = goingOut ? TARGET_X_IN : 0.0;
            double targetY = goingOut ? TARGET_Y_IN : 0.0;

            DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
            MoveToPoseCommand move = new MoveToPoseCommand(targetX, targetY, 0.0,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
            move.init(context);

            double peakOvershootX = 0.0;
            double peakOvershootY = 0.0;

            while (opModeIsActive() && !move.isFinished(context)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();

                handleReset(localizer);

                move.tick(context, dt);
                drivetrain.write(dt);

                double xErr = targetX - localizer.getXInches();
                double yErr = targetY - localizer.getYInches();

                // Track how far past target robot went (negative error = overshoot)
                if (goingOut && xErr < peakOvershootX) peakOvershootX = xErr;
                if (goingOut && yErr < peakOvershootY) peakOvershootY = yErr;

                telemetry.addData("── TRANSLATION D ──", "");
                telemetry.addData("AUTO_X_P", SwerveConfig.AUTO_X_P);
                telemetry.addData("AUTO_X_D", SwerveConfig.AUTO_X_D);
                telemetry.addData("AUTO_Y_P", SwerveConfig.AUTO_Y_P);
                telemetry.addData("AUTO_Y_D", SwerveConfig.AUTO_Y_D);
                telemetry.addData("target", "%.1f, %.1f", targetX, targetY);
                telemetry.addData("pose",   "%.2f, %.2f", localizer.getXInches(), localizer.getYInches());
                telemetry.addData("xError_in", "%.2f", xErr);
                telemetry.addData("yError_in", "%.2f", yErr);
                telemetry.addData("peakOvershootX_in", "%.2f", peakOvershootX);
                telemetry.addData("peakOvershootY_in", "%.2f", peakOvershootY);
                telemetry.addData("leg", goingOut ? "outbound" : "return");
                telemetry.update();
            }

            move.end(context, false);
            goingOut = !goingOut;
            sleep(PAUSE_BETWEEN_LEGS_MS);
        }
    }

    // ── HEADING ──────────────────────────────────────────────────────────────
    // What you're watching: does the robot spin to the target angle cleanly?
    //
    // How to use:
    //   Set ROTATE_ONLY_DEG = 90 (or 180 for a harder test).
    //   Robot holds X=0, Y=0 and just rotates back and forth.
    //   Raise AUTO_HEADING_P until rotation is firm but not oscillating.
    //   Raise AUTO_HEADING_D until any end-of-move oscillation disappears.
    //   AUTO_MAX_TURN_POWER caps the output — raise it if rotation feels weak
    //   even with high P.
    //
    // What "good" looks like:
    //   Robot snaps to target heading in ~0.5s, holds without hunting.
    private void runHeading(HWMap hwMap, SwerveDrivetrain drivetrain,
                             PinpointLocalizer localizer, ElapsedTime timer) {

        SwerveConfig.AUTO_MOVE_TIMEOUT_MS = 3000.0;
        SwerveConfig.AUTO_SETTLE_DELAY_MS = 300.0;

        boolean goingOut = true;

        while (opModeIsActive()) {
            double targetHdg = goingOut
                ? Math.toRadians(ROTATE_ONLY_DEG)
                : 0.0;

            DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
            // Stay at origin, only rotate
            MoveToPoseCommand move = new MoveToPoseCommand(0.0, 0.0, targetHdg,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
            move.init(context);

            while (opModeIsActive() && !move.isFinished(context)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();

                handleReset(localizer);

                move.tick(context, dt);
                drivetrain.write(dt);

                double hdgErr = MathUtil.angleError(
                    localizer.getHeadingRadians(), targetHdg);
                double rawTurnPow = SwerveConfig.AUTO_HEADING_P * hdgErr;

                telemetry.addData("── HEADING ──", "");
                telemetry.addData("AUTO_HEADING_P", SwerveConfig.AUTO_HEADING_P);
                telemetry.addData("AUTO_HEADING_D", SwerveConfig.AUTO_HEADING_D);
                telemetry.addData("AUTO_MAX_TURN_POWER", SwerveConfig.AUTO_MAX_TURN_POWER);
                telemetry.addData("target_deg", "%.1f", Math.toDegrees(targetHdg));
                telemetry.addData("heading_deg", "%.1f",
                    Math.toDegrees(localizer.getHeadingRadians()));
                telemetry.addData("headingError_deg", "%.2f", Math.toDegrees(hdgErr));
                telemetry.addData("rawTurnPow (P*err)", "%.3f", rawTurnPow);
                telemetry.addData("leg", goingOut ? "outbound" : "return");
                telemetry.update();
            }

            move.end(context, false);
            goingOut = !goingOut;
            sleep(PAUSE_BETWEEN_LEGS_MS);
        }
    }

    // ── TOLERANCES ───────────────────────────────────────────────────────────
    // What you're watching: does the command finish at the right time?
    //
    // How to use:
    //   Run a fixed move. Watch finalXError and finalYError at the moment
    //   the command exits. That tells you what tolerance actually makes sense
    //   given your P and D values — there's no point setting tolerance tighter
    //   than your controller can actually hold.
    //   Tune AUTO_SETTLE_DELAY_MS: 0 = exit immediately on first in-tolerance
    //   loop. Raise if the robot tends to bounce back out before truly stopped.
    //
    // What "good" looks like:
    //   finalXError <= AUTO_TRANSLATION_TOLERANCE_IN at exit.
    //   The command does not time out (elapsedMs < AUTO_MOVE_TIMEOUT_MS).
    private void runTolerances(HWMap hwMap, SwerveDrivetrain drivetrain,
                                PinpointLocalizer localizer, ElapsedTime timer) {

        while (opModeIsActive()) {
            DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
            MoveToPoseCommand move = new MoveToPoseCommand(
                TARGET_X_IN, TARGET_Y_IN, Math.toRadians(TARGET_HDG_DEG),
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
            move.init(context);

            double elapsedMs = 0;

            while (opModeIsActive() && !move.isFinished(context)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();
                elapsedMs += dt * 1000.0;

                handleReset(localizer);

                move.tick(context, dt);
                drivetrain.write(dt);

                double xErr = TARGET_X_IN - localizer.getXInches();
                double yErr = TARGET_Y_IN - localizer.getYInches();
                double hdgErr = MathUtil.angleError(
                    localizer.getHeadingRadians(), Math.toRadians(TARGET_HDG_DEG));
                boolean inTol = Math.hypot(xErr, yErr) <= SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN
                             && Math.abs(hdgErr) <= SwerveConfig.AUTO_HEADING_TOLERANCE_RAD;

                telemetry.addData("── TOLERANCES ──", "");
                telemetry.addData("AUTO_TRANSLATION_TOLERANCE_IN", SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN);
                telemetry.addData("AUTO_HEADING_TOLERANCE_RAD_deg",
                    Math.toDegrees(SwerveConfig.AUTO_HEADING_TOLERANCE_RAD));
                telemetry.addData("AUTO_SETTLE_DELAY_MS", SwerveConfig.AUTO_SETTLE_DELAY_MS);
                telemetry.addData("AUTO_MOVE_TIMEOUT_MS", SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
                telemetry.addData("xError_in", "%.3f", xErr);
                telemetry.addData("yError_in", "%.3f", yErr);
                telemetry.addData("headingError_deg", "%.2f", Math.toDegrees(hdgErr));
                telemetry.addData("insideTolerance", inTol);
                telemetry.addData("elapsedMs", "%.0f", elapsedMs);
                telemetry.update();
            }

            // Command finished — show final state so you can judge tolerance quality
            double finalXErr = TARGET_X_IN - localizer.getXInches();
            double finalYErr = TARGET_Y_IN - localizer.getYInches();
            boolean timedOut = elapsedMs >= SwerveConfig.AUTO_MOVE_TIMEOUT_MS;

            telemetry.addData("── FINISHED ──", "");
            telemetry.addData("timedOut", timedOut);
            telemetry.addData("finalXError_in", "%.3f", finalXErr);
            telemetry.addData("finalYError_in", "%.3f", finalYErr);
            telemetry.update();

            // Hold so you can read the final numbers before next leg
            sleep(PAUSE_BETWEEN_LEGS_MS);

            // Drive back to origin for next rep
            DriveContext returnCtx = new DriveContext(drivetrain, localizer, hwMap);
            MoveToPoseCommand returnMove = new MoveToPoseCommand(0, 0, 0,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
            returnMove.init(returnCtx);
            while (opModeIsActive() && !returnMove.isFinished(returnCtx)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();
                returnMove.tick(returnCtx, dt);
                drivetrain.write(dt);
                telemetry.addData("returning to origin...", "");
                telemetry.update();
            }
            returnMove.end(returnCtx, false);
            sleep(PAUSE_BETWEEN_LEGS_MS);
        }
    }

    // ── AZIMUTH ──────────────────────────────────────────────────────────────
    // What you're watching: do modules steer to the right angle before moving?
    // Does the wait exit cleanly or time out?
    //
    // How to use:
    //   Set AUTO_ABORT_ON_AZIMUTH_TIMEOUT = false while tuning so a timeout
    //   doesn't kill your run. Watch azimuthReady and elapsedMs.
    //   If modules reach tolerance well before timeout: tolerance or timeout
    //   could be tightened.
    //   If it always times out: either loosen tolerance or raise timeout.
    //
    // What "good" looks like:
    //   azimuthReady flips true well before AUTO_AZIMUTH_TIMEOUT_MS elapses.
    //   Then the move executes normally.
    private void runAzimuth(HWMap hwMap, SwerveDrivetrain drivetrain,
                             PinpointLocalizer localizer, ElapsedTime timer) {

        SwerveConfig.AUTO_MOVE_TIMEOUT_MS = 5000.0;
        SwerveConfig.AUTO_SETTLE_DELAY_MS = 200.0;

        while (opModeIsActive()) {
            DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
            DriveScheduler scheduler = new DriveScheduler(3);
            scheduler.add(new WaitForAzimuthCommand(
                TARGET_X_IN, TARGET_Y_IN,
                Math.toRadians(TARGET_HDG_DEG),
                SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD,
                SwerveConfig.AUTO_AZIMUTH_TIMEOUT_MS,
                SwerveConfig.AUTO_ABORT_ON_AZIMUTH_TIMEOUT));
            scheduler.add(new MoveToPoseCommand(
                TARGET_X_IN, TARGET_Y_IN,
                Math.toRadians(TARGET_HDG_DEG),
                SwerveConfig.AUTO_SETTLE_DELAY_MS,
                SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
            scheduler.add(new StopDriveCommand());

            double azimuthElapsedMs = 0;
            boolean azimuthDone = false;

            while (opModeIsActive() && !scheduler.isFinished()) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();

                handleReset(localizer);

                // Track azimuth phase elapsed separately
                if (scheduler.getCurrentCommandName().equals("wait_azimuth")) {
                    azimuthElapsedMs += dt * 1000.0;
                } else if (!azimuthDone) {
                    azimuthDone = true; // azimuth phase just ended
                }

                scheduler.tick(context, dt);
                drivetrain.write(dt);

                telemetry.addData("── AZIMUTH ──", "");
                telemetry.addData("AUTO_AZIMUTH_TOLERANCE_RAD_deg",
                    Math.toDegrees(SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD));
                telemetry.addData("AUTO_AZIMUTH_TIMEOUT_MS", SwerveConfig.AUTO_AZIMUTH_TIMEOUT_MS);
                telemetry.addData("AUTO_ABORT_ON_AZIMUTH_TIMEOUT",
                    SwerveConfig.AUTO_ABORT_ON_AZIMUTH_TIMEOUT);
                telemetry.addData("cmd", scheduler.getCurrentCommandName());
                telemetry.addData("aborted", scheduler.wasAborted());
                telemetry.addData("azimuthElapsedMs", "%.0f", azimuthElapsedMs);
                telemetry.addData("azimuthReady",
                    drivetrain.areModulesAzimuthReady(SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD));
                // Per-module steering errors so you can see which one is slow
                for (int i = 0; i < drivetrain.modules.length; i++) {
                    telemetry.addData("module[" + i + "] steerErr_deg",
                        "%.1f", Math.toDegrees(
                            Math.abs(drivetrain.modules[i].getSteerErrorRadians())));
                }
                telemetry.addData("pose", "%.2f, %.2f, %.1f",
                    localizer.getXInches(), localizer.getYInches(),
                    Math.toDegrees(localizer.getHeadingRadians()));
                telemetry.update();
            }

            sleep(PAUSE_BETWEEN_LEGS_MS);

            // Drive back to origin
            DriveContext returnCtx = new DriveContext(drivetrain, localizer, hwMap);
            MoveToPoseCommand returnMove = new MoveToPoseCommand(0, 0, 0,
                200.0, 5000.0);
            returnMove.init(returnCtx);
            while (opModeIsActive() && !returnMove.isFinished(returnCtx)) {
                hwMap.clearBulkCache();
                drivetrain.read();
                localizer.update();
                double dt = Math.max(1e-3, timer.seconds());
                timer.reset();
                returnMove.tick(returnCtx, dt);
                drivetrain.write(dt);
                telemetry.addData("returning...", "");
                telemetry.update();
            }
            returnMove.end(returnCtx, false);
            sleep(PAUSE_BETWEEN_LEGS_MS);
        }
    }

    // ── EIGHT_WAY_TEST ───────────────────────────────────────────────────────
    // What you're watching: does the robot drive accurately in all 8 directions?
    // How to use: Set TUNE_MODE = EIGHT_WAY_TEST. It will drive out to TARGET_X_IN
    // radius in 8 directions, then do two rotations.
    private void runEightWayTest(HWMap hwMap, SwerveDrivetrain drivetrain,
                               PinpointLocalizer localizer, ElapsedTime timer) {

        DriveContext context = new DriveContext(drivetrain, localizer, hwMap);
        DriveScheduler scheduler = new DriveScheduler(25); // Supports up to 25 commands

        double radius = Math.max(Math.abs(TARGET_X_IN), 24.0); // Use TARGET_X_IN or 24" default

        // 1. Eight-way star pattern
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            double targetX = radius * Math.cos(angle);
            double targetY = radius * Math.sin(angle);

            // Drive outwards
            scheduler.add(new MoveToPoseCommand(targetX, targetY, 0.0,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
            
            // Return to origin
            scheduler.add(new MoveToPoseCommand(0.0, 0.0, 0.0,
                SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
        }

        // 2. Rotations (90 degrees CCW, return to 0, 90 degrees CW, return to 0)
        scheduler.add(new MoveToPoseCommand(0.0, 0.0, Math.toRadians(90.0),
            SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
        scheduler.add(new MoveToPoseCommand(0.0, 0.0, 0.0,
            SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
        scheduler.add(new MoveToPoseCommand(0.0, 0.0, Math.toRadians(-90.0),
            SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
        scheduler.add(new MoveToPoseCommand(0.0, 0.0, 0.0,
            SwerveConfig.AUTO_SETTLE_DELAY_MS, SwerveConfig.AUTO_MOVE_TIMEOUT_MS));
        
        scheduler.add(new StopDriveCommand());

        while (opModeIsActive() && !scheduler.isFinished()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            double dt = Math.max(1e-3, timer.seconds());
            timer.reset();

            handleReset(localizer);

            scheduler.tick(context, dt);
            drivetrain.write(dt);

            telemetry.addData("── EIGHT WAY TEST ──", "");
            telemetry.addData("cmd", scheduler.getCurrentCommandName());
            telemetry.addData("aborted", scheduler.wasAborted());
            telemetry.addData("pose", "%.1f, %.1f, %.1f",
                localizer.getXInches(), localizer.getYInches(),
                Math.toDegrees(localizer.getHeadingRadians()));
            telemetry.update();
        }

        telemetry.addData("DONE", "aborted=" + scheduler.wasAborted());
        telemetry.update();
        sleep(5000);
    }

    // ── Shared helper ─────────────────────────────────────────────────────────
    private void handleReset(PinpointLocalizer localizer) {
        if (RESET_POSE) {
            RESET_POSE = false;
            localizer.setPose(0, 0, 0);
        }
    }
}
