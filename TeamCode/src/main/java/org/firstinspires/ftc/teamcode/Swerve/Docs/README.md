# Swerve Library Overview

Welcome to the refactored swerve codebase. The architecture is designed to keep drivetrain logic modular, testable, and easier to tune.

## Core Features

- Unified vector math through `Vector`
- Pinpoint-plus-IMU-plus-observer localization architecture
- Centralized tuning in `SwerveConfig`
- Local JVM tests for pure logic
- A thin mocked-hardware simulator for drivetrain behavior

## Quick Start For Drivers

### 1. Heading Calibration

Press the `Options/Start` button to reset the field-centric heading.

### 2. Cardinal Snapping

Use the D-pad to snap the robot heading to the configured cardinal targets.

### 3. Precision Control

The input pipeline uses non-linear shaping plus motion smoothing for finer low-speed control.

## Developer Guide

### Where is the brain?

- `SwerveController` handles heading-maintain and snapping decisions.
- `MotionSmoother` shapes requested motion.
- `SwerveDrivetrain` coordinates the hardware-facing pipeline.

### What should I read first?

- `AdvancedSettingsGuide.md` for scope and drivetrain assumptions
- `PIDTuningGuide.md` for loop-by-loop tuning order
- `UnitTestingGuide.md` for local tests and Gradle tasks
- `CodebaseAudit.md` for known issues and stale areas

---
Built for iteration, tuning, and competition use.
