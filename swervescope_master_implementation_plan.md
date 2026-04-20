# SwerveScope Master Implementation Plan [AdvantageScope Total Parity]

This document outlines the 8-phase professional roadmap to transform SwerveScope into a total functional equivalent of **AdvantageScope**, tailored specifically for swerve drive physics and FTC/FRC telemetry analysis.

---

## Phase 1: 3D Visualization Foundation
**Goal**: Transition from a 2D canvas to a high-performance 3D field environment.
- [ ] **Three.js + React Three Fiber Integration**: Setup the WebGL rendering pipeline.
- [ ] **Procedural Swerve Chassis**: 3D robot model with dynamic steering modules and wheel spin physics.
- [ ] **12'x12' Engineering Field**: High-fidelity field model with grid overlays and measurement tools.
- [ ] **Cinematic vs. Performance Modes**: Rendering switches for shadows, anti-aliasing, and framerate.

## Phase 2: High-Frequency Data Persistence
**Goal**: Enable massive temporal history and historical log management.
- [ ] **IndexedDB "Flight Recorder"**: Database for storing 50Hz telemetry streams (up to 100,000 packets per session).
- [ ] **Log Archive UI**: Browser for managing saved sessions with metadata tagging (Date, Robot, Event).
- [ ] **Import/Export Suite**: Support for `.swlog` (JSON), `.csv`, and `.wpilog` (WPILib) formats.

## Phase 3: The Command Center (Mosaic Layouts)
**Goal**: Professional, drag-and-drop window management.
- [ ] **Mosaic Tiling Manager**: Resizable, swappable panes for specialized views.
- [ ] **Tab Synchronization**: Global "Snap-to-Frame" logic across all open windows.
- [ ] **Workspace Snapshots**: Save and load custom arrangements of graphs/tables/3D views.

## Phase 4: Expressions & Virtual Telemetry
**Goal**: Real-time math on telemetry streams.
- [ ] **Expression Parser**: Allow users to write JS/Math logic to create new fields (e.g. `PID_Error = target - actual`).
- [ ] **Virtual Sensors**: Calculate derived values like "Distance to Tag" or "Est. Slip Velocity" on-the-fly.

## Phase 5: Specialized Analytical Tabs
**Goal**: Dedicated visualizers for complex subsystems.
- [ ] **Joystick Visualizer**: Render real-time/historical gamepad inputs with button state overlays.
- [ ] **Swerve Detailer**: High-density 4-pane vector view with wheel speed, angle, and current draw labels.
- [ ] **Mechanism 2D/3D Builder**: Visualizing robot arms, elevators, or intakes using simple telemetry definitions.
- [ ] **Mission Console**: High-performance scrolling text with log priority and keyword filtering.

## Phase 6: Deep Diagnostics & Statistics
**Goal**: Automated performance analysis.
- [ ] **Statistics Engine**: Histograms, Standard Deviation, and RMS Error calculation for any selected range.
- [ ] **FFT Analysis**: Frequency domain visualization for detecting drivetrain vibrations or PID oscillations.
- [ ] **Auto-Tuning Profiler**: Automatic calculation of feed-forward constants (kS, kV, kA) from log data.

## Phase 7: External Data Synchronization
**Goal**: Correlating logs with real-world visuals.
- [ ] **Video Sync Overlay**: Synchronize external `.mp4` video files with the telemetry timeline.
- [ ] **Multi-Source Sync**: Align and overlay two different log sessions (e.g., comparing "Driver A" vs "Driver B").
- [ ] **State Machine Visualizer**: 2D flowchart showing active states and transition triggers from the Java backend.

## Phase 8: Spatial Parity (XR & Vision)
**Goal**: Visualizing what the robot "sees".
- [ ] **AprilTag 3D Overlay**: Render field tags and robot-estimated tag coordinates in 3D.
- [ ] **Vision Range Heatmaps**: Spatial breadcrumb trails colored by vision-confidence or latency.
- [ ] **Obstacle Point Clouds**: Render lidar or simulated sensor "Point Clouds" around the 3D chassis.

---

## 🛠 Required Tech Stack Upgrades
- **Rendering**: `three`, `@react-three/fiber`, `@react-three/drei`
- **Layout**: `react-mosaic-component`
- **Math/Expressions**: `mathjs`
- **Database**: `dexie` (IndexedDB Wrapper)
- **Charts**: `uplot` (Already installed)
