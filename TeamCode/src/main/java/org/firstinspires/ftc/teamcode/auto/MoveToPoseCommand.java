package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

public class MoveToPoseCommand implements DriveCommand {
    private final double targetXInches;
    private final double targetYInches;
    private final double targetHeadingRadians;
    private final double settleDelayMs;
    private final double timeoutMs;
    private final double[] command = new double[3];

    private final PIDController xController =
            new PIDController(SwerveConfig.AUTO_X_P, 0.0, SwerveConfig.AUTO_X_D);
    private final PIDController yController =
            new PIDController(SwerveConfig.AUTO_Y_P, 0.0, SwerveConfig.AUTO_Y_D);
    private final PIDController headingController =
            new PIDController(SwerveConfig.AUTO_HEADING_P, 0.0, SwerveConfig.AUTO_HEADING_D);

    private double elapsedMs = 0.0;
    private double settledMs = 0.0;

    public MoveToPoseCommand(double targetXInches, double targetYInches, double targetHeadingRadians) {
        this(targetXInches, targetYInches, targetHeadingRadians,
                SwerveConfig.AUTO_SETTLE_DELAY_MS,
                SwerveConfig.AUTO_MOVE_TIMEOUT_MS);
    }

    public MoveToPoseCommand(double targetXInches, double targetYInches, double targetHeadingRadians,
            double settleDelayMs, double timeoutMs) {
        this.targetXInches = targetXInches;
        this.targetYInches = targetYInches;
        this.targetHeadingRadians = targetHeadingRadians;
        this.settleDelayMs = settleDelayMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void init(DriveContext context) {
        elapsedMs = 0.0;
        settledMs = 0.0;
        xController.reset();
        yController.reset();
        headingController.reset();
    }

    @Override
    public void tick(DriveContext context, double dt) {
        elapsedMs += dt * 1000.0;
        xController.setPID(SwerveConfig.AUTO_X_P, 0.0, SwerveConfig.AUTO_X_D);
        yController.setPID(SwerveConfig.AUTO_Y_P, 0.0, SwerveConfig.AUTO_Y_D);
        headingController.setPID(SwerveConfig.AUTO_HEADING_P, 0.0, SwerveConfig.AUTO_HEADING_D);
        AutoMath.calculateKookyPowers(context.pose, targetXInches, targetYInches, targetHeadingRadians,
                xController, yController, headingController, dt, command);
        context.drivetrain.set(command[0], command[1], command[2], dt);

        if (insideTolerance(context)) {
            settledMs += dt * 1000.0;
        } else {
            settledMs = 0.0;
        }
    }

    @Override
    public boolean isFinished(DriveContext context) {
        return elapsedMs >= timeoutMs || settledMs >= settleDelayMs;
    }

    @Override
    public void end(DriveContext context, boolean interrupted) {
        context.drivetrain.set(0.0, 0.0, 0.0, 0.02);
    }

    @Override
    public String getName() {
        return "move_pose";
    }

    private boolean insideTolerance(DriveContext context) {
        return AutoMath.translationError(context.pose, targetXInches, targetYInches)
                <= SwerveConfig.AUTO_TRANSLATION_TOLERANCE_IN
                && AutoMath.headingError(context.pose, targetHeadingRadians)
                <= SwerveConfig.AUTO_HEADING_TOLERANCE_RAD;
    }
}
