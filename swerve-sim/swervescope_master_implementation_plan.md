# SwerveScope Master Implementation Plan [Windows-Native / Professional Spec]

This is the definitive master roadmap for SwerveScope, optimized for a Windows-native environment with a custom high-performance binary architecture. This plan focuses on replacing generic browser-based telemetry with a professional diagnostic suite tailored for the mechanical and electrical complexities of swerve drive.

---

## Part 1: Detailed Technical Specifications

### 1. Physics Engine (Virtual Hardware Layer)
High-fidelity simulation is achieved by emulating the physical response of the hardware, not just the kinematics.
- **Swerve Module Emulators**: Simulates steering and drive motor lag using a 1st-order system (Low-Pass Filter) with configurable time constants ($~50ms$).
  - **Steering Physics**: Models angular acceleration limits and backlash.
  - **Velocity Physics**: Models friction ($kS$), back-EMF ($kV$), and inertial load ($kA$).
- **Pinpoint Odometry Emulator**: A high-speed (250Hz) virtual sensor that integrates chassis-relative velocities into field-relative coordinates using 2nd-order discretization to minimize error during high-speed rotation.
- **Power Budget Modeling**: Estimating current draw for each module based on acceleration and load to identify potential brownouts in logs.

### 2. Control Theory & Signal Processing
The diagnostic tools are designed to evaluate the performance of our custom control suite.
- **MotionSmoother (Jerk-Limited S-Curve)**:
  - Implements asymmetric braking: Instant snap-back for driver-initiated deceleration, but smoothed ramping for system-level scaling.
  - Limits 3rd-order derivative (Jerk) to prevent mechanical vibration and wheel slip.
- **SwerveAuditor (Vector Optimization)**:
  - **Phase Switching**: Automatically flips module direction and angle by 180° when the error exceeds 90° to minimize steering travel.
  - **Continuity Logic**: Ensures steering angles don't "loop around" the 0/360 boundary violently.
- **SwerveVelocityObserver (State Estimation)**:
  - Fuses multiple module inputs into a single chassis-velocity estimate using a temporal LPF to filter out encoder noise.

### 3. Networking & Real-time Bridge
- **SITL (Software In The Loop)**:
  - **WebSocket Bridge**: Connects the Java "Brain" to the TypeScript "Visualizer" over port 8080.
  - **Jackson Serialization**: High-speed JSON exchange for real-time gamepad inputs (Browser → Java) and robot state (Java → Browser).
- **Control Hub Bridge (Planned)**: A native TCP/UDP listener that bypasses the browser's networking stack for <5ms latency.

### 4. Data Architecture (.sslog)
Instead of standard CSV, SwerveScope will utilize a custom binary format.
- **Binary Schema**:
  - `Header`: 4 bytes (Magic Number), 4 bytes (Version), 8 bytes (Start Timestamp).
  - `Frame`: Float32 Array of [X, Y, Heading, M1_V, M1_A, M2_V, M2_A, M3_V, M3_A, M4_V, M4_A, Current_Draw, Loop_Time].
- **Instant Seek**: The binary format allows O(1) seeking to any point in a 30-minute log without loading the entire file into memory.

---

## Part 2: The Full Feature List

### 1. Windows-Native Core & Telemetry
- **Native TCP/UDP Bridge**: High-speed connection to Control Hub.
- **.sslog (SwerveScope Log) Format**: custom binary file type for high-speed read/write.
- **ADB Native Integration**: "One-Click Sync" logs from the Robot Controller via USB/Wi-Fi.
- **Live Rewind (Flight Recorder)**: High-speed local caching for real-time history scrubbing.

### 2. Advanced Swerve Visualization
- **Procedural 3D Chassis**: Three.js powered 3D robot visualizing rotation and wheel spin.
- **Target vs. Actual "Ghosting"**: Visualizes the "Control Gap" by rendering a semi-transparent ghost robot (Target) overlaid on the solid robot (Actual).
- **4-Pane Vector Detailer**: High-density dashboard showing direction and magnitude vectors for all modules.
- **Traction Circle Visualizer**: Indicates if a module is saturating its friction budget.

### 3. Physics-Based Diagnostics
- **FFT (Fast Fourier Transform)**: Pane for frequency analysis to isolate mechanical vibrations.
- **Module Phase Analysis**: Graphing current spikes vs. angle changes to detect mechanical binding.
- **Odometry Drift Tracker**: Calculating the delta between Vision Pose and Encoder Pose.

---

## Part 3: Implementation Phases

### Phase 1: Windows & Telemetry Foundation [In Progress]
- [x] Implement the **Mosaic Tiling Manager** for the drag-and-drop Windows UI.
- [/] Define the **.sslog binary schema** for maximum data density.
- [x] Build the **high-speed SITL bridge** (WebSocket) for Java parity.

### Phase 2: The 3D Engine & Ghosting
- [ ] Create the **Procedural 3D Robot Model** (Three.js).
- [ ] Implement **Target vs. Actual "Ghost" Overlay** in the 3D view.
- [ ] Add the **12'x12' Field Sandbox** with measurement grids.

### Phase 3: Deep Signal Analytics
- [ ] Build the **FFT Analysis Pane** for noise diagnostics.
- [ ] Integrate the **Visual Math Engine** (Node-based UI) for calculated fields.
- [ ] Create the **Live Filter Playground** for real-time tuning of LPF constants.

### Phase 4: Swerve Physics & Validation
- [ ] Develop the **Swerve Detailer** (4-pane vector view).
- [ ] Implement **Traction Circle Visuals** and **Wheel Slip Heatmap**.
- [ ] Create **Inconsistency Triggers** to auto-flag "Virtual Skipping" events.

### Phase 5: Automated Profiling & Optimization
- [ ] Build the **Auto-Tuning Profiler** to calculate $kS$, $kV$, and $kA$ from logs.
- [ ] Finalize **ADB One-Click Sync** for log management.
- [ ] Implement **Workspace Snapshots** for custom layouts.

---

## 🛠 Technical Stack Summary
- **Frontend**: React / Three.js / react-mosaic-component
- **Backend (Desktop Shell)**: Electron / Node.js
- **Sim Brain**: Java (Production Logic Reused)
- **Data**: .sslog (Custom Binary) / Dexie (IndexedDB)
- **Math**: mathjs / dsp.js
