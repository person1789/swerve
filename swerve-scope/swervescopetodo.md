# SwerveScope TODO

This file is a working implementation backlog for the current `swerve-scope` app state.

It is not the canonical product spec.

Authoritative direction still lives in:

- `SWERVESCOPE_CANONICAL_IMPLEMENTATION_PLAN.md`
- `SWERVESCOPE_CHAT_CONTEXT.md`

## Current State

What is already in place:

- Docked workspace shell
- Arena / field pane
- Module detail pane
- Graph pane with field registry
- Inspector pane with scrub-to-row
- Console pane
- Joystick pane
- Expression pane
- Local TypeScript simulation fallback
- Java SITL websocket path
- Basic playback/history

What is still not in a finished state:

- Overall responsiveness is still not good enough
- Pane lifecycle/render isolation is incomplete
- Some pane scrolling/layout behavior is still unreliable
- Persistence/replay/session tooling is not finished
- Java-first full telemetry parity is not complete

## Highest Priority

### 1. Fix Whole-App Lag

The app still feels laggy in practice even after several targeted reductions.

Likely causes still remaining:

- The full app shell still rerenders too often when telemetry advances
- Hidden panes are still mounted and still doing work
- Some heavy panes still recompute too much per update
- React state is still being used too high in the tree for live telemetry

Required follow-up:

- Split live telemetry subscriptions by pane instead of pushing all frame changes through the whole shell
- Make pane rendering visibility-aware
- Prevent inactive panes from doing expensive work
- Measure with React DevTools profiler and browser performance profiler

### 2. Hidden Panes Should Not Render

Current problem:

- Panes swapped out of a slot are not shown, but the architecture still needs a stricter "only active visible pane does work" model

Required follow-up:

- Ensure only the pane selected in each slot is mounted
- If multiple panes remain mounted indirectly, refactor slot rendering to hard-unmount non-visible panes
- Consider lazy mounting for expensive panes
- Pause polling/timers/plot updates when pane is not active

### 3. Fix Pane Scrolling Completely

User-reported issue still unresolved:

- Some panes, especially `System Console`, still do not reliably scroll through all content

Nuances:

- Nested flex + `min-h-0` + `overflow` chains must be correct all the way down
- A pane can have `overflow-y-auto` and still fail if any parent in the chain has unconstrained height
- Some wheel events may be effectively consumed by parent containers depending on exact DOM sizing

Required follow-up:

- Inspect actual rendered DOM in browser devtools for console pane
- Verify computed heights on:
  - workspace slot wrapper
  - panel
  - panel body
  - console root
  - console list container
- If needed, explicitly set:
  - `height: 100%`
  - `min-height: 0`
  - `overflow: hidden`
  on every intermediate wrapper
- Add direct wheel handling fallback if browser layout still fights scrolling

## Performance TODOs

### Arena / Field View

Current state:

- Dedicated RAF loop added
- Interpolation between previous and current telemetry frame added
- Field/robot path intended to remain 60 FPS

Remaining work:

- Verify actual smoothness under load, not just code intent
- Ensure canvas draw loop is isolated from non-arena pane updates
- Consider trail simplification / decimation if trail size grows large
- Consider offscreen caching for static field/grid/AprilTag layers

### Graph Pane

Current state:

- uPlot renders
- Field registry works
- Stats panel exists

Remaining work:

- Only update graph when pane is visible
- Avoid recomputing all derived series every UI tick
- Cache expression results per frame where possible
- Add multi-axis grouping by unit
- Add range selection
- Add cursor-to-global scrubber sync

### Console Pane

Current state:

- Severity filters
- Search
- Manual injection
- Synthetic events from telemetry transitions

Remaining work:

- Fix scroll reliability
- Limit log growth smarter
- Avoid generating repetitive threshold logs every frame
- Add source grouping / source filters
- Add streamed Java logs with better schema support

### Module Detail Pane

Current state:

- 4 modules
- center chassis summary
- target vs actual
- traction/current visualization

Remaining work:

- Confirm scroll/layout behavior in smaller slots
- Add better chassis rotation visualization
- Add target vs actual delta summaries
- Add configurable vector scaling

### Joystick Pane

Current state:

- Gamepad polling reduced
- Telemetry fallback exists

Remaining work:

- Confirm no polling churn remains
- Add raw vs filtered distinction clearly in UI
- Add short input history trails
- Add keyboard fallback indication when no gamepad is present

## Architecture TODOs

### 4. Move Live Telemetry Out Of Top-Level Render Path

Current issue:

- `App.tsx` + shared state still receive enough live updates that large parts of the tree rerender together

Better target:

- Shared ring buffer store
- Pane-specific selectors/subscriptions
- Top-level shell only rerenders for layout and control changes
- Data panes rerender only for their own consumed slice

Candidate directions:

- Zustand-style store
- `useSyncExternalStore`
- custom subscription store around telemetry/history/playback state

### 5. Separate "Live Transport" From "Playback View"

Current state:

- Transport, history, and viewed frame are mixed closely

Needed:

- Raw live ingest buffer
- playback cursor state
- derived viewed frame state
- pane subscriptions to viewed frame only when needed

This will likely improve both:

- responsiveness
- replay correctness

### 6. Formalize Pane Contracts

Needed:

- each pane should declare what it needs:
  - live frame
  - viewed frame
  - history window
  - selected range
  - metadata only

Why:

- Prevent every pane from getting the full state bag
- Makes performance work tractable

## Simulation TODOs

### 7. Finish Local TS Simulation

Current state:

- Local fallback works enough for basic testing

Remaining work:

- Validate behavior against Java SITL more carefully
- Add explicit local simulation reset action
- Add clear local keyboard help in UI
- Make local sim timing deterministic enough for repeatable testing
- Consider exposing sim params in UI

### 8. Finish Java SITL Integration

Current state:

- WebSocket connect path exists
- Java server emits fairly rich telemetry

Remaining work:

- Verify exact launch flow
- Confirm `npm run sitl:java` works on this machine consistently
- Confirm websocket reconnect behavior across Java restarts
- Finish schema parity review field-by-field
- Add schema version handling beyond warning text

### 9. Telemetry Schema Parity Audit

Need a strict diff between:

- Java emitted fields
- TS `TelemetryFrame`
- what each pane actually consumes

Specifically verify:

- `logs`
- `gamepad`
- `observerVel`
- `smootherState`
- `currentDraw`
- `batteryVoltage`
- `controllerMode`
- `drivetrainState`
- `snapTargetRad`

## UX TODOs

### 10. Make Runtime Mode Obvious

Current state:

- Control bar shows `JAVA // SITL` vs `LOCAL // TS`

Remaining work:

- Add clearer connection health wording
- Show whether gamepad or keyboard is driving local mode
- Add explicit "connected to websocket" timestamp / status detail if useful

### 11. Improve Resize UX

Current issues:

- small tiles can become cramped quickly
- some panes are not graceful at very small sizes

Needed:

- enforce better minimum slot sizes
- possibly allow pane-specific min sizes
- better small-layout behavior for console/detailer/joystick

### 12. Add Reset / Clear Actions

Needed:

- reset local sim pose
- clear console logs
- clear trail only
- reset graphs
- reset workspace layout

Some exist already, but they are not complete or not pane-specific enough.

## Persistence / Replay TODOs

### 13. Saved Sessions

Not finished.

Needed:

- session metadata model
- save current buffer
- browse saved sessions
- load and replay saved sessions
- delete saved sessions

### 14. Timeline / Playback Improvements

Current state:

- scrubber
- play/pause
- playback rate

Remaining work:

- markers
- selected analysis range
- frame/time jump controls
- better live vs replay transition semantics

### 15. Import / Export

Current state:

- CSV export exists in inspector flow

Remaining work:

- configurable columns
- CSV import
- dedicated export UI
- future `.sslog` format if still desired

## Analytics TODOs

### 16. Expression Engine Upgrade

Current state:

- `new Function`-style lightweight evaluator

Needed:

- safer deliberate evaluator
- derived field caching
- better validation/errors
- first-class unit metadata

### 17. Statistics / Range Analysis

Current state:

- graph shows basic visible-window stats

Needed:

- selected range stats
- target vs actual comparison
- histogram/distribution view
- RMS / std dev / mean / median

### 18. FFT / Tuning Workflow

Not started in a real way.

Needed:

- FFT pane or graph mode
- selected signal analysis
- feedforward characterization workflow
- profiler session flow

## Electron / Runtime TODOs

### 19. Finish Actual Desktop Runtime

Current state:

- `main.cjs` exists
- app is still primarily being exercised as Vite browser app

Needed:

- verify Electron runtime end-to-end
- ensure Java child-process launch path is correct
- ensure shutdown cleanup is reliable
- decide whether browser-only and Electron-only should share the same commands or separate ones

### 20. Package Scripts Cleanup

Current scripts are better than before but still not final.

Needed:

- `dev`
- `dev:open`
- `dev:host`
- `sitl:java`
- possibly `dev:electron`
- possibly `test:renderer`
- possibly `test:sitl`

## Testing TODOs

### 21. Manual Validation Checklist

Need a repeatable checklist for:

- local TS mode startup
- Java SITL mode startup
- field rendering smoothness
- console scrolling
- graph responsiveness
- playback
- pane resize behavior
- gamepad path
- keyboard fallback path

### 22. Automated Coverage

Currently weak / incomplete on renderer behavior.

Needed:

- history/playback store tests
- telemetry parsing tests
- derived field evaluation tests
- pane rendering smoke tests
- workspace layout persistence tests

## Known Nuances / Gotchas

### Rendering nuance

- The field/robot really does need its own smooth render path.
- The rest of the UI does not need 60 FPS.
- Trying to drive the whole shell at telemetry cadence hurts everything.

### Layout nuance

- Scroll bugs in nested flex layouts are usually parent sizing bugs, not child overflow bugs.
- `min-h-0` is necessary but not sufficient.
- Every intermediate wrapper between slot and scroll area matters.

### Local sim nuance

- Local sim is for testability and iteration speed.
- Java SITL remains the authoritative v1 behavior target.
- Local sim should not drift silently from Java assumptions.

### Performance nuance

- Hidden or inactive panes must not keep expensive loops running.
- Graphs, console processing, joystick polling, and field rendering each need separate performance treatment.

### Product nuance

- The current app is a migration base, not the final architecture.
- It is usable enough for iteration, but not yet at the canonical-quality end state.

## Suggested Next Order Of Work

1. Fix console and pane scrolling completely
2. Make hidden panes truly not render/do work
3. Move telemetry/view state into a pane-subscribed store
4. Re-profile and fix remaining lag
5. Finish persistence/replay
6. Finish Java schema parity audit
7. Add range stats / analytics
8. Finish Electron runtime path
