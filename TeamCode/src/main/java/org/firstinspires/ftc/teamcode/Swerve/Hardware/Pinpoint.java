package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Core.RobotSettings;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

@Config
@Deprecated
/**
 * Legacy Pinpoint wrapper retained for older experiments.
 *
 * The active teleop/localization path uses {@code SwerveLocalizer} directly.
 * This class is therefore redundant for the current driver-control stack and
 * should not be used for new code without first reconciling its unit and reset
 * semantics.
 */
public class Pinpoint {

    GoBildaPinpointDriver odo;
    Pose2D pos;
    public double x, y, heading;
    public static double Xoffset, Yoffset;
    private RobotSettings robotSettings;


    public Pinpoint(HWMap hwMap, RobotSettings robotSettings) {
        odo = hwMap.getOdo();
        this.robotSettings = robotSettings;
        Xoffset = SwerveConfig.ODO_X_OFFSET_MM;
        Yoffset = SwerveConfig.ODO_Y_OFFSET_MM;

        odo.setOffsets(Xoffset, Yoffset, DistanceUnit.MM);
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections( GoBildaPinpointDriver.EncoderDirection.FORWARD,  GoBildaPinpointDriver.EncoderDirection.FORWARD);

        odo.setPosition(robotSettings.startPosState.getPose2D());
        update();
    }

    public boolean pinpointReady() {
        return odo.getDeviceStatus() == GoBildaPinpointDriver.DeviceStatus.READY;
    }

    public void update() {
        odo.update();
        Pose2D nextPos = odo.getPosition();
        double nextX = nextPos != null ? nextPos.getX(DistanceUnit.INCH) : Double.NaN;
        double nextY = nextPos != null ? nextPos.getY(DistanceUnit.INCH) : Double.NaN;
        double nextHeading = nextPos != null ? nextPos.getHeading(AngleUnit.DEGREES) : Double.NaN;

        pos = nextPos;
        if (Double.isFinite(nextX)) {
            x = nextX;
        }
        if (Double.isFinite(nextY)) {
            y = nextY;
        }
        if (Double.isFinite(nextHeading)) {
            heading = nextHeading;
        }
    }

    public void updateHeadingOnly() {
        odo.update(GoBildaPinpointDriver.ReadData.ONLY_UPDATE_HEADING);
        double nextHeading = odo.getHeading(AngleUnit.DEGREES);
        if (Double.isFinite(nextHeading)) {
            heading = nextHeading;
        }
    }

    public Pose2D getPos() {
        return pos;
    }

    public double getHeading() {
        return heading;
    }


    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }

    public void resetIMU() {
        Pose2D current = pos != null ? pos : odo.getPosition();
        double currentX = current != null && Double.isFinite(current.getX(DistanceUnit.INCH)) ? current.getX(DistanceUnit.INCH) : x;
        double currentY = current != null && Double.isFinite(current.getY(DistanceUnit.INCH)) ? current.getY(DistanceUnit.INCH) : y;
        odo.setPosition(new Pose2D(DistanceUnit.INCH, currentX, currentY, AngleUnit.DEGREES, 0));
        heading = 0.0;
        x = currentX;
        y = currentY;
    }


    public double getGoalDistance() {
        return distanceToGoalInches(x, y, robotSettings.alliance.getGoalPos());
    }

    public void setPosition(Pose2D pose2D) {
        odo.setPosition(pose2D);
        odo.update();
    }


    public double getHeadingErrorTrig() {
        return headingErrorToGoalDegrees(x, y, heading, robotSettings.alliance.getGoalPos());
    }

    static double distanceToGoalInches(double robotXInches, double robotYInches, Pose2D goalPose) {
        double dx = goalPose.getX(DistanceUnit.INCH) - robotXInches;
        double dy = goalPose.getY(DistanceUnit.INCH) - robotYInches;
        return Math.hypot(dx, dy);
    }

    static double headingErrorToGoalDegrees(double robotXInches, double robotYInches, double robotHeadingDegrees, Pose2D goalPose) {
        double targetAngle = Math.toDegrees(Math.atan2(
                goalPose.getY(DistanceUnit.INCH) - robotYInches,
                goalPose.getX(DistanceUnit.INCH) - robotXInches));

        double error = targetAngle - robotHeadingDegrees;
        if (error <= -180) {
            error += 360;
        } else if (error >= 180) {
            error -= 360;
        }
        return error;
    }

}
