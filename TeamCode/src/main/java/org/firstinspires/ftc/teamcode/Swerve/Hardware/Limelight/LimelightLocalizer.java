package org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

import java.util.List;

/**
 * LimelightLocalizer
 *
 * Processes MegaTag2 botpose estimates from the Limelight into field-space
 * pose corrections for SwerveLocalizer. All confidence gating, unit
 * conversion, and trust scoring lives here.
 *
 * Relocalization is fully reactive: a correction is produced every loop
 * cycle in which the camera has a valid, fresh result. There is no periodic
 * scheduling — when the robot sees a tag we correct; otherwise we defer to
 * Pinpoint dead-reckoning.
 *
 * Usage (inside an OpMode, guarded by SwerveConfig.LIMELIGHT_ENABLED):
 * <pre>
 *   limelightLocalizer.update(headingDeg, angularVelDegS);
 *   if (limelightLocalizer.hasVisionUpdate()) {
 *       swerveLocalizer.applyVisionUpdate(
 *           limelightLocalizer.getVisionPose(),
 *           limelightLocalizer.getTrustFactor());
 *   }
 * </pre>
 */
public class LimelightLocalizer extends Limelight {

    // ── State ────────────────────────────────────────────────────────────────
    private Vector  lastVisionPose        = null;  // null = no valid update this cycle
    private int     lastTagCount          = 0;
    private boolean visionUpdateAvailable = false;

    // ── Constructor ──────────────────────────────────────────────────────────

    public LimelightLocalizer(HardwareMap hardwareMap) {
        super(hardwareMap);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Call every loop BEFORE reading the pose estimate.
     *
     * Seeds MegaTag2 with the robot's current heading, fetches the latest
     * camera result, and runs all confidence gates. After this call, check
     * {@link #hasVisionUpdate()} to see whether a corrected pose is available.
     *
     * @param currentYawDegrees      Current robot heading in degrees (Pinpoint or IMU).
     * @param currentAngularVelDegS  Current angular velocity in degrees/second.
     *                               Used to suppress updates during fast spins.
     */
    public void update(double currentYawDegrees, double currentAngularVelDegS) {
        visionUpdateAvailable = false;
        lastVisionPose        = null;
        lastTagCount          = 0;

        // ── Step 1: Seed MegaTag2 with the current heading ──────────────────
        // Must be called every loop. MT2 uses this to resolve the pose ambiguity
        // that MT1 cannot handle with a single forward-facing camera.
        updateRobotOrientation(currentYawDegrees);

        // ── Step 2: Fetch the latest result ─────────────────────────────────
        // Relocalization fires every loop a valid result arrives — there is no
        // rate limiter. getLatestResult() returns the most recent frame computed
        // by the on-camera pipeline (up to 100 Hz).
        LLResult result = getLatestResult();
        if (result == null || !result.isValid()) return;

        // ── Step 3: Staleness guard ──────────────────────────────────────────
        // Rejects frames where the camera has NOT produced a new computation
        // since our last poll (e.g. tag momentarily out of frame). Applying a
        // stale pose correction moves our estimate to where the robot was — not
        // where it is — introducing error rather than reducing it.
        if (result.getStaleness() > SwerveConfig.LIMELIGHT_MAX_STALENESS_MS) return;

        // ── Step 4: Get MT2 botpose ──────────────────────────────────────────
        Pose3D botpose = result.getBotpose_MT2();
        if (botpose == null) return;

        // ── Step 5: Tag count ────────────────────────────────────────────────
        List<LLResultTypes.FiducialResult> fiducials = result.getFiducialResults();
        lastTagCount = (fiducials != null) ? fiducials.size() : 0;
        if (lastTagCount == 0) return;

        // ── Step 6: Single-tag distance gate ────────────────────────────────
        // With only one tag visible, accuracy degrades at long range.
        // getRobotPoseTargetSpace().getPosition().z is the depth (forward distance)
        // from the camera to the tag — a reliable confidence proxy.
        if (fiducials != null && lastTagCount == 1) {
            double depthM = fiducials.get(0).getRobotPoseTargetSpace().getPosition().z;
            if (depthM > SwerveConfig.LIMELIGHT_MAX_TAG_DISTANCE_M) return;
        }

        // ── Step 7: Angular velocity gate ───────────────────────────────────
        // Fast spins introduce IMU lag that corrupts MT2's heading seed, causing
        // transient XY errors. Suppress updates above the threshold.
        if (Math.abs(currentAngularVelDegS) > SwerveConfig.LIMELIGHT_MAX_ANGULAR_VEL_DEG_S) return;

        // ── Step 8: Unit conversion ──────────────────────────────────────────
        // Limelight returns meters (FTC field space); SwerveLocalizer uses inches.
        // Heading is carried along but is NEVER fused — SwerveLocalizer ignores it.
        double visionX   = botpose.getPosition().x * 39.3701; // m → in
        double visionY   = botpose.getPosition().y * 39.3701; // m → in
        double visionYaw = Math.toRadians(botpose.getOrientation().getYaw()); // deg → rad

        lastVisionPose        = new Vector(visionX, visionY, visionYaw);
        visionUpdateAvailable = true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns true if {@link #update} produced a valid corrected pose this cycle.
     */
    public boolean hasVisionUpdate() {
        return visionUpdateAvailable;
    }

    /**
     * Returns the latest vision pose in inches (X, Y) and radians (heading).
     * Null if no valid update was produced this cycle.
     */
    public Vector getVisionPose() {
        return lastVisionPose;
    }

    /**
     * Returns the number of AprilTags that contributed to the current estimate.
     * Zero when no update is available.
     */
    public int getLastTagCount() {
        return lastTagCount;
    }

    /**
     * Returns the blending alpha [0.0, 1.0] for this estimate, scaled by tag count.
     * Higher tag count → higher trust → larger correction applied by SwerveLocalizer.
     *
     * Single tag  → {@code SwerveConfig.LIMELIGHT_SINGLE_TAG_ALPHA}
     * 2+ tags     → {@code SwerveConfig.LIMELIGHT_MULTI_TAG_ALPHA}
     * No update   → 0.0
     */
    public double getTrustFactor() {
        if (!visionUpdateAvailable) return 0.0;
        return (lastTagCount >= 2)
                ? SwerveConfig.LIMELIGHT_MULTI_TAG_ALPHA
                : SwerveConfig.LIMELIGHT_SINGLE_TAG_ALPHA;
    }
}
