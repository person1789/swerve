package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.core.HWMap;

/**
 * SwerveLocalizer
 * 
 * Specialized wrapper for the GoBILDA Pinpoint odometry computer.
 * Provides the robot's current position and heading on the field
 * in a format optimized for swerve control logic.
 */
public class SwerveLocalizer {

    private final GoBildaPinpointDriver odo;
    
    /**
     * Initialize the Pinpoint localizer using hardware references from HWMap
     * and calibration offsets from SwerveConfig.
     */
    public SwerveLocalizer(HWMap hwMap) {
        this.odo = hwMap.getOdo();
        
        // Configure offsets from the central config file
        odo.setOffsets(SwerveConfig.ODO_X_OFFSET_MM, SwerveConfig.ODO_Y_OFFSET_MM, DistanceUnit.MM);
        
        // Standard Pinpoint configuration for goBILDA 4-bar pods
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD, 
                                 GoBildaPinpointDriver.EncoderDirection.FORWARD);
        
        // Start fresh
        resetHeading();
    }

    /**
     * Call this every loop to update the odometry computer.
     */
    public void update() {
        odo.update();
    }

    /**
     * Return the current robot pose (X, Y in inches, Heading in radians).
     */
    public Pose getPose() {
        Pose2D pos = odo.getPosition();
        // Convert to our Swerve/Geo/Pose format
        return new Pose(
            pos.getX(DistanceUnit.INCH),
            pos.getY(DistanceUnit.INCH),
            pos.getHeading(AngleUnit.RADIANS)
        );
    }

    /**
     * Return current heading in radians.
     */
    public double getHeading() {
        return odo.getHeading(AngleUnit.RADIANS);
    }

    /**
     * Reset the robot's heading to 0 (Field forward).
     */
    public void resetHeading() {
        odo.resetPosAndIMU();
    }

    /**
     * Set the robot's current position on the field.
     */
    public void setPose(Pose pose) {
        odo.setPosition(new Pose2D(
            DistanceUnit.INCH, 
            pose.x, 
            pose.y, 
            AngleUnit.RADIANS, 
            pose.heading
        ));
    }
}
