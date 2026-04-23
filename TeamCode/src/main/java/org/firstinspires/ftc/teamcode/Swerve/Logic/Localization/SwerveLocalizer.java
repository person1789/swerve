package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;

/**
 * SwerveLocalizer
 * 
 * Multi-source fail-safe localization engine. Fuses absolute data from Pinpoint
 * with fallback orientation from IMU and relative dead-reckoning from the Velocity Observer.
 */
public class SwerveLocalizer {
    private final GoBildaPinpointDriver odo;
    private final IMU imu;
    
    private Vector masterPose = new Vector(0, 0, 0); // [X, Y, Heading]
    private boolean pinpointPreviouslyHealthy = false;
    private double headingOffset = 0.0;

    public SwerveLocalizer(HWMap hwMap) {
        this.odo = hwMap.getOdo();
        this.imu = hwMap.imu;
        
        odo.setOffsets(SwerveConfig.ODO_X_OFFSET_MM, SwerveConfig.ODO_Y_OFFSET_MM, DistanceUnit.MM);
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD, GoBildaPinpointDriver.EncoderDirection.FORWARD);
        
        resetHeading();
    }

    /**
     * Updates the global robot pose by fusing all available sensors.
     * 
     * @param observedVelocity Chassis-relative velocity from the wheel encoders (Observer).
     * @param dt               Loop time in seconds.
     */
    public void update(Vector observedVelocity, double dt) {
        odo.update();
        
        boolean pinpointHealthy = (odo.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY);
        double heading;
        double x;
        double y;
        if (pinpointHealthy) {
            Pose2D pos = odo.getPosition();
            heading = odo.getHeading(AngleUnit.RADIANS);
            x = pos.getX(DistanceUnit.INCH);
            y = pos.getY(DistanceUnit.INCH);
            headingOffset = masterPose.omega() - imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
        } else {
            heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS) + headingOffset;
            Vector worldVelocity = observedVelocity.rotate(masterPose.omega());
            x = masterPose.x() + worldVelocity.x() * dt;
            y = masterPose.y() + worldVelocity.y() * dt;
        }

        masterPose = new Vector(x, y, heading);
        pinpointPreviouslyHealthy = pinpointHealthy;
    }

    public Vector getPose() {
        return masterPose;
    }

    public double getHeading() {
        return masterPose.omega();
    }

    public void resetHeading() {
        odo.resetPosAndIMU();
        imu.resetYaw();
        headingOffset = 0.0;
        masterPose = new Vector(masterPose.x(), masterPose.y(), 0);
    }

    public void setPose(Vector pose) {
        this.masterPose = pose;
        headingOffset = pose.omega() - imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
        odo.setPosition(new Pose2D(DistanceUnit.INCH, pose.x(), pose.y(), AngleUnit.RADIANS, pose.omega()));
        // Note: IMU doesn't support setting an arbitrary yaw, only resetting to 0.
        // The masterPose will track the offset internally.
    }
}
