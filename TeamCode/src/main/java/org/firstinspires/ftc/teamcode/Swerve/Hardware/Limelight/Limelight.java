package org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Limelight
 *
 * Thin hardware wrapper around the Limelight3A FTC driver.
 * Responsible only for low-level I/O: initialization, pipeline selection,
 * IMU orientation seeding, and raw result retrieval.
 *
 * All confidence-gating and pose conversion logic lives in LimelightLocalizer.
 */
public class Limelight {

    private final Limelight3A ll;

    /**
     * Initializes the Limelight3A and starts the AprilTag pipeline.
     *
     * @param hardwareMap The OpMode hardware map. Requires a Limelight3A
     *                    named "limelight" in the hardware configuration.
     */
    public Limelight(HardwareMap hardwareMap) {
        ll = hardwareMap.get(Limelight3A.class, "limelight");
        ll.setPollRateHz(100); // Request data at up to 100 Hz
        ll.pipelineSwitch(0);  // Pipeline 0 must be configured as AprilTag (Full 3D) in the web UI
        ll.start();
    }

    /**
     * Seeds MegaTag2 with the robot's current yaw. Must be called every loop
     * before reading the pose estimate, or MT2 will use a stale orientation.
     *
     * @param yawDegrees Current robot heading in degrees (from Pinpoint or IMU).
     *                   CCW-positive, 0 = facing the direction defined by the field map.
     */
    public void updateRobotOrientation(double yawDegrees) {
        // Full signature: yaw, yawRate, pitch, pitchRate, roll, rollRate
        // Rates are 0 — we handle angular velocity gating ourselves in LimelightLocalizer.
        ll.updateRobotOrientation(yawDegrees);
    }

    /**
     * Returns the most recent result computed by the Limelight pipeline.
     * May return null if the camera has not yet produced any output.
     */
    public LLResult getLatestResult() {
        return ll.getLatestResult();
    }

    /**
     * Returns the age of the latest result in milliseconds.
     * Returns Long.MAX_VALUE if no result is available.
     */
    public long getStaleness() {
        LLResult r = ll.getLatestResult();
        return r != null ? r.getStaleness() : Long.MAX_VALUE;
    }

    /**
     * Stops the Limelight pipeline. Call during OpMode stop if needed.
     */
    public void stop() {
        ll.stop();
    }
}
