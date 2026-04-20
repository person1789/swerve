# Local Unit Testing Guide — Android Studio Integration

This guide explains how to run the Swerve Control System tests directly on your computer (Host Machine) within Android Studio, allowing for rapid iteration without needing a physical robot.

## 1. Running via the IDE (Recommended)
Android Studio provides a visual way to run and debug tests.

1.  Open the Project view in Android Studio.
2.  Navigate to `TeamCode > src > test > java > ... > Swerve > Tests`.
3.  **Right-Click** on `MasterTestSuite.java`.
4.  Select **Run 'MasterTestSuite.main()'**.
5.  The results will appear in the **Run** tab at the bottom of the IDE with full color formatting.

## 2. Running via Terminal
If you prefer the command line within the IDE terminal:

```bash
# In the Android Studio Terminal (bottom of screen)
./gradlew :TeamCode:testDebugUnitTest --tests "org.firstinspires.ftc.teamcode.Swerve.Tests.MasterTestSuite"
```

## 3. Interpreting Results
The `MasterTestSuite` is configured with professional status indicators:

- **✔ (Checkmark)**: The logic passed the mathematical verification.
- **✘ (Cross)**: An error was detected. The "Reason" line will tell you exactly what assertion failed (e.g., expected 1.0 but got 1.5).
- **⚠ (Warning)**: Indicates a setup error, usually caused by a class trying to access robot hardware (which is not available on your PC).

## 4. Key Rule for Local Tests
Only classes that perform **Pure Math or Logic** can be tested this way. 
> [!IMPORTANT]
> If a test tries to talk to a physical Motor (`DcMotorEx`) or Sensor (`IMU`) without a Mock, the test will throw a `RuntimeException`. Use the provided **Logic Tests** (Kinematics, PID, Smoother) to calibrate your "Brain" before deploying to the "Body".
