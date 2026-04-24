@depreciatted
# Swerve Simulation Guide

This repo includes a simple browser-based simulator that drives mocked module hardware through the current Java drivetrain pipeline. The sim is intentionally thin: it reads a browser gamepad, sends those inputs to a local Java server, and renders the outputs from mocked module hardware as four blocks on a chassis.

## What It Reuses

The simulator reuses the real Java logic instead of duplicating it in a separate frontend codebase.

Shared pipeline pieces:

- `SwerveController`
- `MotionSmoother`
- `SwerveKinematics`
- `SwerveAuditor`
- `SwerveDrivetrain`
- `SwerveModule`

The only mocked layer is the hardware implementation behind each module via `SwerveModuleIO`.

## How To Run It

Start the local simulator server:

```powershell
.\gradlew :TeamCode:runSwerveSimulator
```

Then open:

```text
http://localhost:8080
```

Use Chrome or Edge for the best Gamepad API support.

## Controller Mapping

| Input | Action |
| :--- | :--- |
| Left Stick | Field-centric translation request |
| Right Stick X | Rotation request |
| D-Pad | Heading snap |
| Y / Triangle | Reset heading to zero |

## What You See

- A robot body that translates and rotates from the simulated chassis state
- Four module blocks at the wheel positions
- Each module block rotates to show its current steering angle
- Each module block changes color with drive power direction and magnitude
- A side panel with pose, velocity, drivetrain state, heading hold state, and per-module targets vs actuals

## Why This Sim Stays Modular

The browser does not implement swerve logic.

Instead:

1. The browser reads the controller.
2. The Java simulator feeds those values into the existing control pipeline.
3. Mock module hardware stores actuator commands and simulated sensor feedback.
4. The browser only renders the resulting state.

That means drivetrain logic changes mostly belong in the normal Java code, not in the simulator UI.

## Files Involved

- Server entry point:
  `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/Swerve/Sim/SwerveSimulatorServer.java`
- Simulator engine:
  `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/Swerve/Sim/SwerveSimulator.java`
- Mock module hardware:
  `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/Swerve/Sim/MockSwerveModuleIO.java`
- Browser UI:
  `TeamCode/src/test/resources/swerve-sim/index.html`

## Extending It

If you want more realism later, the safest extension points are:

- improve the mock hardware response model in `MockSwerveModuleIO`
- add richer telemetry to the simulator state payload
- add field objects, traces, or path overlays in the HTML renderer

Avoid re-implementing drivetrain math in JavaScript. The whole point of this sim is to keep the browser dumb and the Java pipeline canonical.
