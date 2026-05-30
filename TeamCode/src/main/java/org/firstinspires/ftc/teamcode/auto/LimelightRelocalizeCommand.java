package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.List;

public class LimelightRelocalizeCommand implements DriveCommand {

    private final boolean rotateToFindTarget;
    private final double rotationPower;
    
    // Config limits
    private static final double MAX_SEARCH_TIME_MS = 500.0;
    private static final double REQUIRED_VISIBLE_TIME_MS = 200.0;
    private static final int PIPELINE_INDEX = 1;

    private boolean isFinished = false;
    private double totalTimeMs = 0;
    private double visibleTimeMs = 0;

    private Limelight3A limelight;

    public LimelightRelocalizeCommand(boolean rotateToFindTarget, double rotationPower) {
        this.rotateToFindTarget = rotateToFindTarget;
        this.rotationPower = rotationPower;
    }

    @Override
    public void init(DriveContext context) {
        this.limelight = context.hwMap.limelight;
        if (this.limelight != null) {
            this.limelight.setPollRateHz(100);
            this.limelight.start();
            this.limelight.pipelineSwitch(PIPELINE_INDEX);
        } else {
            // Limelight not found in hardware map, abort instantly
            isFinished = true;
        }
    }

    @Override
    public void tick(DriveContext context, double dt) {
        if (isFinished || limelight == null) return;
        
        double dtMs = dt * 1000.0;
        totalTimeMs += dtMs;

        // Give up if we exceeded our search timeout
        if (totalTimeMs > MAX_SEARCH_TIME_MS) {
            isFinished = true;
            context.drivetrain.set(0, 0, 0, dt); // stop moving
            return;
        }

        // 1. Seed the Limelight MT2 with the current Pinpoint heading
        // Limelight expects heading in degrees, CCW positive (same as our Pinpoint)
        double currentHeadingDeg = Math.toDegrees(context.pose.getHeadingRadians());
        limelight.updateRobotOrientation(currentHeadingDeg);

        // 2. Fetch the latest camera result
        LLResult result = limelight.getLatestResult();
        boolean seesValidTarget = false;

        if (result != null && result.isValid()) {
            List<LLResultTypes.FiducialResult> fiducials = result.getFiducialResults();
            if (fiducials != null && !fiducials.isEmpty()) {
                seesValidTarget = true;
            }
        }

        if (seesValidTarget) {
            // Target seen! Accumulate visible time and stop spinning
            visibleTimeMs += dtMs;
            context.drivetrain.set(0, 0, 0, dt);

            if (visibleTimeMs >= REQUIRED_VISIBLE_TIME_MS) {
                // We've seen it clearly for 200ms. Apply the MT2 pose.
                Pose3D botpose = result.getBotpose_MT2();
                if (botpose != null) {
                    double visionX = botpose.getPosition().x * 39.3701; // meters to inches
                    double visionY = botpose.getPosition().y * 39.3701;
                    double visionHeading = Math.toRadians(botpose.getOrientation().getYaw());
                    
                    // Snap the localizer to the absolute field coordinate
                    context.pose.setPose(visionX, visionY, visionHeading);
                }
                
                isFinished = true;
            }
        } else {
            // Target not seen
            visibleTimeMs = 0; // Reset consecutive visible timer
            
            if (rotateToFindTarget) {
                // Spin in place to search
                context.drivetrain.set(0, 0, rotationPower, dt);
            } else {
                // Just wait quietly
                context.drivetrain.set(0, 0, 0, dt);
            }
        }
    }

    @Override
    public boolean isFinished(DriveContext context) {
        return isFinished;
    }

    @Override
    public void end(DriveContext context, boolean interrupted) {
        if (limelight != null) {
            limelight.stop();
        }
    }

    @Override
    public String getName() {
        return "LimelightRelocalize";
    }
}
