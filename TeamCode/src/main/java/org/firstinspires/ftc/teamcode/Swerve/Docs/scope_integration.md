# Swerve Scope Integration

Swerve Scope is a diagnostic workbench designed to visualize the internal state of the robot in real-time. To make the redo successful, the control system needs to be designed for visibility.

## Principles of "Observable" Controls
1. **Double-Entry Logging**: Every target must have a measured counterpart (e.g., `targetAngle` vs `actualAngle`).
2. **Frequency Alignment**: Telemetry should ideally be sent at the same frequency as the control loop (e.g., 50Hz or 100Hz).
3. **Structured Data**: Use JSON or a robust binary format to send data to the Scope.

## Integration Points
### 1. Module Debugging
Send the following for each module:
- Drive motor power/velocity.
- Turn motor target angle.
- Turn motor actual angle (encoder position).
- PID error.

### 2. Kinematics Simulation
The Scope should be able to simulate the kinematics layer:
- Feed the Scope the 4 module vectors.
- Let the Scope calculate the resultant robot vector and compare it with the robot's intended motion.

### 3. Localization Visualization
- Stream the `(x, y, heading)` pose to the Scope field view.
- Stream the "Path" or "Waypoints" so the Scope can show the robot's intended trajectory vs actual trajectory.

## Usage in Swerve Redo
As you build the new layers, keep a `telemetry` method in each class that populates a map of data to be sent to the Scope.
