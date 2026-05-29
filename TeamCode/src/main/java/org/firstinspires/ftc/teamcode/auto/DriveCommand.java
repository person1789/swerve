package org.firstinspires.ftc.teamcode.auto;

public interface DriveCommand {
    void init(DriveContext context);

    void tick(DriveContext context, double dt);

    boolean isFinished(DriveContext context);

    void end(DriveContext context, boolean interrupted);

    String getName();
}
