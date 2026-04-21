# SwerveScope Chat Context

This file is contextual background, not the governing spec. If any note here conflicts with implementation direction, `SWERVESCOPE_CANONICAL_IMPLEMENTATION_PLAN.md` is authoritative.

## User Intent
- Preserve the plan from this chat in-repo so future agents follow it instead of older SwerveScope notes.
- Mark one plan file as canonical and make the other a supporting context artifact.
- Begin implementation immediately against the canonical plan instead of preserving the current prototype architecture.

## Current Repo Reality Discovered During Exploration
- The repository is an FTC Android/Gradle robot project with a separate `swerve-scope` renderer app.
- The current renderer is a Vite + React prototype with a tab-first shell, not the full planned dockable desktop workbench.
- `swerve-scope/main.cjs` already contains an Electron entry point, but `package.json` does not yet represent the end-state stack described by older planning docs.
- The Java SITL authority lives in `TeamCode/src/test/java/.../Swerve/Tests/SwerveSimServer.java`.
- The production swerve code lives under `TeamCode/src/main/java/.../Swerve`.

## Key Mismatches Found
- Older docs describe a richer Electron/React 19/AdvantageScope-like workbench, while the current app is still a simpler React 18 prototype.
- Older docs reference `swerve-sim`, but the real folder is `swerve-scope`.
- The Java SITL server already emits richer data than the TypeScript client consumed at the time of exploration.
- The current frontend previously dropped or ignored telemetry such as `gamepad`, `observerVel`, and `currentDraw`.

## Product Decisions Chosen In This Chat
- Full rebuild to end-state
- Electron desktop target
- Migrate in place
- Java-first, TS fallback later
- Include analytics, defer 3D
- Dockable tiled workspace

## Frontend Structure Notes
- `App.tsx` originally owned a tab-based shell with playback and split-pane logic.
- Existing renderer components that were already useful and worth migrating:
  - `Arena.tsx`
  - `ModuleDetail.tsx`
  - `ScopePanel.tsx`
  - `InspectorPanel.tsx`
  - `ConsolePanel.tsx`
  - `JoystickPanel.tsx`
  - `ExprEditor.tsx`
- Existing hooks that were already useful and worth migrating:
  - `useHistory.ts`
  - `useSITL.ts`
  - `useGamepad.ts`

## Backend Structure Notes
- `SwerveController`, `SwerveKinematics`, and `MotionSmoother` are core production logic reused by simulation.
- `SwerveSimServer.java` is the Java-side SITL bridge and should remain the authoritative v1 simulation source.
- Existing Java tests are part of the confidence model and should remain aligned with any telemetry/schema expansion.

## Telemetry Notes
- The renderer should use one canonical telemetry schema shared conceptually between Java emission and TS consumption.
- Important telemetry areas identified during this chat:
  - pose
  - targets and actuals
  - controller state
  - snap target
  - gamepad inputs
  - observer velocity
  - smoother state
  - current draw
  - battery voltage
  - loop timing
  - structured logs where available

## Guidance For Future Agents
- Use this file for background only.
- Use the canonical implementation plan file for decisions.
- Do not preserve prototype-era architecture just because it exists in the current codebase.
