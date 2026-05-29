package org.firstinspires.ftc.teamcode.auto;

public class WaitForAzimuthCommand implements DriveCommand {
    private final double targetXInches;
    private final double targetYInches;
    private final double targetHeadingRadians;
    private final double toleranceRadians;
    private final double timeoutMs;
    private final boolean abortOnTimeout;
    private final double[] command = new double[3];

    private double elapsedMs = 0.0;
    private boolean timedOut = false;
    private int commandedLoops = 0;
    private boolean feedbackArmed = false;

    public WaitForAzimuthCommand(double targetXInches, double targetYInches, double targetHeadingRadians,
            double toleranceRadians, double timeoutMs, boolean abortOnTimeout) {
        this.targetXInches = targetXInches;
        this.targetYInches = targetYInches;
        this.targetHeadingRadians = targetHeadingRadians;
        this.toleranceRadians = toleranceRadians;
        this.timeoutMs = timeoutMs;
        this.abortOnTimeout = abortOnTimeout;
    }

    @Override
    public void init(DriveContext context) {
        elapsedMs = 0.0;
        timedOut = false;
        commandedLoops = 0;
        feedbackArmed = false;
    }

    @Override
    public void tick(DriveContext context, double dt) {
        elapsedMs += dt * 1000.0;
        feedbackArmed = commandedLoops > 0;
        AutoMath.calculateInitialPowers(context.pose, targetXInches, targetYInches, targetHeadingRadians, command);
        context.drivetrain.pointModulesForCommand(command[0], command[1], command[2], dt);

        timedOut = feedbackArmed && elapsedMs >= timeoutMs;
        if (timedOut && abortOnTimeout && !context.drivetrain.areModulesAzimuthReady(toleranceRadians)) {
            context.requestAbort();
        }
        commandedLoops++;
    }

    @Override
    public boolean isFinished(DriveContext context) {
        return feedbackArmed && (context.drivetrain.areModulesAzimuthReady(toleranceRadians) || timedOut);
    }

    @Override
    public void end(DriveContext context, boolean interrupted) {
        context.drivetrain.set(0.0, 0.0, 0.0, 0.02);
    }

    @Override
    public String getName() {
        return "wait_azimuth";
    }
}
