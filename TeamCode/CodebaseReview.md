# TeamCode Non-Simulator Code Review

Reviewed on 2026-04-23 with simulator code intentionally excluded.

## Scope

- Reviewed the non-simulator `TeamCode/src/main/java` swerve stack, plus the current unit-test setup and testing docs.
- Focused most heavily on `MainTeleOp`, localization, drivetrain, controller, config, and supporting docs.
- Did not treat existing dirty worktree changes as mine.

## What I verified

- `.\gradlew :TeamCode:testDebugUnitTest` - passes
- `.\gradlew :TeamCode:compileDebugJavaWithJavac` - passes

## Highest-Priority Findings

### 1. Wheel-size constant and comment disagree, and one of them is almost certainly wrong

- File: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/SwerveConfig.java:135-136`
- Related use: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Hardware/SwerveModule.java:210-214`

`WHEEL_RADIUS_METERS` is set to `0.049`, but the comment says `49mm diameter swerve wheel`.

Why this matters:

- If the wheel is actually 49 mm in diameter, the radius should be `0.0245`, not `0.049`.
- That would make observer velocity, fallback dead-reckoning, telemetry, and stall logic all read about 2x too large.
- If `0.049` is correct, then the comment is wrong and will keep poisoning future tuning.

Recommended fix:

1. Physically confirm the real wheel diameter.
2. Make the code and comment agree.
3. Add a small unit test around drive tick-to-meters conversion so this cannot silently drift again.

Suggested direction:

```java
public static double WHEEL_RADIUS_METERS = 0.0245; // 49 mm diameter wheel
```

Only use that value if the hardware really is a 49 mm diameter wheel.

### 2. Driver heading reset only resets the localizer, not the controller state

- File: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/OpModes/MainTeleOp.java:64-67`

Pressing `START` calls `localizer.resetHeading()`, but `SwerveController` keeps its previous heading target and mode flags.

Why this matters:

- If heading-hold or snap state is active, the robot can immediately try to rotate back toward the stale target after the driver re-zeros heading.
- That creates a nasty "I just reset heading and it still wants to turn" failure mode during teleop.

Recommended fix:

When `START` is edge-detected, reset the controller and clear transient drive shaping:

```java
if (startPressed && !previousStartPressed) {
    localizer.resetHeading();
    swerveController.resetHeading(0.0);
    swerveDrivetrain.resetSmoother();
}
```

### 3. Stored auto pose is consumed by teleop but never cleared

- Files:
  - `src/main/java/org/firstinspires/ftc/teamcode/Swerve/OpModes/MainTeleOp.java:49-52`
  - `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/PoseStorage.java:27-32`

`MainTeleOp` loads `PoseStorage.getCurrentPose()` and seeds the localizer from it, but it does not call `PoseStorage.clear()` afterward.

Why this matters:

- Once autonomous writes a pose, later teleop runs in the same app process can keep inheriting that stale pose forever.
- That becomes very confusing if you stop using auto for a while and teleop still comes up with an old field pose.

Recommended fix:

```java
Pose storedPose = PoseStorage.getCurrentPose();
if (storedPose != null) {
    localizer.setPose(storedPose.toVector());
    PoseStorage.clear();
}
```

## Medium-Priority Findings

### 4. Dashboard telemetry is wrapped twice when dashboard mode is enabled

- Files:
  - `src/main/java/org/firstinspires/ftc/teamcode/Swerve/OpModes/MainTeleOp.java:37-41`
  - `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/Logger.java:21-26`

`MainTeleOp` wraps `telemetry` in `MultipleTelemetry`, then `Logger` does it again when `DASHBOARD_ENABLED` is true.

Why this matters:

- Duplicate telemetry plumbing can create duplicate packets or at least unnecessary overhead and confusion while tuning.

Recommended fix:

- Pick one place to own dashboard wrapping.
- The cleaner choice is usually to leave wrapping in `MainTeleOp` and let `Logger` trust the telemetry object it receives.

### 5. Logger toggle code has a misleading parameter name and duplicate state writes

- File: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/Logger.java:54-67`
- Call site: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/OpModes/MainTeleOp.java:63`

The method parameter is named `D_Pad_Right`, but the actual caller passes `LEFT_BUMPER`. It also writes `"CURRENT LOGGER STATE"` twice on some transitions.

Why this matters:

- It makes the control binding harder to trust when you come back later.
- The duplicate telemetry update is harmless but noisy.

Recommended fix:

- Rename the method parameter to something honest like `toggleButtonPressed`.
- Add braces around the `else if` block and emit the state line once.

### 6. `LocalTestingGuide.md` is stale and points to a test entrypoint that does not exist

- File: `src/main/java/org/firstinspires/ftc/teamcode/Swerve/docs/LocalTestingGuide.md:8-20`

The guide says to navigate to `... > Swerve > Tests`, right-click `MasterTestSuite.java`, and run `org.firstinspires.ftc.teamcode.Swerve.Tests.MasterTestSuite`. That class/package is not present in the current tree.

Why this matters:

- Anyone trying to onboard from the doc will hit a dead end immediately.

Recommended fix:

- Replace the guide with the actual flow already supported by the repo:
  - right-click a real JUnit test class under `TeamCode/src/test/java`
  - or run `.\gradlew :TeamCode:testDebugUnitTest`
- Point readers to `SwerveControllerTest`, `MotionSmootherTest`, and `SwerveDrivetrainTest` as current entry points.

## Low-Priority Cleanup

### 7. Several files have encoding damage in comments and docs

Examples:

- `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/SwerveConfig.java`
- `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/MathUtil.java`
- `src/main/java/org/firstinspires/ftc/teamcode/Swerve/Logic/Kinematics/SwerveModuleState.java`
- `src/main/java/org/firstinspires/ftc/teamcode/Swerve/docs/LocalTestingGuide.md`

Why this matters:

- It does not break runtime behavior, but it makes tuning docs and comments look corrupted and harder to trust.

Recommended fix:

- Re-save these files as UTF-8 or normalize the separators/comments back to ASCII.

## TeleOp Testing Setup For `MainTeleOp`

This section is specifically for teleop-only bring-up. No autonomous run is required.

### Where to place the robot before starting

Based on the current code path:

- `SwerveLocalizer` calls `resetHeading()` in its constructor (`SwerveLocalizer.java:26-35`).
- `MainTeleOp` also lets the driver re-zero heading with `START` (`MainTeleOp.java:64-67`).

That means teleop field-centric control does **not** require a special field tile or an autonomous handoff. It requires a known heading reference.

Use this procedure:

1. Put the robot anywhere with enough room to drive safely.
2. Point the robot in the direction you want to count as heading `0`.
3. Initialize and start `MainTeleOp`.
4. If the robot got bumped or you want to redefine forward, hold it still and tap `START` to re-zero heading.

Practical recommendation:

- For easiest driver testing, point the robot straight away from the driver station before start.
- Then pushing the stick "forward" will feel intuitive for field-centric checks.

### What to test first

Run these checks in order:

1. **Module zero check**
   - Robot on blocks.
   - Small left-stick inputs should make wheel angles settle cleanly without violent hunting.

2. **Forward/backward translation**
   - On floor, drive forward and backward at low speed.
   - Confirm all four modules align consistently and the robot does not crab sideways.

3. **Strafe**
   - Test left and right translation.
   - Watch for one module lagging or flipping late.

4. **Rotation**
   - Apply small right-stick rotation.
   - Confirm smooth turning without oscillation after stick release.

5. **Heading reset**
   - Rotate the robot to a random heading.
   - Tap `START`.
   - Confirm the new facing direction now behaves as field-forward.

6. **Observer sanity**
   - Compare felt top speed to reported telemetry.
   - If the robot feels normal but telemetry/observer values look too large, re-check the wheel radius constant first.

### Where to start if you want to improve test coverage for `MainTeleOp`

`MainTeleOp` itself is still mostly integration code, so the right first step is not a giant OpMode unit test. Start here instead:

1. Keep logic tests in `TeamCode/src/test/java`.
2. Add focused unit tests around the pieces teleop depends on:
   - `SwerveController`
   - `MotionSmoother`
   - `SwerveDrivetrain`
3. If you want direct teleop tests later, extract a small helper that:
   - accepts raw stick inputs
   - accepts current heading
   - returns commanded chassis speeds and any reset actions

That helper can be tested locally without FTC runtime objects.

### Commands to use today

Run all current local tests:

```powershell
.\gradlew :TeamCode:testDebugUnitTest
```

Run a teleop-adjacent logic class:

```powershell
.\gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveControllerTest"
```

Compile-check the robot code:

```powershell
.\gradlew :TeamCode:compileDebugJavaWithJavac
```

## Suggested next fixes in order

1. Resolve the wheel radius mismatch.
2. Reset `SwerveController` and smoother on teleop heading re-zero.
3. Clear `PoseStorage` after teleop consumes it.
4. Clean up dashboard telemetry ownership.
5. Rewrite `LocalTestingGuide.md` to match the real test suite.
