# Pedro + Custom Swerve Architecture

## Goal

Use one robot-motion stack for both:

- teleop driving
- Pedro autonomous path following

without forking Pedro and without throwing away the robot-specific behavior already built into the custom swerve code.

## Why this is not a hard fork of Pedro

Pedro already supports custom composition through:

- `FollowerBuilder.setLocalizer(...)`
- `FollowerBuilder.setDrivetrain(...)`

That means we can keep Pedro's follower, path generation, callbacks, and tuning flow while replacing the parts that are robot-specific.

This is better than a hard fork because:

- updates from Pedro are easier to take later
- our robot behavior stays in our own codebase
- teleop and auto can share code without duplicating logic

## What is shared now

The new shared class is:

- `PedroUnifiedSwerveStack`

It owns the custom motion stack:

- `HWMap`
- `SwerveLocalizer`
- `SwerveDrivetrain`
- `SwerveController`
- field-input `MotionSmoother`
- `LoopTimeEstimator`

This class is now the common layer for both modes.

## Teleop path

`MainTeleOp` now uses `PedroUnifiedSwerveStack` instead of creating the drivetrain, localizer, controller, and dt estimator separately.

Teleop still keeps its custom driver behavior:

- field-centric driving
- heading hold / heading snap state
- translation smoothing
- Pinpoint NaN fallback handling
- steer-transition authority shaping
- feasible translation filtering

So teleop is still custom, but it is now custom on top of the same shared stack that auto uses.

## Auto path

Pedro auto now uses adapters instead of Pedro's stock Pinpoint localizer and stock swerve drivetrain.

### Localizer adapter

- `PedroLocalizerAdapter`

This implements Pedro's `Localizer` interface but delegates to `SwerveLocalizer`.

That keeps:

- Pinpoint failover behavior
- IMU fallback
- multi-loop NaN protection
- the team's pose storage handoff

### Drivetrain adapter

- `PedroDrivetrainAdapter`

This extends Pedro's `CustomDrivetrain` and feeds Pedro's robot-centric drive requests into the custom `SwerveDrivetrain`.

That keeps:

- module steering transition logic
- steer-error authority shaping
- feasible translation limiting
- the same observer-based velocity feedback used in teleop

## Timing

The shared stack handles loop timing in one place.

### Teleop

Teleop still measures dt directly with the OpMode timer and sends it into the stack.

### Pedro auto

Pedro auto uses the shared stack's internal loop-timing path:

- first loop uses `20 ms`
- later loops use the rolling average estimator
- bad dt samples still get filtered out

This keeps the same dt behavior across both modes.

## Pose handoff

Pose handoff still uses:

- `PoseStorage`

Teleop can restore the last Pedro pose, and Pedro auto can restore the last teleop pose.

## What changed in the factory

`PedroSwerveFactory` no longer builds:

- stock Pedro `PinpointLocalizer`
- stock Pedro `Swerve` drivetrain

Instead it builds:

- `PedroUnifiedSwerveStack`
- `PedroLocalizerAdapter`
- `PedroDrivetrainAdapter`

and passes those into `FollowerBuilder`.

## Why this is the right middle ground

This gives us:

- one motion stack
- Pedro pathing for auto
- custom robot handling for teleop
- no library fork to maintain yet

If we ever find a real architectural reason to fork Pedro later, this adapter layer gives us a clean seam to do it from.

## Files to look at

- `PedroUnifiedSwerveStack.java`
- `PedroLocalizerAdapter.java`
- `PedroDrivetrainAdapter.java`
- `PedroBlockCommand.java`
- `PedroBlockRouteBuilder.java`
- `PedroSwerveFactory.java`
- `PedroLineAuto.java`
- `PedroPathChainAuto.java`
- `PedroDecodeRoute.java`
- `MainTeleOp.java`

## Block routes

The DECODE route now uses a small block-based route layer.

Each block specifies:

- field endpoint X/Y
- target robot heading
- straight or curved segment
- optional curve control point
- control-point scale
- heading interpolation weight

That means season routes can now be expressed as a sequence of simple movement blocks instead of hand-writing raw Pedro Bezier code in every autonomous.

The current builder files are:

- `PedroBlockCommand`
- `PedroBlockRouteBuilder`

This makes it easier to:

- string route pieces together
- scale curve aggressiveness
- swap field endpoints quickly
- reuse the same route definition in robot code and simulator code

## Route designer workflow

The browser simulator now includes a route-designer panel for this block-route layer.

The intended workflow is:

1. Pick a preset start pose or drag a custom start pose.
2. Set robot size so the outline matches the real footprint.
3. Optionally load a field image into the designer as a background reference.
4. Add straight and curved blocks.
5. Drag endpoints, curve control points, and heading handles until the route shape looks right.
6. Copy the generated `PedroBlockRouteBuilder.build(...)` snippet into robot code.

The generated code maps directly onto:

- `PedroStartPose`
- `PedroBlockCommand`
- `PedroBlockRouteBuilder`

If the start pose still matches a preset, the code uses:

- `PedroDecodeRoute.startPose(PedroStartPose.X)`

If the start pose was manually moved, the code uses:

- `PedroStartPose.custom(x, y, headingDeg)`

That keeps the route designer useful for both:

- common legal start locations
- quick custom experiments in the simulator

## Centripetal correction

Pedro's drivetrain API already separates:

- pathing power
- heading power
- corrective power

The corrective term is where Pedro's path-following compensation, including curved-path correction, enters the drivetrain side.

Because `PedroDrivetrainAdapter` extends Pedro's `CustomDrivetrain`, the adapter still receives Pedro's mixed robot-centric drive request after that correction logic has already been applied upstream by the follower.

That means this architecture keeps Pedro's path-following correction behavior while still routing the final command through the custom swerve stack.

## Live Dashboard tuning

The Pedro follower tuning values now live in:

- `SwerveConfig.java`

The important families are:

- translational PID
- heading PID
- drive PID
- centripetal scaling
- predictive braking
- mass and zero-power acceleration model values

The current Pedro autos and the tagged simulator wrapper now call:

- `PedroSwerveFactory.applyLiveTuning(follower)`

inside the active loop.

That means FTC Dashboard changes apply during the run instead of only at init time.

## Simulator status

The browser simulator now has a real Pedro autonomous entry:

- `DECODELaneAuto`

The simulator path is now backed by:

- a real Pedro `Follower`
- a simulator-backed Pedro `Localizer`
- a simulator-backed Pedro `CustomDrivetrain`
- the same shared DECODE route definition used by the robot auto

So:

- robot / Control Hub: full Pedro path
- current browser sim: full Pedro follower on top of simulator-backed pose and drivetrain interfaces

This is still a simulator, so the physics are only as good as the swerve sim model, but the autonomous logic itself is no longer a waypoint replay stand-in.

## Next likely work

1. Add a Pedro-assisted teleop test OpMode that uses Pedro's own teleop utilities on top of the same shared stack.
2. Reconcile Pedro pathing constants with the current robot after the first real autonomous tests.
3. Add autonomous docs for start pose, path chain setup, and end-of-auto pose handoff.
4. Improve simulator physics fidelity if we want even closer parity with the real robot.
