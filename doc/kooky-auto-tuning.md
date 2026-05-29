# Kooky-Style Auto Path Preview And Tuning

This auto is a fixed queue of drive-to-pose commands. A path is a start pose plus ordered target poses. The robot does not follow splines or Pedro/Road Runner motion profiles; it uses Kooky-style X/Y/heading PID to drive directly to each pose.

## Visualizing The Path

Use FTC Dashboard on the robot for live truth. `MiniDriveAuto` sends a field overlay with:

- Green dot: route start pose.
- Red dots: queued target poses.
- Blue lines: straight command segments.
- Black circle/line: current robot pose and heading from Pinpoint.

Open Dashboard while running `Mini Drive Auto`, then view the field overlay. This is the best tuning visual because it shows actual Pinpoint pose error while the same scheduler is driving the robot.

Use MeepMeep for pre-robot planning. Copy `doc/KookyAutoMeepMeepPreview.java` into the existing Juniper MeepMeep project. It previews the same start/target coordinates with `strafeToLinearHeading(...)` segments. Treat this as a geometry preview only; MeepMeep is not simulating our swerve PID or azimuth wait.

## Building A Path

Add waypoints in `AutoRoute`. Each waypoint becomes one scheduler command:

```java
new AutoWaypoint(xInches, yInches, headingRadians, settleDelayMs, timeoutMs)
```

The route builder automatically expands each waypoint into:

```text
WAIT_FOR_AZIMUTH -> MOVE_TO_POSE
```

when `WAIT_FOR_AZIMUTH` is enabled in `MiniDriveAuto`.

For Kooky-like behavior, prefer short, obvious waypoint segments. If a move needs to curve around something, split it into two or three pose commands rather than adding a follower.

## Auto Constants To Tune

Tune only these for drive-to-pose auto:

- `AUTO_X_P`
- `AUTO_X_D`
- `AUTO_Y_P`
- `AUTO_Y_D`
- `AUTO_HEADING_P`
- `AUTO_HEADING_D`
- `AUTO_MAX_TRANSLATION_POWER`
- `AUTO_MAX_TURN_POWER`
- `AUTO_TRANSLATION_TOLERANCE_IN`
- `AUTO_HEADING_TOLERANCE_RAD`
- `AUTO_TRANSLATION_DEADBAND`
- `AUTO_MOVE_TIMEOUT_MS`
- `AUTO_SETTLE_DELAY_MS`

Do not tune Pedro follower PIDF constants for this auto. They are not used.

## Exact Tuning Method

Start with current Kooky values:

```java
AUTO_X_P = 0.04;
AUTO_X_D = 0.05;
AUTO_Y_P = 0.04;
AUTO_Y_D = 0.05;
AUTO_HEADING_P = 0.6;
AUTO_HEADING_D = 0.3;
AUTO_MAX_TRANSLATION_POWER = 1.0;
AUTO_MAX_TURN_POWER = 0.5;
AUTO_TRANSLATION_TOLERANCE_IN = 0.25;
AUTO_HEADING_TOLERANCE_RAD = Math.toRadians(1.0);
AUTO_TRANSLATION_DEADBAND = 0.01;
AUTO_SETTLE_DELAY_MS = 0.0;
AUTO_MOVE_TIMEOUT_MS = 2500.0;
```

1. Tune heading first with a turn-only target. Set start pose to the real robot pose, set target X/Y equal to start X/Y, and change only heading by 90 degrees.

Pass condition: final heading error is within `AUTO_HEADING_TOLERANCE_RAD` three runs in a row without visible oscillation.

Fail condition: robot overshoots and wiggles, never settles, or times out before reaching heading.

Adjustment: increase `AUTO_HEADING_P` until it turns decisively, then increase `AUTO_HEADING_D` only enough to remove overshoot. If it buzzes or reverses repeatedly near target, reduce `AUTO_HEADING_D` first, then reduce `AUTO_HEADING_P`.

2. Tune X translation with a straight field-X move. Keep Y and heading constant, command a 24 inch X move.

Pass condition: final X error is within `AUTO_TRANSLATION_TOLERANCE_IN` three runs in a row, with Y drift less than 1 inch.

Fail condition: it stops short, overshoots by more than tolerance, or curves sideways more than 1 inch.

Adjustment: increase `AUTO_X_P` if it is lazy or stops short. Increase `AUTO_X_D` if it overshoots. Reduce `AUTO_X_D` if it chatters or crawls near the end.

3. Tune Y translation the same way. Keep X and heading constant, command a 24 inch Y move.

Pass condition: final Y error is within `AUTO_TRANSLATION_TOLERANCE_IN` three runs in a row, with X drift less than 1 inch.

Fail condition: it stops short, overshoots by more than tolerance, or curves sideways more than 1 inch.

Adjustment: tune `AUTO_Y_P` and `AUTO_Y_D` the same way as X. If X and Y need wildly different values, check module angle offsets and Pinpoint axes before hiding it with PID.

4. Tune diagonal translation. Command a 24 inch X and 24 inch Y move with constant heading.

Pass condition: final translation error is within tolerance and the Dashboard trace looks mostly straight.

Fail condition: one axis arrives much earlier than the other or the robot arcs heavily.

Adjustment: balance `AUTO_X_P/Y_P` first, then `AUTO_X_D/Y_D`. Keep X and Y gains close unless the robot consistently proves one axis is different.

5. Tune combined move. Command X, Y, and heading together using a real auto segment.

Pass condition: final pose is inside both translation and heading tolerances three runs in a row.

Fail condition: heading correction dominates translation, translation dominates heading, or it times out.

Adjustment: lower `AUTO_MAX_TURN_POWER` if heading yanks the robot off-line. Lower `AUTO_MAX_TRANSLATION_POWER` if it drives too aggressively for heading to keep up. Increase `AUTO_MOVE_TIMEOUT_MS` only after the motion itself is stable.

6. Set settle delay. Keep `AUTO_SETTLE_DELAY_MS = 0.0` for Kooky-like fast progression. Use `100-250 ms` only if the robot reaches tolerance briefly and immediately leaves it.

Pass condition: command advances only after pose is reliably inside tolerance.

Fail condition: auto feels slow between segments or advances on a lucky one-loop tolerance hit.

## Dashboard Signals To Watch

- `autoX`, `autoY`, `autoHeadingDeg`: current Pinpoint pose.
- `autoRoute`: selected route.
- `autoCommand`: current scheduler command.
- `loopMs`: loop time telemetry from `MiniDriveAuto`.
- Field overlay: actual robot circle should converge to each red target dot.

If Dashboard shows the robot pose moving correctly but the real robot does not, suspect drivetrain output or module hardware. If the real robot moves but Dashboard pose does not, suspect Pinpoint configuration or odometry wiring.

## Methodology Rules

- Change one constant family at a time.
- Run each test at least three times before trusting it.
- Prefer lowering max powers over adding complicated logic.
- Do not compensate for bad localization with PID gains.
- Do not tune with low battery and then expect the same behavior at full battery.
- Keep the MeepMeep preview and `AutoRoute` coordinates identical.
