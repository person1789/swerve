package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

public class DriveScheduler {
    private final DriveCommand[] commands;
    private int count = 0;
    private int index = 0;
    private boolean currentInitialized = false;
    private boolean aborted = false;

    public DriveScheduler(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("DriveScheduler capacity must be positive.");
        }
        commands = new DriveCommand[capacity];
    }

    public DriveScheduler add(DriveCommand command) {
        if (count >= commands.length) {
            throw new IllegalStateException("DriveScheduler queue is full.");
        }
        commands[count++] = command;
        return this;
    }

    public DriveScheduler addLimelightRelocalize(boolean rotateToFindTarget) {
        return add(new LimelightRelocalizeCommand(rotateToFindTarget, 0.3));
    }

    public DriveScheduler addMoveToPose(double xInches, double yInches, double headingRadians,
            boolean waitForAzimuth) {
        return addMoveToPose(xInches, yInches, headingRadians, waitForAzimuth,
                SwerveConfig.AUTO_SETTLE_DELAY_MS,
                SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
    }

    public DriveScheduler addMoveToPose(double xInches, double yInches, double headingRadians,
            boolean waitForAzimuth, double settleDelayMs, double timeoutMs) {
        if (waitForAzimuth) {
            add(new WaitForAzimuthCommand(xInches, yInches, headingRadians,
                    SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD,
                    SwerveConfig.AUTO_AZIMUTH_TIMEOUT_MS,
                    SwerveConfig.AUTO_ABORT_ON_AZIMUTH_TIMEOUT));
        }
        return add(new MoveToPoseCommand(xInches, yInches, headingRadians, settleDelayMs, timeoutMs));
    }

    public void tick(DriveContext context, double dt) {
        if (isFinished()) {
            context.drivetrain.set(0.0, 0.0, 0.0, dt);
            return;
        }

        DriveCommand current = commands[index];
        if (!currentInitialized) {
            current.init(context);
            currentInitialized = true;
        }

        current.tick(context, dt);

        if (context.consumeAbortRequest()) {
            current.end(context, true);
            aborted = true;
            index = count;
            currentInitialized = false;
            context.drivetrain.set(0.0, 0.0, 0.0, dt);
            return;
        }

        if (current.isFinished(context)) {
            current.end(context, false);
            index++;
            currentInitialized = false;
        }
    }

    public boolean isFinished() {
        return index >= count;
    }

    public boolean wasAborted() {
        return aborted;
    }

    public int getCurrentIndex() {
        return index;
    }

    public String getCurrentCommandName() {
        return isFinished() ? "finished" : commands[index].getName();
    }
}
