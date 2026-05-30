package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;

public class DriveContext {
    public final SwerveDrivetrain drivetrain;
    public final AutoPoseProvider pose;
    public final HWMap hwMap;

    private boolean abortRequested = false;

    public DriveContext(SwerveDrivetrain drivetrain, AutoPoseProvider pose, HWMap hwMap) {
        this.drivetrain = drivetrain;
        this.pose = pose;
        this.hwMap = hwMap;
    }

    public void requestAbort() {
        abortRequested = true;
    }

    boolean consumeAbortRequest() {
        boolean result = abortRequested;
        abortRequested = false;
        return result;
    }
}
