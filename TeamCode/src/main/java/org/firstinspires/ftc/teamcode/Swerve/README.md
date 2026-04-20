# Superior Swerve Control System

A high-performance, high-fidelity swerve drivetrain library optimized for the 20ms FTC control loop. 

## 🚀 Key Features
- **Second-Order Discretization**: Advanced kinematics that account for curved paths during high-speed rotation-translation.
- **Physics Feedforward**: Full $V = kS + kV \cdot v + kA \cdot a$ modeling for precise voltage-based motor response.
- **Hardware Health Monitoring**: Real-time current sensing and stall detection to protect motors and sensors.
- **Velocity Observer**: Fused encoder feedback providing a secondary source of truth for chassis velocity.
- **S-Curve Motion Smoothing**: Jerk-limited acceleration profiles for premium, vibration-free robot motion.

## 📁 System Architecture
- **[Hardware](Hardware/)**: Drivers for GoBILDA motors, Pinpoint odometry, and absolute encoders.
- **[Logic](Logic/)**: Core mathematical engines for kinematics, localization, and feedback observers.
- **[Geometry](Geometry/)**: Native vector and pose representations optimized for planar geometry.
- **[Input](Input/)**: Joystick scaling, deadbanding, and temporal filtering (LPF).
- **[Docs](Docs/)**: Comprehensive guides for tuning, diagnostics, and testing.

## 🛠️ Getting Started
1. **Initialize Hardware**: Map your motors and servos in `HWMap.java`.
2. **Calibrate Offsets**: Set your absolute encoder zero-points in `SwerveConfig.java`.
3. **Tuning**: Follow the [Advanced Settings Guide](Docs/AdvancedSettingsGuide.md) to tune your Physics Feedforward and LPF gains.

## 🧪 Testing and Verification
The system includes a robust JUnit test suite located in `src/test/java`.
- Run `AdvancedSystemTest.java` to verify kinematics and physics math.
- Run `FinalPolishTest.java` to verify basic logic stability.

---
*Built for excellence in competition robotics.*
