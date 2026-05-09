# Local Testing Guide

This guide covers the local JVM test flow that exists in the repo today. It does not use the simulator and it does not require a robot to be connected.

## What you can test locally

Use local tests for logic that does not need FTC runtime hardware objects:

- math helpers
- geometry
- PID logic
- input shaping
- kinematics
- observer logic
- drivetrain state-machine behavior that is already abstracted behind test IO

Current examples live under `TeamCode/src/test/java`, including:

- `Swerve/Core/MathUtilTest.java`
- `Swerve/Input/MotionSmootherTest.java`
- `Swerve/Logic/Control/SwerveControllerTest.java`
- `Swerve/Hardware/SwerveDrivetrainTest.java`

## What you should not try to unit test directly

These still belong to on-robot or deeper integration testing unless you extract more seams first:

- `HWMap`
- `SwerveLocalizer`
- `MainTeleOp`
- direct IMU, motor, servo, and Pinpoint bring-up

## Run tests from Android Studio

1. Open the `TeamCode/src/test/java` tree.
2. Right-click an actual test class such as `SwerveControllerTest`.
3. Choose **Run**.

You can also right-click the `test` source root to run the whole local suite.

## Run tests from the terminal

Run all local TeamCode tests:

```powershell
.\gradlew :TeamCode:testDebugUnitTest
```

Run one test class:

```powershell
.\gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveControllerTest"
```

Run one test method:

```powershell
.\gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveControllerTest.headingLockWaitsForConfiguredDelayBeforeActivating"
```

Compile-check the robot code without running tests:

```powershell
.\gradlew :TeamCode:compileDebugJavaWithJavac
```

## TeleOp-focused starting points

If your next goal is better `MainTeleOp` confidence without building a full OpMode harness, start with these:

1. `SwerveControllerTest`
   - heading hold
   - snap behavior
   - reset behavior
2. `MotionSmootherTest`
   - acceleration and braking feel
3. `SwerveDrivetrainTest`
   - idle locking behavior
   - drive-mode setup
4. `SwerveModuleTest`
   - wheel speed conversion sanity

## TeleOp bring-up on the real robot

For `MainTeleOp`, the practical field test flow is:

1. Place the robot anywhere with open space.
2. Point it in the direction you want to count as forward.
3. Start `MainTeleOp`.
4. Tap `START` if you want to re-zero heading before driving.

That is enough for teleop-only testing. You do not need to run autonomous first.
> Deprecated: parts of this guide still refer to older helper-layer architecture. Use it only as a partial reference until the full swerve docs are rewritten around the slimmer teleop path.
