# CANONICAL: SwerveScope Implementation Plan

Status: Canonical plan

Precedence: This file supersedes `swervescope_master_implementation_plan.md`, `TODO.md`, and prior SwerveScope notes whenever they conflict with this implementation direction.

This file is the canonical SwerveScope implementation plan. If this file conflicts with `swervescope_master_implementation_plan.md`, `TODO.md`, or any prior notes, this file wins. All code, docs, architecture, and follow-on agent work must remain consistent with this file unless it is intentionally superseded by a newer canonical plan file.

## Agent Instructions
- Follow this file over any earlier SwerveScope plan.
- Keep implementation decisions consistent with this file.
- Do not reintroduce prototype-era architecture as the target state.
- Treat older docs as historical/reference material only.

## Implementation Guardrails
- The current React/Vite prototype is a migration base, not the target architecture.
- Electron desktop is the supported product runtime even if browser-first development remains useful.
- Java SITL is the authoritative simulation source in v1.
- Future work should use the `SwerveScope` name consistently rather than `swerve-sim`.

## SwerveScope End-State Migration Plan

### Summary
- Rebuild `swerve-scope` in place from the current React/Vite prototype into an Electron desktop diagnostics workbench with a dockable tiled workspace, using the existing app as the migration base rather than starting a separate app.
- Treat Java SITL as the authoritative simulation source in v1. Keep the current TypeScript port as a helper/reference layer, but defer true TS-only simulation to a later compatibility/offline phase.
- Include the full core platform in the first implementation wave: tab/workspace parity, richer telemetry protocol, improved simulation fidelity, session persistence/replay, expression-driven analysis, FFT/statistics, and tuning workflows.
- Explicitly defer 3D visualization to a later stretch phase.

### Key Changes
- Runtime and shell:
  - Keep Vite for renderer development, but make Electron the supported product runtime.
  - Replace the current tab-first shell with a dockable tiled workspace modeled after AdvantageScope behavior.
  - Keep the renderer browser-runnable during development, but all product-facing file/session features should be designed for Electron desktop use first.
- Frontend architecture:
  - Promote a shared application state layer for connection state, telemetry buffer, playback, session metadata, selected range, field registry, workspace layout, and panel settings.
  - Refactor current prototype panels into reusable workspace panes instead of top-level tab destinations.
  - Preserve and adapt proven pieces: `useHistory`, WebSocket ingestion, expression persistence, scrub/playback controls, current arena rendering, current inspector/console patterns.
  - Introduce panel contracts so every pane can consume the same timeline state, selected time window, and field registry without duplicating data transforms.
- Workspace model:
  - Use a dockable tiled workspace as the primary layout model.
  - Standard default layout should include: 2D field view, swerve detailer, line graph, inspector/table, console, joystick visualizer.
  - Persist workspace layouts locally and support reset-to-default.
  - Do not keep the current top-level tabs as the main navigation model except possibly for simple preset workspaces if needed later.
- Telemetry protocol and typing:
  - Define one canonical telemetry schema shared between Java emitter and TS consumer.
  - Extend the current `TelemetryFrame` to include at minimum: timestamp, pose, targets, actuals, maintain/snap state, snap target, raw/filtered gamepad snapshot, smoother state, observer velocity, current draw, battery voltage, loop time, drivetrain/controller mode, and log events if streamed.
  - Update the WebSocket bridge to map the full schema rather than silently dropping server fields.
  - Add schema versioning so the renderer can reject or warn on incompatible stream payloads.
- Java simulation backend:
  - Keep `SwerveSimServer` in `TeamCode` as the authority and evolve it instead of building a parallel primary sim.
  - Expand its telemetry output to match the canonical schema.
  - Improve fidelity around steering response, feedforward-aware drive response, battery sag/current draw, odometry noise, and drivetrain/controller mode transitions.
  - Add explicit log streaming from Java where possible so the desktop console can show real backend diagnostics instead of only synthetic UI-generated events.
- Analysis and visualization panes:
  - 2D Field View:
    - Keep the current arena as the starting point.
    - Add proper zoom/pan/reset, configurable trail length, coordinate readout, and target-vs-actual ghost pose rendering.
  - Swerve Detailer:
    - Add dedicated 4-module vector cards with target/actual overlays, speed/angle readouts, center chassis vector/rotation indicator, and friction/traction utilization indicators.
  - Line Graphs:
    - Replace the current selected-field-only scope with a more general field-driven graph pane.
    - Support multi-series plotting, field picker/registry, shared cursor with global playback, selected-range analysis, and unit-aware axis grouping.
  - Joystick Visualizer:
    - Show raw vs filtered inputs, D-pad/button state, and short input history.
    - Prefer server-provided gamepad data when connected to Java SITL.
  - Inspector/Table:
    - Keep live/raw/table modes, but extend the table to support historical browsing, column selection, row-to-timeline sync, and threshold coloring.
  - Console:
    - Keep severity filter/search/autoscroll/manual injection patterns where useful.
    - Add streamed Java logs, source tags, and timeline correlation.
  - Statistics/Analytics:
    - Add selected-range statistics, histogram/distribution view, and target-vs-actual comparison views.
  - Expression engine:
    - Replace the current `new Function` lightweight evaluator with a deliberate expression engine based on the planned analytics stack.
    - Make derived fields first-class plottable/selectable signals.
  - FFT/tuning:
    - Add FFT analysis on selected fields/ranges.
    - Add a guided feedforward/profiling workflow that consumes recorded sessions or dedicated capture routines.
- Persistence and replay:
  - Keep the in-memory ring buffer as the live recorder.
  - Add saved session persistence with metadata, searchable session browser, load/replay flow, and marker support.
  - Preserve CSV export, add column-aware export/import, and keep binary `.sslog` as a follow-on enhancement if time allows after the core persistence path is stable.
  - The playback system should be global and drive every pane consistently: play/pause/step, speed, frame/time labels, markers, selected analysis ranges.
- Version and dependency alignment:
  - Update the app dependencies to match the intended end-state stack where needed: Electron desktop packaging, React 19 target, dockable layout library, analytics libraries, persistence library, and any panel-specific additions.
  - Remove stale assumptions in docs/config only as part of the migration work; the implementation should not preserve the current mismatch between docs and actual stack.

### Implementation Approach
- Phase A: Platform foundation
  - Normalize package/runtime strategy for Electron + Vite.
  - Introduce shared app store/state and canonical telemetry schema.
  - Refactor the current shell into workspace/pane infrastructure.
- Phase B: Telemetry and backend alignment
  - Expand Java telemetry, log streaming, and schema versioning.
  - Update renderer ingestion/types/selectors to consume the full stream.
- Phase C: Core pane migration
  - Migrate current arena, inspector, console, joystick, and scope capabilities into workspace panes.
  - Build the new swerve detailer and improved graph pane on top of shared selectors.
- Phase D: Replay and persistence
  - Add saved sessions, loading, replay, timeline markers, and range selection.
- Phase E: Analytics
  - Add derived-field engine, statistics, histogram, FFT, and tuning/profiler workflows.
- Phase F: Deferred work
  - TS-only simulation mode.
  - 3D field visualization.

### Test Plan
- Telemetry/schema tests:
  - Java emission and TS parsing agree on field names, shapes, defaults, and version handling.
  - Unknown/missing fields fail safely and visibly.
- Backend simulation tests:
  - Existing swerve tests remain green.
  - Add targeted tests for expanded telemetry payload content and improved emulator behaviors where deterministic.
- Renderer state tests:
  - History/playback, range selection, markers, workspace persistence, and session load/save work without pane desynchronization.
- Pane behavior tests:
  - Arena, detailer, graphs, console, joystick, and inspector all render correctly in live mode and replay mode.
  - Graph field selection and derived expressions update correctly and remain unit-consistent.

### Assumptions and Defaults
- Electron desktop is the supported end-state runtime.
- Java SITL remains the authoritative simulation source in the first full implementation.
- The current React/Vite prototype is a migration base, not the final architecture.
- Dockable tiled workspace is the primary UX model.
- Analytics are in scope for the first end-state implementation; 3D is not.
