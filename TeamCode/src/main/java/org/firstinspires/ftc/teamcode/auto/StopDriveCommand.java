package org.firstinspires.ftc.teamcode.auto;

public class StopDriveCommand implements DriveCommand {
    private boolean finished = false;

    @Override
    public void init(DriveContext context) {
        finished = false;
    }

    @Override
    public void tick(DriveContext context, double dt) {
        context.drivetrain.set(0.0, 0.0, 0.0, dt);
        finished = true;
    }

    @Override
    public boolean isFinished(DriveContext context) {
        return finished;
    }

    @Override
    public void end(DriveContext context, boolean interrupted) {
        context.drivetrain.set(0.0, 0.0, 0.0, 0.02);
    }

    @Override
    public String getName() {
        return "stop";
    }
}
