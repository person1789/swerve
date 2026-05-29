package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;

public class PinpointLocalizer implements AutoPoseProvider {
    private final GoBildaPinpointDriver pinpoint;

    private double xInches = 0.0;
    private double yInches = 0.0;
    private double headingRadians = 0.0;
    private boolean ready = false;

    public PinpointLocalizer(HWMap hwMap) {
        pinpoint = hwMap.getOdo();
        pinpoint.setOffsets(SwerveConfig.ODO_X_OFFSET_MM, SwerveConfig.ODO_Y_OFFSET_MM, DistanceUnit.MM);
        pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        pinpoint.setEncoderDirections(
                GoBildaPinpointDriver.EncoderDirection.FORWARD,
                GoBildaPinpointDriver.EncoderDirection.FORWARD);
    }

    @Override
    public void update() {
        pinpoint.update();
        Pose2D pose = pinpoint.getPosition();
        ready = pinpoint.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY && pose != null;
        if (!ready) {
            return;
        }

        xInches = pose.getX(DistanceUnit.INCH);
        yInches = pose.getY(DistanceUnit.INCH);
        headingRadians = pinpoint.getHeading(AngleUnit.RADIANS);
    }

    public void setPose(double xInches, double yInches, double headingRadians) {
        pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, xInches, yInches, AngleUnit.RADIANS, headingRadians));
        this.xInches = xInches;
        this.yInches = yInches;
        this.headingRadians = headingRadians;
    }

    @Override
    public double getXInches() {
        return xInches;
    }

    @Override
    public double getYInches() {
        return yInches;
    }

    @Override
    public double getHeadingRadians() {
        return headingRadians;
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}
