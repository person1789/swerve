package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

@Config
public class Pinpoint {

    GoBildaPinpointDriver odo;
    Pose2D pos;
    public double x, y, heading;
    public static double Xoffset, Yoffset;
    private RobotSettings robotSettings;


    public Pinpoint(HWMap hwMap, RobotSettings robotSettings) {
        odo = hwMap.getOdo();
        this.robotSettings = robotSettings;
        Xoffset = -127.6669; Yoffset = -52.23;

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
        pos = odo.getPosition();
        x = pos.getX(DistanceUnit.INCH);
        y= pos.getY(DistanceUnit.INCH);
        heading = pos.getHeading(AngleUnit.DEGREES);
    }

    public void updateHeadingOnly() {
        odo.update(GoBildaPinpointDriver.ReadData.ONLY_UPDATE_HEADING);
        heading = Math.toDegrees(odo.getHeading(AngleUnit.DEGREES));
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
        odo.setPosition(new Pose2D(DistanceUnit.INCH, 89, 8, AngleUnit.DEGREES, 0));
    }


    public double getGoalDistance() {
        return Math.sqrt(Math.pow((robotSettings.alliance.getGoalPos().getX(DistanceUnit.METER) - x), 2) + Math.pow((robotSettings.alliance.getGoalPos().getY(DistanceUnit.METER) - y), 2));
    }

    public void setPosition(Pose2D pose2D) {
        odo.setPosition(pose2D);
        odo.update();
    }


    public double getHeadingErrorTrig() {
        double targetAngle;
        targetAngle = Math.toDegrees(Math.atan2((robotSettings.alliance.getGoalPos().getY(DistanceUnit.METER) - y), (robotSettings.alliance.getGoalPos().getX(DistanceUnit.METER) - x)));

        double error = targetAngle - heading;

        if(error <= -180) {
            error += 360;
        }
        else if (error >= 180) {
            error -= 360;
        }

        return error;
    }

}
