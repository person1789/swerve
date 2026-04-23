# Unit Testing Guide For TeamCode Swerve

This project supports local JVM unit tests for the parts of the swerve stack that are deterministic and do not need a robot controller, REV hub, odometry board, or Android runtime. The goal of this guide is to explain both the mechanics of running tests in this repo and the reasoning behind the current test layout, so future tests are added in the right place with the right dependencies.

## What Kind Of Tests This Repo Uses

There are three practical testing levels in an FTC Android project:

1. Local unit tests
   These run on your computer's JVM under `TeamCode/src/test/java`.
   They are fast and are the best place to test math, state machines, filters, PID logic, kinematics, optimization, and other pure logic.

2. Instrumented Android tests
   These run in `TeamCode/src/androidTest/java`.
   They execute against Android APIs and are only needed when a test genuinely depends on the Android framework.

3. Robot or hardware integration tests
   These are usually OpModes, hardware smoke tests, or on-robot validation routines.
   They are required for classes that talk directly to motors, servos, IMUs, telemetry, `HardwareMap`, or vendor hardware drivers.

For this swerve codebase, the most valuable local unit test targets are:

- `MathUtil`
- `Vector`
- `Pose`
- `LowPassFilter`
- `PIDController`
- `MotionSmoother`
- `SwerveController`
- `SwerveModuleState`
- `SwerveKinematics`
- `SwerveAuditor`
- `SwerveVelocityObserver`

The following are not good local JVM unit test targets without extra abstraction or heavy mocking:

- `HWMap`
- `SwerveModule`
- `SwerveDrivetrain`
- `SwerveLocalizer`
- `Logger`
- OpModes such as `MainTeleOp`, `Auto`, and `SwerveModulePIDTune`

Those classes touch hardware SDK objects, vendor drivers, FTC telemetry, or runtime-owned lifecycle behavior. They should be covered with integration testing, on-robot validation, or additional seam extraction before unit testing.

## The Gradle Files That Matter

### `TeamCode/build.gradle`

This is the module-level Gradle file for the `TeamCode` Android app module. It is the main file that controls local unit test behavior for this team's code.

The important sections are:

```groovy
android {
    testOptions {
        unitTests.returnDefaultValues = true
        unitTests.all {
            useJUnitPlatform()
        }
    }

    sourceSets {
        main.java.srcDirs = ['src/main/java']
        test.java.srcDirs = ['src/test/java']
        androidTest.java.srcDirs = ['src/androidTest/java']
    }
}
```

What each part does:

- `unitTests.returnDefaultValues = true`
  This lets some Android-dependent APIs return default placeholder values during local unit tests instead of immediately crashing when the Android runtime is absent. It is useful, but it should not be treated as permission to unit test hardware-heavy classes blindly.

- `useJUnitPlatform()`
  This switches the Gradle test runner to the JUnit 5 platform. Without this line, JUnit 5 tests in `src/test/java` would not run correctly.

- `sourceSets`
  This tells Gradle where the production, local test, and Android instrumented test source trees live.

The test dependency block in the same file is also important:

```groovy
ext.junitVersion = "5.10.3"
dependencies {
    testImplementation "org.junit.jupiter:junit-jupiter:${junitVersion}"
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
    testImplementation "org.mockito:mockito-core:5.13.0"
}
```

What each dependency is for:

- `junit-jupiter`
  The top-level JUnit 5 bundle used to write and run tests.

- `junit-jupiter-api`
  The annotations and assertion APIs, such as `@Test`, `assertEquals`, and `assertThrows`.

- `junit-jupiter-engine`
  The runtime engine that actually discovers and executes JUnit 5 tests.

- `mockito-core`
  The mocking library available if a future unit test needs test doubles. The current swerve unit suite intentionally avoids overusing mocks and prefers pure logic tests.

### `build.dependencies.gradle`

This file provides shared dependencies for the Android FTC project. It is applied into `TeamCode/build.gradle`.

Its main purpose is not unit testing; it primarily pulls in FTC SDK, AndroidX, dashboard, FTCLib, and Pedro Pathing libraries. It also contains a legacy `testImplementation 'junit:junit:4.13.2'`.

That JUnit 4 dependency does not break JUnit 5, but the active local unit test setup for `TeamCode` is JUnit 5 because `TeamCode/build.gradle` calls `useJUnitPlatform()`.

Practical rule:

- If you are adding or changing how TeamCode unit tests run, edit `TeamCode/build.gradle`.
- If you are adding SDK or shared library dependencies used by production code, `build.dependencies.gradle` may also be involved.

### `build.common.gradle`

This file defines the common Android application setup used by the FTC modules. It is intentionally shared and should generally not be edited just for test authoring.

Why it matters for tests:

- It applies the Android application plugin.
- It sets Java source and target compatibility to Java 8.
- It defines the Android build structure that Gradle uses before the TeamCode-specific overrides are applied.

In other words, it provides the Android app foundation, while `TeamCode/build.gradle` layers the TeamCode-specific unit test configuration on top.

## Where To Put Tests

Use this layout:

```text
TeamCode/
  src/
    main/java/... production code
    test/java/... local JVM unit tests
    androidTest/java/... Android instrumented tests
```

Recommended convention:

- Mirror the production package structure.
- Name each file after the class or related behavior being tested.
- Keep one logical area per test class.

Examples in this repo:

- `src/test/java/.../Core/MathUtilTest.java`
- `src/test/java/.../Input/MotionSmootherTest.java`
- `src/test/java/.../Logic/Kinematics/SwerveKinematicsAndAuditorTest.java`

## How To Run Tests

### From The Terminal

Run all TeamCode local unit tests:

```powershell
.\gradlew :TeamCode:testDebugUnitTest
```

Run every module test task in the repo:

```powershell
.\gradlew test
```

Run one specific test class:

```powershell
.\gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Core.MathUtilTest"
```

Run one specific test method:

```powershell
.\gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Core.MathUtilTest.normalizeAngleWrapsAcrossPiBoundary"
```

### From Android Studio

You can run tests in several ways:

1. Right-click a single test class in `src/test/java` and choose Run.
2. Right-click the `test` source root to run the full local unit suite.
3. Use the gutter icon next to an individual `@Test` method.

Android Studio will still use the Gradle/JUnit 5 setup from `TeamCode/build.gradle`.

## How To Write A Good Local Unit Test In This Repo

### Step 1: Decide Whether The Target Is Truly Unit-Testable

Ask this question first:

Does the class depend only on plain Java logic, or does it talk directly to FTC hardware/runtime objects?

Good candidates:

- Pure math helpers
- PID, filters, and smoothing
- State machines
- Kinematics and optimization
- Data objects and conversions

Bad candidates unless refactored first:

- `HardwareMap` wrappers
- Motor or servo control code
- IMU, Pinpoint, telemetry, or OpMode lifecycle code

### Step 2: Put The Test In The Matching Package

If production code lives at:

```text
src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/PIDController.java
```

then the local unit test should usually live at:

```text
src/test/java/org/firstinspires/ftc/teamcode/Swerve/Core/PIDControllerTest.java
```

This keeps the suite easy to navigate and prevents a random pile of unrelated tests.

### Step 3: Prefer Deterministic Inputs

Use exact or tightly controlled inputs:

- fixed `dt`
- explicit vectors
- explicit headings
- explicit config values when needed

Avoid randomness unless you are writing a dedicated repeatable property-style test and seeding it deterministically.

### Step 4: Keep Assertions Behavior-Focused

A strong test says what behavior matters:

- angle wrapping chooses the shortest path
- smoothing caps acceleration
- heading lock waits before engaging
- forward kinematics reconstructs translation

A weak test only checks implementation trivia:

- private variable values
- exact intermediate sequencing that does not affect public behavior

### Step 5: Restore Global Config After Mutation

Many swerve classes read values from `SwerveConfig`, and those fields are static and mutable.

If a test changes a config value, it must restore it afterward, usually in `@AfterEach`.

Example pattern:

```java
private final double originalGain = SwerveConfig.OBSERVER_LPF_GAIN;

@AfterEach
void restoreConfig() {
    SwerveConfig.OBSERVER_LPF_GAIN = originalGain;
}
```

Without restoration, one test can silently poison later tests.

## What The Current Unit Suite Covers

The current local suite is designed to validate the core logic path of the swerve system:

### Core math and geometry

- angle normalization
- wrapped angle error
- clamping and deadband
- vector arithmetic
- defensive copying
- 2D rotation
- pose conversion and wrapped pose addition
- low-pass filtering behavior

### Control and smoothing

- proportional/integral PID behavior
- derivative-on-measurement protection against kick
- wrapped-error PID path
- smoother acceleration limiting
- smoother braking snap behavior
- smoother reset behavior
- heading lock delay
- snap control direction
- manual turn passthrough

### Kinematics and optimization

- module-state construction from Cartesian vectors
- pure-translation inverse kinematics
- forward/inverse compatibility
- alias API coverage
- module flipping when angle error exceeds the threshold
- speed desaturation
- cosine scaling
- NaN sanitization

### Observer behavior

- low-pass blending of chassis estimates
- observer reset

## Why Some Swerve Classes Are Not In The Local Unit Suite

### `SwerveModule`

This class reads encoder voltage, talks to `DcMotorEx`, controls a servo, and reads current draw. Those are hardware interactions, not pure unit logic. It can be tested later by introducing small interfaces around the hardware layer, but without that seam the test value is low and the mocking cost is high.

### `SwerveDrivetrain`

This class coordinates multiple hardware modules and uses an FTC timer object. The logic inside it is important, but much of its public value depends on collaborators that are hardware-owned. The best next step, if deeper unit coverage is wanted, is to extract a smaller pure pipeline coordinator that accepts interfaces instead of raw FTC devices.

### `SwerveLocalizer`

It depends on Pinpoint and IMU devices. That behavior is best covered with integration tests or with a later adapter seam that turns sensor reads into plain Java inputs.

### OpModes

OpModes are lifecycle entrypoints, not unit-level logic containers. Their behavior is best validated through:

- a small amount of local logic testing for extracted helpers
- compile checks
- targeted field validation
- autonomous and teleop smoke runs on hardware

## Recommended Test Authoring Style For This Repo

Use this pattern:

1. Arrange
   Build the object and set any config needed for the scenario.

2. Act
   Call one public method with explicit inputs.

3. Assert
   Check the externally visible result with a tolerance if doubles are involved.

General rules:

- Use JUnit 5.
- Use `assertEquals(..., tolerance)` for doubles.
- Use `assertThrows` for invalid usage.
- Prefer descriptive method names over long comments.
- Keep one behavior per test.
- Keep tests independent and order-agnostic.

## Example Skeleton

```java
class ExampleTest {

    @Test
    void behaviorNameDescribesWhatMustBeTrue() {
        Example subject = new Example();

        double result = subject.calculate(1.0, 0.02);

        assertEquals(0.5, result, 1e-9);
    }
}
```

## Common Mistakes To Avoid

- Writing hardware tests in `src/test/java` and expecting FTC devices to exist.
- Forgetting `useJUnitPlatform()` when adding JUnit 5 tests to a new module.
- Mutating `SwerveConfig` in one test and not restoring it.
- Comparing doubles with exact equality when a tolerance is appropriate.
- Testing private implementation details instead of public behavior.
- Putting project-wide test config in `build.common.gradle` when it really belongs in `TeamCode/build.gradle`.

## When To Edit Gradle For Future Test Work

Edit `TeamCode/build.gradle` when:

- you add JUnit 5 extensions or assertion libraries
- you need Mockito integration helpers
- you want to change TeamCode unit test source sets
- you want to customize TeamCode test execution

Edit `build.dependencies.gradle` when:

- production code needs a new shared dependency
- multiple modules need the same library

Avoid editing `build.common.gradle` for ordinary TeamCode unit test work unless the Android application baseline itself has to change.

## Final Recommendation

Treat the local unit suite as the first gate for every logic change in the swerve stack. If the change affects math, control, filtering, state transitions, or kinematic transformations, add or update a unit test before field-testing the robot. Use hardware and OpMode testing only for the parts that truly require the robot runtime.
