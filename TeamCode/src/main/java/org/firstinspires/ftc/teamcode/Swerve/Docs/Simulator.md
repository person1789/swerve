# 🎮 Swerve Simulation Guide

This guide covers everything you need to know to run and use the **Next.js Swerve Simulator**, a high-fidelity "Digital Twin" of your robot.

## 🚀 Getting Started

The simulator is located in the `./swerve-scope` directory. It uses the exact same `SwerveKinematics` and `MotionSmoother` logic as your actual robot.

### 1. Installation
If this is your first time running the sim on a new machine:
```bash
cd swerve-sim
npm install
```

### 2. Launching the App
Start the development server:
```bash
npm run dev
```
Open the provided `localhost:5173` link in a modern browser (**Chrome** or **Edge** are recommended for the best Gamepad support).

## 🕹️ Controller Mapping

The simulator is designed for use with a physical Xbox or PlayStation controller.

| Input | Action |
| :--- | :--- |
| **Left Stick** | XY Translation (Robot-Relative) |
| **Right Stick** | Rotation (Yaw / Angular Velocity) |
| **A / X Button** | Toggle Vector Overlays |

## 📊 Telemetry & Visualization

### 1. Vector Overlays
- **Green Arrows**: Individual Wheel Velocities.
- **Blue Center Arrow**: Overall Chassis Translation Vector.
- **Purple Arc**: Angular Velocity indicator.

### 2. Virtual HUD
The top-left corner displays real-time telemetry calculated by the JS engine:
- **Pose X/Y**: Field position in meters.
- **Heading**: Current rotation in degrees.
- **Linear V**: Current integrated speed in m/s.

## 🔴 Front Indicator
To assist with Field-Centric driving practice, the robot chassis features a **vibrant red edge** indicating the "Front" of the robot. The other three sides are blue.

## 🛠️ Modifying the Sim
If you change your physical robot's `TRACK_WIDTH` or `MAX_SPEED` in `SwerveConfig.java`, you should update the corresponding values in `swerve-scope/src/lib/SwerveLogic.ts` to keep the simulation accurate.
