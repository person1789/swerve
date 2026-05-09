# Kooky Auto Tuning

This guide applies to the simplified autonomous stack:

- `SimplePoseAuto`
- `SimpleAutoSequence`
- `KookyAutoController`

## What needs tuning

Only a small set of control constants matter now:

- `KookyAutoController.X_P`
  Pass if the robot closes forward/backward position error smoothly without long settling.
  Fail if it crawls, oscillates, or overshoots badly in the field X direction.

- `KookyAutoController.Y_P`
  Pass if the robot closes left/right position error smoothly without long settling.
  Fail if it crawls, oscillates, or overshoots badly in the field Y direction.

- `KookyAutoController.H_P`
  Pass if the robot rotates to target heading promptly without ringing.
  Fail if heading converges too slowly, overshoots repeatedly, or chatters near target.

- `KookyAutoController.MAX_TRANSLATION`
  Pass if autos remain fast but controllable and do not saturate so hard that pose correction becomes sloppy.
  Fail if motion is unnecessarily capped or if full-speed corrections make the robot unstable.

- `KookyAutoController.MAX_ROTATION`
  Pass if heading corrections are fast enough without causing module thrash.
  Fail if rotation is too weak to align on time or too aggressive for the modules to track cleanly.

- `SimpleAutoSequence.ALLOWED_TRANSLATIONAL_ERROR_IN`
  Pass if each step completes only after the robot is truly where it needs to be.
  Fail if steps advance too early or if the robot waits forever on tiny harmless residual error.

- `SimpleAutoSequence.ALLOWED_HEADING_ERROR_RAD`
  Pass if heading-sensitive steps only finish once the robot is acceptably aimed.
  Fail if steps advance while visibly mis-aimed or if the heading tolerance is unrealistically strict.

## What does not need tuning

These are not part of the new auto controller itself:

- `STEER_P`, `STEER_I`, `STEER_D`
  These still matter for module steering quality, but they are drivetrain/module tuning, not auto-path tuning.

- Teleop-only helper gains from the old architecture
  The simplified auto path does not use the retired follower gain groups.

## Tuning order

1. Tune `STEER_P / I / D` first so the modules track commanded angles reliably.
2. Tune `H_P` next so the robot can face target headings consistently.
3. Tune `X_P` and `Y_P` for translation convergence.
4. Tune `MAX_TRANSLATION` and `MAX_ROTATION` to match real traction and module response.
5. Tune finish tolerances last.

## Practical note

If translation is poor in both axes, do not assume `X_P` and `Y_P` are the first problem.
Bad module steering, weak heading control, or excessive drivetrain saturation can all look like bad translational PID.
