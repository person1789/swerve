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
 * Multi-source fail-safe localization engine. Fuses absolute data from Pinpoint
 * with fallback orientation from IMU and relative dead-reckoning from the
 * velocity observer.
 */
public class SwerveLocalizer {
    private final GoBildaPinpointDriver odo;
    private final IMU imu;

    private double poseXInches = 0.0;
    private double poseYInches = 0.0;
    private double poseHeadingRadians = 0.0;
    private double rawPinpointXInches = Double.NaN;
    private double rawPinpointYInches = Double.NaN;
    private double rawPinpointHeadingRadians = Double.NaN;

    private boolean pinpointPreviouslyHealthy = false;
    private boolean usingPinpoint = false;
    private int consecutiveInvalidPinpointLoops = 0;
    private double headingOffset = 0.0;
    private boolean cachedPinpointReady = false;
    private double cachedPinpointXInches = Double.NaN;
    private double cachedPinpointYInches = Double.NaN;
    private double cachedPinpointHeadingRadians = Double.NaN;
    private double cachedImuYawRadians = Double.NaN;

    public SwerveLocalizer(HWMap hwMap) {
        this.odo = hwMap.getOdo();
        this.imu = hwMap.imu;

        odo.setOffsets(SwerveConfig.ODO_X_OFFSET_MM, SwerveConfig.ODO_Y_OFFSET_MM, DistanceUnit.MM);
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD, GoBildaPinpointDriver.EncoderDirection.FORWARD);

        refreshSensors();
        resetHeading();
    }

    public void refreshSensors() {
        odo.update();

        cachedPinpointReady = (odo.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY);
        Pose2D position = odo.getPosition();
        cachedPinpointHeadingRadians = odo.getHeading(AngleUnit.RADIANS);
        cachedPinpointXInches = position != null ? position.getX(DistanceUnit.INCH) : Double.NaN;
        cachedPinpointYInches = position != null ? position.getY(DistanceUnit.INCH) : Double.NaN;
        cachedImuYawRadians = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
        rawPinpointXInches = cachedPinpointXInches;
        rawPinpointYInches = cachedPinpointYInches;
        rawPinpointHeadingRadians = cachedPinpointHeadingRadians;
    }

    public void update(Vector observedVelocity, double dt) {
        boolean pinpointHealthy = cachedPinpointReady
                && isFinite(cachedPinpointXInches)
                && isFinite(cachedPinpointYInches)
                && isFinite(cachedPinpointHeadingRadians);

        double heading;
        double x;
        double y;
        if (pinpointHealthy) {
            heading = cachedPinpointHeadingRadians;
            x = cachedPinpointXInches;
            y = cachedPinpointYInches;

            double imuYaw = getFiniteCachedImuYawRadians();
            if (isFinite(imuYaw)) {
                headingOffset = poseHeadingRadians - imuYaw;
            }
        } else {
            if (pinpointPreviouslyHealthy) {
                double imuYaw = getFiniteCachedImuYawRadians();
                if (isFinite(imuYaw)) {
                    headingOffset = poseHeadingRadians - imuYaw;
                }
            }

            double imuYaw = getFiniteCachedImuYawRadians();
            heading = isFinite(imuYaw) ? imuYaw + headingOffset : poseHeadingRadians;

            double observedVx = sanitizeVelocityComponent(observedVelocity.x());
            double observedVy = sanitizeVelocityComponent(observedVelocity.y());
            double cos = Math.cos(poseHeadingRadians);
            double sin = Math.sin(poseHeadingRadians);
            double worldVx = observedVx * cos - observedVy * sin;
            double worldVy = observedVx * sin + observedVy * cos;
            x = poseXInches + metersToInches(worldVx) * dt;
            y = poseYInches + metersToInches(worldVy) * dt;
        }

        poseXInches = x;
        poseYInches = y;
        poseHeadingRadians = heading;
        usingPinpoint = pinpointHealthy;
        consecutiveInvalidPinpointLoops = pinpointHealthy ? 0 : (consecutiveInvalidPinpointLoops + 1);
        pinpointPreviouslyHealthy = pinpointHealthy;
    }

    public Vector getPose() {
        return new Vector(poseXInches, poseYInches, poseHeadingRadians);
    }

    public double getPoseXInches() {
        return poseXInches;
    }

    public double getPoseYInches() {
        return poseYInches;
    }

    public double getHeading() {
        return poseHeadingRadians;
    }

    public Vector getRawPinpointPose() {
        return new Vector(rawPinpointXInches, rawPinpointYInches, rawPinpointHeadingRadians);
    }

    public double getRawPinpointXInches() {
        return rawPinpointXInches;
    }

    public double getRawPinpointYInches() {
        return rawPinpointYInches;
    }

    public double getRawPinpointHeadingRadians() {
        return rawPinpointHeadingRadians;
    }

    public boolean isUsingPinpoint() {
        return usingPinpoint;
    }

    public int getConsecutiveInvalidPinpointLoops() {
        return consecutiveInvalidPinpointLoops;
    }

    public void resetHeading() {
        double x = poseXInches;
        double y = poseYInches;

        if (isFinite(cachedPinpointXInches)) {
            x = cachedPinpointXInches;
        }
        if (isFinite(cachedPinpointYInches)) {
            y = cachedPinpointYInches;
        }

        imu.resetYaw();
        odo.setPosition(new Pose2D(DistanceUnit.INCH, x, y, AngleUnit.RADIANS, 0.0));
        headingOffset = 0.0;
        poseXInches = x;
        poseYInches = y;
        poseHeadingRadians = 0.0;
        rawPinpointXInches = x;
        rawPinpointYInches = y;
        rawPinpointHeadingRadians = 0.0;
        usingPinpoint = false;
        consecutiveInvalidPinpointLoops = 0;
        pinpointPreviouslyHealthy = false;
        cachedImuYawRadians = 0.0;
        cachedPinpointHeadingRadians = 0.0;
        cachedPinpointXInches = x;
        cachedPinpointYInches = y;
        cachedPinpointReady = false;
    }

    public void setPose(Vector pose) {
        double safeX = sanitizePoseComponent(pose.x(), poseXInches);
        double safeY = sanitizePoseComponent(pose.y(), poseYInches);
        double safeHeading = sanitizePoseComponent(pose.omega(), poseHeadingRadians);

        poseXInches = safeX;
        poseYInches = safeY;
        poseHeadingRadians = safeHeading;

        double imuYaw = getFiniteCachedImuYawRadians();
        if (isFinite(imuYaw)) {
            headingOffset = safeHeading - imuYaw;
        }

        odo.setPosition(new Pose2D(DistanceUnit.INCH, safeX, safeY, AngleUnit.RADIANS, safeHeading));
        cachedPinpointXInches = safeX;
        cachedPinpointYInches = safeY;
        cachedPinpointHeadingRadians = safeHeading;
        rawPinpointXInches = safeX;
        rawPinpointYInches = safeY;
        rawPinpointHeadingRadians = safeHeading;
    }

    public void applyVisionUpdate(Vector visionPose, double trustFactor) {
        if (visionPose == null || trustFactor <= 0.0) {
            return;
        }

        double errorX = visionPose.x() - poseXInches;
        double errorY = visionPose.y() - poseYInches;
        double errorDist = Math.sqrt(errorX * errorX + errorY * errorY);

        if (errorDist > SwerveConfig.LIMELIGHT_HARD_RESET_THRESHOLD_IN) {
            poseXInches = visionPose.x();
            poseYInches = visionPose.y();
        } else {
            poseXInches += trustFactor * (visionPose.x() - poseXInches);
            poseYInches += trustFactor * (visionPose.y() - poseYInches);
        }
    }

    private double metersToInches(double meters) {
        return meters / 0.0254;
    }

    private double getFiniteCachedImuYawRadians() {
        return isFinite(cachedImuYawRadians) ? cachedImuYawRadians : Double.NaN;
    }

    private double sanitizeVelocityComponent(double value) {
        return isFinite(value) ? value : 0.0;
    }

    private double sanitizePoseComponent(double candidate, double fallback) {
        return isFinite(candidate) ? candidate : fallback;
    }

    private boolean isFinite(double value) {
        return Double.isFinite(value);
    }
}
