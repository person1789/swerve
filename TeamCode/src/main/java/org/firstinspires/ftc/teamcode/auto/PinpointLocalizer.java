package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;

public class PinpointLocalizer implements AutoPoseProvider {
    private final GoBildaPinpointDriver pinpoint;
    private final IMU imu;

    private double xInches = 0.0;
    private double yInches = 0.0;
    private double headingRadians = 0.0;
    private boolean ready = false;
    private boolean usingImuFallback = false;

    // Tracks consecutive pinpoint heading failures to trigger IMU fallback
    private int headingFailCount = 0;
    private static final int HEADING_FAIL_THRESHOLD = 5;
    
    // Offset to ensure IMU heading matches the Pinpoint field coordinate system 
    // even after the IMU is reset.
    private double imuOffsetRadians = 0.0;

    public PinpointLocalizer(HWMap hwMap) {
        pinpoint = hwMap.getOdo();
        imu = hwMap.imu;
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
        GoBildaPinpointDriver.DeviceStatus status = pinpoint.getDeviceStatus();
        ready = status == GoBildaPinpointDriver.DeviceStatus.READY && pose != null;

        if (!ready) {
            // Pinpoint completely down: fall back to IMU for heading, X/Y freeze at last known
            usingImuFallback = true;
            headingRadians = getImuHeading();
            return;
        }

        xInches = pose.getX(DistanceUnit.INCH);
        yInches = pose.getY(DistanceUnit.INCH);

        // Use pinpoint heading as primary source
        double pinpointHeading = pinpoint.getHeading(AngleUnit.RADIANS);
        if (Double.isFinite(pinpointHeading)) {
            headingRadians = pinpointHeading;
            headingFailCount = 0;
            usingImuFallback = false;
        } else {
            headingFailCount++;
            if (headingFailCount >= HEADING_FAIL_THRESHOLD) {
                usingImuFallback = true;
                headingRadians = getImuHeading();
            }
            // If under threshold, keep last known heading
        }
    }

    public void setPose(double xInches, double yInches, double headingRadians) {
        pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, xInches, yInches, AngleUnit.RADIANS, headingRadians));
        imu.resetYaw();
        this.imuOffsetRadians = headingRadians;
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

    public boolean isUsingImuFallback() {
        return usingImuFallback;
    }

    private double getImuHeading() {
        YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
        return org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil.normalizeAngle(
            angles.getYaw(AngleUnit.RADIANS) + imuOffsetRadians
        );
    }
}
