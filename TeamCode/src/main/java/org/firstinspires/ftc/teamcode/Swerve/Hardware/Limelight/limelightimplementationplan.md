# Limelight Pose Relocalization — Detailed Implementation Plan

## 0. Background & Decision Framework

### MegaTag 1 vs MegaTag 2 — Which to Use?

**Verdict: Use MegaTag 2 exclusively.**

| Factor | MegaTag 1 (MT1) | MegaTag 2 (MT2) |
|---|---|---|
| **IMU Required** | No | Yes (we have Pinpoint + IMU) |
| **Single-tag accuracy** | Poor (pose ambiguity) | Excellent (ambiguity-free) |
| **Multi-tag accuracy** | Good | Excellent |
| **Distance tolerance** | < 3 m recommended | Any distance, single tag viable |
| **Heading output** | Derived from vision | Locked to IMU (reliable) |
| **FTC loop overhead** | Low | Low (one extra `updateRobotOrientation` call) |
| **Recommended by Limelight** | Legacy | Primary since 2024 |

**Why MT1 is not viable for this robot:**

MT1 solves for the full 6-DOF pose from image data alone, which means it is inherently susceptible to the **pose ambiguity problem**: a single flat tag close to the camera can produce two geometrically valid pose solutions with no way to distinguish between them. In a fast-moving FTC match with a front-facing camera that may only see one or two tags at a time, this causes catastrophic jumps in the estimated pose.

MT2 eliminates this problem entirely by accepting the robot's current yaw from the IMU. Since we already have a high-quality yaw from the **GoBilda Pinpoint** odometry pod (primary) and the **REV IMU** (fallback) running inside `SwerveLocalizer`, we have exactly the input MT2 needs. The heading component of the MT2 output is **not used** — only X and Y are blended into `masterPose`, consistent with how the FRC reference implementation handles the large heading standard deviation (`9999999`).

**Is MT1 even possible?** Yes. The FTC API exposes `result.getBotpose()` for MT1 and `result.getBotpose_MT2()` for MT2. MT1 works without any extra call. However, the accuracy and reliability are significantly worse for a single-camera forward-facing setup, especially with only one tag visible. **It is not worth implementing.**

---

### Relocalization Frequency — Important Principle

**Relocalization runs every single loop cycle in which the Limelight has a valid result.** There is no timer or rate limiter. The camera publishes a new botpose every time it successfully detects and processes a tag frame (up to 100 Hz at our configured poll rate). Our code checks for a fresh result on every robot loop (~50 Hz) and immediately applies the correction if one is available.

The **staleness check** (`result.getStaleness()`) does NOT implement periodic sampling — it rejects frames where the Limelight's internal pipeline has not produced a new computation since the last time we asked. This can happen momentarily when the camera loses sight of a tag mid-frame. If `getStaleness()` is above the threshold it means the data is from a previous vision frame and the robot has already moved — that stale pose correction would introduce error rather than reduce it, so we discard it.

**In plain terms:** when the robot sees a tag → we relocalize. When it doesn't → we continue on Pinpoint dead-reckoning alone. The system is fully reactive, not scheduled.

---

### Custom Python Pipeline / Neural Network — Worth It?

**Verdict: No. Use the built-in AprilTag pipeline (Pipeline 0).**

- **Neural network pipelines** (classifier/detector) are designed for detecting game pieces (rings, pixels, samples). They are completely unrelated to pose estimation, which uses geometric tag detection.
- **Python SnapScripts** are useful for custom color processing or object filtering logic. For pose relocalization, the AprilTag pipeline already does everything needed on-device and returns structured `Pose3D` objects. Writing a Python replacement offers zero benefit and adds complexity.
- **Conclusion:** Keep Pipeline 0 as an AprilTag pipeline with "Full 3D" enabled. No custom pipeline needed.

---

## 1. Hardware & Web UI Setup (One-Time, Before Coding)

### 1.1 Wiring
- Connect the Limelight 3A to the Control Hub via USB.
- It will appear as `Limelight3A` in the hardware configuration.

### 1.2 Limelight Web UI Configuration
Access via `http://limelight.local:5801` on the robot's network.

**Pipeline 0 — AprilTag:**
1. Set **Pipeline Type** → `AprilTag`
2. Enable **"Full 3D"** in the Advanced tab.
3. Upload the current season's `.fmap` field map file (download from Limelight or use the Map Builder tool at `https://tools.limelightvision.io`).

**Robot-Space Camera Pose:**
This is the most critical calibration step. You must tell the Limelight where it sits relative to the center of the robot's floor footprint.

| Parameter | Description | Units |
|---|---|---|
| `LL Forward` | Distance from robot center to camera along robot's forward axis | meters |
| `LL Right` | Distance from robot center to camera along robot's right axis | meters |
| `LL Up` | Height of camera above floor | meters |
| `LL Roll` | Camera roll | degrees |
| `LL Pitch` | Camera pitch (angle up/down from horizontal) | degrees |
| `LL Yaw` | Camera yaw (0° = faces forward with robot) | degrees |

Since the camera faces the same direction as the robot, `LL Yaw = 0`. Measure `LL Forward` and `LL Right` from the robot center to the camera lens carefully — errors here directly translate into a constant offset in every pose estimate.

### 1.3 FTC Hardware Configuration
In the Driver Hub's hardware config file, add:
```
Device: Limelight3A
Name: "limelight"
```

---

## 2. Coordinate Systems

Understanding the coordinate systems prevents sign errors.

**FTC Field Space (what `getBotpose()` returns):**
- Origin: center of field floor
- Returned as: `[X (m), Y (m), Z (m), Roll (deg), Pitch (deg), Yaw (deg)]`

**Our `SwerveLocalizer` Master Pose:**
- `Vector(x_inches, y_inches, heading_radians)`
- Units: **inches** for X/Y, **radians** for heading

**Required Conversions when ingesting a Limelight pose:**
```java
// Limelight returns meters; SwerveLocalizer uses inches
double visionX   = botpose.getPosition().x * 39.3701; // m -> in
double visionY   = botpose.getPosition().y * 39.3701; // m -> in
double visionYaw = Math.toRadians(botpose.getOrientation().getYaw()); // deg -> rad
```

**Robot Space:**
- X+ = Forward, Y+ = Right, Z+ = Up (standard right-hand robot frame)
- The Limelight uses this internally to transform camera-space poses to field-space poses.

---

## 3. New Files — `Swerve/Hardware/Limelight/`

### 3.1 `Limelight.java` — Hardware Wrapper

Wraps the `Limelight3A` hardware object. Responsible only for low-level I/O.

```java
package org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Limelight
 *
 * Thin hardware wrapper around the Limelight3A FTC driver.
 * Handles initialization, polling rate, and IMU orientation seeding.
 */
public class Limelight {

    private final Limelight3A ll;

    public Limelight(HardwareMap hardwareMap) {
        ll = hardwareMap.get(Limelight3A.class, "limelight");
        ll.setPollRateHz(100); // Request data at 100Hz
        ll.pipelineSwitch(0); // Ensure AprilTag pipeline is active
        ll.start();
    }

    /**
     * Must be called every loop before reading results.
     * Seeds the camera with the robot's current yaw for MegaTag2.
     *
     * @param yawDegrees Current robot yaw from the IMU, in degrees.
     */
    public void updateRobotOrientation(double yawDegrees) {
        // Signature: yaw, yawRate, pitch, pitchRate, roll, rollRate
        // We only provide yaw; rates are 0 since we handle that filtering ourselves.
        ll.updateRobotOrientation(yawDegrees);
    }

    /**
     * Returns the latest result from the Limelight. May be null.
     */
    public LLResult getLatestResult() {
        return ll.getLatestResult();
    }

    /**
     * Returns the data staleness in milliseconds. Useful for sanity checks.
     */
    public long getStaleness() {
        LLResult r = ll.getLatestResult();
        return r != null ? r.getStaleness() : Long.MAX_VALUE;
    }

    public void stop() {
        ll.stop();
    }
}
```

### 3.2 `LimelightLocalizer.java` — Vision Pose Processor

Contains the trust/rejection logic and converts Limelight output to a form `SwerveLocalizer` can consume.

```java
package org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

import java.util.List;

/**
 * LimelightLocalizer
 *
 * Converts MegaTag2 botpose estimates from the Limelight into field-space
 * pose corrections for SwerveLocalizer. Applies confidence gating to
 * reject unreliable measurements.
 */
public class LimelightLocalizer extends Limelight {

    // --- State ---
    private Vector lastVisionPose = null;  // null = no valid update this cycle
    private int lastTagCount = 0;
    private double lastAmbiguity = 1.0;
    private boolean visionUpdateAvailable = false;

    public LimelightLocalizer(com.qualcomm.robotcore.hardware.HardwareMap hardwareMap) {
        super(hardwareMap);
    }

    /**
     * Call every loop BEFORE reading the pose estimate.
     * Feeds the current robot yaw into MegaTag2 and processes the result.
     *
     * @param currentYawDegrees Robot heading in degrees (from Pinpoint or IMU).
     * @param currentAngularVelocityDegS Current angular velocity in deg/s.
     *                                   Used to gate updates during fast spins.
     */
    public void update(double currentYawDegrees, double currentAngularVelocityDegS) {
        visionUpdateAvailable = false;
        lastVisionPose = null;

        // Step 1: Seed MegaTag2 with current heading
        updateRobotOrientation(currentYawDegrees);

        // Step 2: Fetch the latest result from the Limelight.
        // getLatestResult() returns the most recent frame the camera has computed.
        // This is called every loop — relocalization fires every time a valid
        // tag result arrives. There is no rate limiter here by design.
        LLResult result = getLatestResult();
        if (result == null || !result.isValid()) return;

        // Step 3: Staleness guard — reject frames that are too old.
        // Staleness > threshold means the camera has NOT produced a new
        // computation since our last call (e.g. tag briefly out of frame).
        // Applying a stale pose would move our estimate backward in time
        // relative to where the robot actually is now. Discard it.
        if (result.getStaleness() > SwerveConfig.LIMELIGHT_MAX_STALENESS_MS) return;

        // Step 4: Get MT2 botpose
        Pose3D botpose = result.getBotpose_MT2();
        if (botpose == null) return;

        // Step 5: Count tags and check ambiguity
        List<LLResultTypes.FiducialResult> fiducials = result.getFiducialResults();
        lastTagCount = fiducials != null ? fiducials.size() : 0;
        if (lastTagCount == 0) return;

        // Step 6: Ambiguity check (only meaningful for single-tag MT1 mode,
        // but we check it here as a belt-and-suspenders sanity filter).
        lastAmbiguity = 1.0;
        if (fiducials != null && fiducials.size() == 1) {
            // getRobotPoseTargetSpace distance is a proxy for confidence
            // For single tag at long distance, we can be more cautious
            double distToTag = fiducials.get(0).getRobotPoseTargetSpace().getPosition().z;
            if (distToTag > SwerveConfig.LIMELIGHT_MAX_TAG_DISTANCE_M) return;
        }

        // Step 7: Gate on angular velocity — fast spins corrupt MT2
        if (Math.abs(currentAngularVelocityDegS) > SwerveConfig.LIMELIGHT_MAX_ANGULAR_VEL_DEG_S) return;

        // Step 8: Convert to SwerveLocalizer units (meters -> inches, degrees -> radians)
        double visionX   = botpose.getPosition().x * 39.3701;
        double visionY   = botpose.getPosition().y * 39.3701;
        // Note: we DO NOT use visionYaw for fusion — the IMU heading is trusted.
        // SwerveLocalizer.applyVisionUpdate() ignores the heading component.
        double visionYaw = Math.toRadians(botpose.getOrientation().getYaw());

        lastVisionPose = new Vector(visionX, visionY, visionYaw);
        visionUpdateAvailable = true;
    }

    /** Returns true if a valid vision pose was computed this cycle. */
    public boolean hasVisionUpdate() { return visionUpdateAvailable; }

    /** Returns the latest vision pose in inches and radians. Null if no update. */
    public Vector getVisionPose() { return lastVisionPose; }

    /** Returns the number of AprilTags contributing to this estimate. */
    public int getLastTagCount() { return lastTagCount; }

    /**
     * Computes a trust factor [0.0, 1.0] for the current estimate.
     * Higher tag count = more trust. Used as the lerp alpha in SwerveLocalizer.
     */
    public double getTrustFactor() {
        if (!visionUpdateAvailable) return 0.0;
        // Scale trust with tag count. Single tag = LIMELIGHT_SINGLE_TAG_ALPHA.
        // Multiple tags converge toward LIMELIGHT_MULTI_TAG_ALPHA.
        if (lastTagCount >= 2) return SwerveConfig.LIMELIGHT_MULTI_TAG_ALPHA;
        return SwerveConfig.LIMELIGHT_SINGLE_TAG_ALPHA;
    }
}
```

---

## 4. Modified Files

### 4.1 `SwerveConfig.java` — New Limelight Constants

Add a new section at the bottom of `SwerveConfig.java`:

```java
// ─────────────────────────────────────────────────────────────────────────
// 9. Limelight Vision Relocalization
// ─────────────────────────────────────────────────────────────────────────

/**
 * Master enable switch for Limelight pose relocalization.
 *
 * When FALSE: LimelightLocalizer is never instantiated. No hardware map
 * lookup is attempted. SwerveLocalizer operates purely on Pinpoint + IMU
 * dead-reckoning. The camera does not need to be physically present.
 *
 * When TRUE:  LimelightLocalizer is created during OpMode init and
 * applyVisionUpdate() is called every loop where a valid result arrives.
 *
 * This is a @Config field so it can be toggled on FTC Dashboard without
 * redeploying code.
 */
public static boolean LIMELIGHT_ENABLED = false;

/**
 * Camera position offsets from robot center (set via Limelight Web UI,
 * documented here for reference). These are NOT used in code — they are
 * programmed directly into the Limelight hardware.
 *   LL_CAMERA_FORWARD_M  = distance forward from robot center in meters
 *   LL_CAMERA_RIGHT_M    = distance right from robot center in meters
 *   LL_CAMERA_UP_M       = height above floor in meters
 *   LL_CAMERA_PITCH_DEG  = pitch angle (positive = tilted up)
 */
public static double LL_CAMERA_FORWARD_M  = 0.0;  // TODO: Measure
public static double LL_CAMERA_RIGHT_M    = 0.0;  // TODO: Measure
public static double LL_CAMERA_UP_M       = 0.0;  // TODO: Measure
public static double LL_CAMERA_PITCH_DEG  = 0.0;  // TODO: Measure (0 = horizontal)

/** Reject data older than this many milliseconds. */
public static long LIMELIGHT_MAX_STALENESS_MS = 100;

/**
 * Reject updates if the robot is spinning faster than this (degrees/sec).
 * MT2 is susceptible to IMU lag during fast rotation.
 * 360 deg/s is a conservative threshold.
 */
public static double LIMELIGHT_MAX_ANGULAR_VEL_DEG_S = 360.0;

/**
 * Maximum allowed distance (meters) to a single AprilTag.
 * Beyond this distance, single-tag estimates become less accurate.
 */
public static double LIMELIGHT_MAX_TAG_DISTANCE_M = 4.0;

/**
 * Lerp alpha when only 1 tag is visible. Lower = more conservative blending.
 * A value of 0.05 means 5% vision, 95% odometry per cycle.
 */
public static double LIMELIGHT_SINGLE_TAG_ALPHA = 0.05;

/**
 * Lerp alpha when 2+ tags are visible. Can be more aggressive.
 * A value of 0.15 means 15% vision per cycle.
 */
public static double LIMELIGHT_MULTI_TAG_ALPHA = 0.15;

/**
 * Hard-reset threshold in inches. If vision disagrees with odometry by more
 * than this amount, perform a hard pose reset instead of a gentle lerp.
 * This handles the case where Pinpoint has drifted significantly.
 */
public static double LIMELIGHT_HARD_RESET_THRESHOLD_IN = 18.0;
```

### 4.2 `HWMap.java` — Add Limelight Hardware

The `Limelight3A` hardware initialization is delegated to `LimelightLocalizer` itself (it calls `super(hardwareMap)`), so `HWMap` does **not** need to be modified. The `LimelightLocalizer` is instantiated directly in the OpMode, similar to how `SwerveLocalizer` is.

> **Design decision**: `LimelightLocalizer` is not part of `HWMap` because vision is an optional subsystem. If the camera is physically absent, the OpMode can simply skip instantiating it without any `HWMap` changes or null-checks in unrelated code.

### 4.3 `SwerveLocalizer.java` — Add Vision Fusion Method

Add `applyVisionUpdate()` to the existing localizer:

```java
/**
 * Applies a vision-based pose correction from the Limelight.
 *
 * Two modes:
 * 1. Hard reset: if the vision estimate differs from masterPose by more
 *    than LIMELIGHT_HARD_RESET_THRESHOLD_IN, snap directly to the vision pose
 *    (X and Y only — heading is always preserved from IMU/Pinpoint).
 * 2. Soft blend: lerp masterPose toward visionPose by trustFactor.
 *
 * @param visionPose   Field-space pose from LimelightLocalizer (inches, radians).
 * @param trustFactor  Weight [0.0, 1.0] for the blend. 1.0 = full vision, 0.0 = ignore.
 */
public void applyVisionUpdate(Vector visionPose, double trustFactor) {
    if (visionPose == null || trustFactor <= 0.0) return;

    double errorX = visionPose.x() - masterPose.x();
    double errorY = visionPose.y() - masterPose.y();
    double errorDist = Math.sqrt(errorX * errorX + errorY * errorY);

    double newX, newY;
    if (errorDist > SwerveConfig.LIMELIGHT_HARD_RESET_THRESHOLD_IN) {
        // Hard reset: snap to vision (X/Y only)
        newX = visionPose.x();
        newY = visionPose.y();
    } else {
        // Soft blend: lerp toward vision pose
        newX = masterPose.x() + trustFactor * errorX;
        newY = masterPose.y() + trustFactor * errorY;
    }

    // Heading is always sourced from Pinpoint/IMU — never from vision.
    masterPose = new Vector(newX, newY, masterPose.omega());
}
```

### 4.4 `MainTeleOp.java` — Integration

```java
// --- Declarations ---
// Declared as null. Only assigned if LIMELIGHT_ENABLED is true.
private LimelightLocalizer limelightLocalizer;

// --- In runOpMode(), after HWMap init ---
// Limelight is an optional subsystem. Only instantiate if the flag is set.
// This prevents a hardware map crash if the camera is physically absent.
if (SwerveConfig.LIMELIGHT_ENABLED) {
    limelightLocalizer = new LimelightLocalizer(hardwareMap);
}

// --- Main loop, after localizer.update() ---

// Limelight relocalization — fully gated behind LIMELIGHT_ENABLED.
// No Limelight code runs at all if the flag is false.
if (SwerveConfig.LIMELIGHT_ENABLED && limelightLocalizer != null) {

    // 1. Get angular velocity in deg/s for the MT2 spin gate.
    //    omega() from the velocity observer is in rad/s.
    double angularVelocityDegS = Math.toDegrees(swerveDrivetrain.getActualVelocity().omega());

    // 2. Feed IMU yaw to Limelight and process the latest frame.
    double headingDeg = Math.toDegrees(localizer.getHeading());
    limelightLocalizer.update(headingDeg, angularVelocityDegS);

    // 3. Apply vision correction to localizer if a valid result arrived.
    if (limelightLocalizer.hasVisionUpdate()) {
        localizer.applyVisionUpdate(
            limelightLocalizer.getVisionPose(),
            limelightLocalizer.getTrustFactor()
        );
    }

    // 4. Telemetry (only emitted when Limelight is active).
    logger.log("LL TagCount",  limelightLocalizer.getLastTagCount(),              Logger.LogLevels.PRODUCTION);
    logger.log("LL HasUpdate", limelightLocalizer.hasVisionUpdate() ? 1.0 : 0.0, Logger.LogLevels.PRODUCTION);
    if (limelightLocalizer.getVisionPose() != null) {
        logger.log("LL Vision X (in)", limelightLocalizer.getVisionPose().x(), Logger.LogLevels.PRODUCTION);
        logger.log("LL Vision Y (in)", limelightLocalizer.getVisionPose().y(), Logger.LogLevels.PRODUCTION);
    }
}
```

---

## 5. Key FTC API Reference

```java
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.LLResultTypes.FiducialResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
```

| Method | Description |
|---|---|
| `limelight.start()` | Begin processing pipeline |
| `limelight.stop()` | Stop processing |
| `limelight.setPollRateHz(100)` | Set data poll rate |
| `limelight.pipelineSwitch(0)` | Activate pipeline 0 (AprilTag) |
| `limelight.updateRobotOrientation(yawDeg)` | Seed MT2 with IMU yaw **(must call every loop)** |
| `limelight.getLatestResult()` | Returns `LLResult` or null |
| `result.isValid()` | True if target detected |
| `result.getStaleness()` | Age of data in milliseconds |
| `result.getBotpose()` | MT1 field-space pose (avoid for production) |
| `result.getBotpose_MT2()` | **MT2 field-space pose** (use this) |
| `result.getFiducialResults()` | List of individual tag detections |
| `fiducial.getFiducialId()` | Tag ID |
| `fiducial.getRobotPoseTargetSpace()` | Robot pose in tag's coordinate frame |

---

## 6. Field Map Notes

- Limelight ships with the current FTC season's AprilTag map pre-installed.
- The map uses the `.fmap` JSON format with 4×4 homogeneous transform matrices for each tag's field pose.
- The FTC coordinate system has **(0,0,0)** at the center of the field floor.
- For non-diamond field configurations: **0° yaw = blue alliance on left, red on right**.
- Custom maps can be created at `https://tools.limelightvision.io` and uploaded via the web UI.
- If your current season's map is not pre-installed, you can use the REST API to upload it:
  ```java
  // Programmatic upload (rarely needed; prefer web UI)
  boolean ok = limelight.uploadFieldmap(fieldMap, null); // null = default slot
  ```

---

## 7. Tuning Guide

### Step 1: Camera Pose Calibration
- Measure physical camera position carefully (mm accuracy) and enter into the Limelight web UI.
- To verify: place the robot at a known field position and check that `LimelightLocalizer.getVisionPose()` matches within a few inches.

### Step 2: Staleness & Angular Velocity Thresholds
- Start conservative: `LIMELIGHT_MAX_STALENESS_MS = 100`, `LIMELIGHT_MAX_ANGULAR_VEL_DEG_S = 360`.
- If pose "jumps" during spins, lower the angular velocity threshold.

### Step 3: Lerp Alphas
- Start at `LIMELIGHT_SINGLE_TAG_ALPHA = 0.05`, `LIMELIGHT_MULTI_TAG_ALPHA = 0.15`.
- Monitor `masterPose` vs. `visionPose` telemetry. If the two diverge under movement, increase the alpha slightly.
- If the pose jitters, decrease the alpha.

### Step 4: Hard Reset Threshold
- Start at `LIMELIGHT_HARD_RESET_THRESHOLD_IN = 18.0` (1.5 feet).
- This fires when Pinpoint has drifted badly. If spurious hard resets happen, increase the value.

---

## 8. Verification Plan

### Automated
- Unit test: mock `LimelightLocalizer.getVisionPose()` and verify `SwerveLocalizer.applyVisionUpdate()` correctly lerps `masterPose`.
- Unit test: verify hard-reset path fires when error exceeds threshold.
- Unit test: verify `LIMELIGHT_ENABLED = false` results in zero Limelight-related calls (no crash without hardware).

### Manual
1. **Sanity Check**: Start robot at (0,0). Confirm `LimelightLocalizer.getVisionPose()` returns near (0,0) when in front of a visible tag.
2. **Drift Correction**: Drive robot 6 feet in a straight line, causing Pinpoint drift. Return to a known position with a tag visible. Confirm `masterPose` converges toward the true position.
3. **Spin Rejection**: Spin the robot rapidly. Confirm `hasVisionUpdate()` returns `false` during the spin (angular velocity gate firing).
4. **FTC Dashboard**: Stream `Vision X`, `Vision Y`, `LL TagCount`, and `Pinpoint X`, `Pinpoint Y` to compare convergence in real-time.
5. **Isolation Test**: Set `LIMELIGHT_ENABLED = false` and confirm the robot runs normally with no Limelight hardware present.

---

## 9. Subsystem Isolation Design

### Philosophy

The Limelight is treated as a **completely optional, zero-cost-when-disabled** subsystem. The robot must be able to compete at full capability without the camera physically present. This is enforced at the source code level — not just with a runtime null-check.

### The Isolation Contract

| Scope | Rule |
|---|---|
| `SwerveConfig` | `LIMELIGHT_ENABLED = false` is the default. Must be explicitly opted in. |
| `HWMap` | Does **not** reference `Limelight3A`. No hardware lookup, no exception if camera is absent. |
| `SwerveLocalizer` | Has no knowledge of Limelight. `applyVisionUpdate()` is a generic method callable from any source. |
| `Limelight.java` | Encapsulates all hardware I/O. No other file imports `Limelight3A` directly. |
| `LimelightLocalizer.java` | Encapsulates all vision processing logic. No swerve-specific imports. |
| OpModes | The only place that wires Limelight into the robot. All Limelight code is inside a single `if (SwerveConfig.LIMELIGHT_ENABLED)` block. |
| Telemetry | Limelight telemetry only emitted when the subsystem is active. No dead log lines when disabled. |

### How to Enable / Disable

**Disabling (default):**
```java
// SwerveConfig.java
public static boolean LIMELIGHT_ENABLED = false;
```
With this set, no `LimelightLocalizer` is ever constructed, the hardware map is never queried for `Limelight3A`, and `SwerveLocalizer` operates on Pinpoint + IMU alone.

**Enabling:**
```java
// SwerveConfig.java
public static boolean LIMELIGHT_ENABLED = true;
```
The OpMode's `if (SwerveConfig.LIMELIGHT_ENABLED)` block runs, `LimelightLocalizer` is constructed (which calls `hardwareMap.get(Limelight3A.class, "limelight")`), and vision corrections are applied every loop.

> [!NOTE]
> `LIMELIGHT_ENABLED` is a `@Config` field, so it can also be toggled live from FTC Dashboard without redeploying. This is useful during practice matches to compare odometry-only vs. vision-fused localization.

### Dependency Graph

```
MainTeleOp
  ├── SwerveLocalizer      ← no Limelight imports
  ├── SwerveDrivetrain     ← no Limelight imports
  └── [if LIMELIGHT_ENABLED]
        └── LimelightLocalizer
              └── Limelight
                    └── Limelight3A (FTC SDK hardware)
```

The Limelight branch is a leaf — it depends on nothing in the Swerve package. Deleting the entire `Limelight/` folder and setting `LIMELIGHT_ENABLED = false` leaves the rest of the codebase completely unaffected.
