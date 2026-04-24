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
    private Vector rawPinpointPose = new Vector(Double.NaN, Double.NaN, Double.NaN);
    private boolean pinpointPreviouslyHealthy = false;
    private boolean usingPinpoint = false;
    private int consecutiveInvalidPinpointLoops = 0;
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

        boolean pinpointDeviceReady = (odo.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY);
        Pose2D pos = odo.getPosition();
        double rawHeading = odo.getHeading(AngleUnit.RADIANS);
        double rawX = pos != null ? pos.getX(DistanceUnit.INCH) : Double.NaN;
        double rawY = pos != null ? pos.getY(DistanceUnit.INCH) : Double.NaN;
        rawPinpointPose = new Vector(rawX, rawY, rawHeading);

        boolean pinpointHealthy = pinpointDeviceReady
                && isFinite(rawX)
                && isFinite(rawY)
                && isFinite(rawHeading);

        double heading;
        double x;
        double y;
        if (pinpointHealthy) {
            heading = rawHeading;
            x = rawX;
            y = rawY;

            double imuYaw = getFiniteImuYawRadians();
            if (isFinite(imuYaw)) {
                headingOffset = masterPose.omega() - imuYaw;
            }
        } else {
            if (pinpointPreviouslyHealthy) {
                double imuYaw = getFiniteImuYawRadians();
                if (isFinite(imuYaw)) {
                    headingOffset = masterPose.omega() - imuYaw;
                }
            }

            double imuYaw = getFiniteImuYawRadians();
            heading = isFinite(imuYaw) ? imuYaw + headingOffset : masterPose.omega();

            Vector safeObservedVelocity = sanitizeVelocity(observedVelocity);
            Vector worldVelocity = safeObservedVelocity.rotate(masterPose.omega());
            x = masterPose.x() + metersToInches(worldVelocity.x()) * dt;
            y = masterPose.y() + metersToInches(worldVelocity.y()) * dt;
        }

        masterPose = new Vector(x, y, heading);
        usingPinpoint = pinpointHealthy;
        consecutiveInvalidPinpointLoops = pinpointHealthy ? 0 : (consecutiveInvalidPinpointLoops + 1);
        pinpointPreviouslyHealthy = pinpointHealthy;
    }

    public Vector getPose() {
        return masterPose;
    }

    public double getHeading() {
        return masterPose.omega();
    }

    public Vector getRawPinpointPose() {
        return rawPinpointPose;
    }

    public boolean isUsingPinpoint() {
        return usingPinpoint;
    }

    public int getConsecutiveInvalidPinpointLoops() {
        return consecutiveInvalidPinpointLoops;
    }

    public void resetHeading() {
        Pose2D currentPos = odo.getPosition();
        double x = masterPose.x();
        double y = masterPose.y();

        if (currentPos != null) {
            double currentX = currentPos.getX(DistanceUnit.INCH);
            double currentY = currentPos.getY(DistanceUnit.INCH);
            if (isFinite(currentX)) {
                x = currentX;
            }
            if (isFinite(currentY)) {
                y = currentY;
            }
        }

        imu.resetYaw();
        odo.setPosition(new Pose2D(DistanceUnit.INCH, x, y, AngleUnit.RADIANS, 0.0));
        headingOffset = 0.0;
        masterPose = new Vector(x, y, 0.0);
        rawPinpointPose = new Vector(x, y, 0.0);
        usingPinpoint = false;
        consecutiveInvalidPinpointLoops = 0;
        pinpointPreviouslyHealthy = false;
    }

    public void setPose(Vector pose) {
        Vector safePose = sanitizePose(pose, masterPose);
        this.masterPose = safePose;

        double imuYaw = getFiniteImuYawRadians();
        if (isFinite(imuYaw)) {
            headingOffset = safePose.omega() - imuYaw;
        }

        odo.setPosition(new Pose2D(DistanceUnit.INCH, safePose.x(), safePose.y(), AngleUnit.RADIANS, safePose.omega()));
        // Note: IMU doesn't support setting an arbitrary yaw, only resetting to 0.
        // The masterPose will track the offset internally.
    }

    /**
     * Applies a vision-based pose correction from LimelightLocalizer.
     *
     * Two correction modes:
     *
     * 1. HARD RESET — if the Euclidean distance between the vision pose and the
     *    current masterPose exceeds {@code SwerveConfig.LIMELIGHT_HARD_RESET_THRESHOLD_IN},
     *    the X/Y components are snapped directly to the vision estimate. This
     *    recovers from large Pinpoint drift in one step.
     *
     * 2. SOFT BLEND — otherwise, masterPose is lerped toward the vision pose by
     *    {@code trustFactor}, which scales with tag count (see LimelightLocalizer).
     *    e.g. trustFactor = 0.10 → 10% vision, 90% odometry this cycle.
     *
     * Heading is NEVER sourced from vision. The omega() component of the
     * masterPose is always preserved from the Pinpoint/IMU pipeline. The
     * visionPose heading is carried along for logging purposes only.
     *
     * @param visionPose  Field-space pose from LimelightLocalizer (inches, radians).
     * @param trustFactor Blending weight in [0.0, 1.0]. 0 = ignore, 1 = full snap.
     */
    public void applyVisionUpdate(Vector visionPose, double trustFactor) {
        if (visionPose == null || trustFactor <= 0.0) return;

        // Compute Euclidean XY error to decide correction mode.
        double errorX    = visionPose.x() - masterPose.x();
        double errorY    = visionPose.y() - masterPose.y();
        double errorDist = Math.sqrt(errorX * errorX + errorY * errorY);

        // Work in 2D (X, Y only) so the lerp never touches the heading component.
        Vector currentXY = new Vector(masterPose.x(), masterPose.y());
        Vector visionXY  = new Vector(visionPose.x(),  visionPose.y());

        Vector correctedXY;
        if (errorDist > SwerveConfig.LIMELIGHT_HARD_RESET_THRESHOLD_IN) {
            // Hard reset: snap directly to vision (no blending).
            correctedXY = visionXY;
        } else {
            // Soft blend: lerp from current toward vision by trustFactor.
            // currentXY.lerp(visionXY, alpha) = currentXY + alpha * (visionXY - currentXY)
            correctedXY = currentXY.lerp(visionXY, trustFactor);
        }

        // Heading always comes from Pinpoint/IMU — never from vision.
        masterPose = new Vector(correctedXY.x(), correctedXY.y(), masterPose.omega());
    }

    private double metersToInches(double meters) {
        return meters / 0.0254;
    }

    private double getFiniteImuYawRadians() {
        double yaw = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
        return isFinite(yaw) ? yaw : Double.NaN;
    }

    private Vector sanitizeVelocity(Vector observedVelocity) {
        double vx = isFinite(observedVelocity.x()) ? observedVelocity.x() : 0.0;
        double vy = isFinite(observedVelocity.y()) ? observedVelocity.y() : 0.0;
        double omega = isFinite(observedVelocity.omega()) ? observedVelocity.omega() : 0.0;
        return new Vector(vx, vy, omega);
    }

    private Vector sanitizePose(Vector candidate, Vector fallback) {
        double x = isFinite(candidate.x()) ? candidate.x() : fallback.x();
        double y = isFinite(candidate.y()) ? candidate.y() : fallback.y();
        double heading = isFinite(candidate.omega()) ? candidate.omega() : fallback.omega();
        return new Vector(x, y, heading);
    }

    private boolean isFinite(double value) {
        return Double.isFinite(value);
    }
}
