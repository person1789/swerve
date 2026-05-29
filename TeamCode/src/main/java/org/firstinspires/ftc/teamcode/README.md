# Swerve Control System

This branch intentionally keeps the active robot path small:

- `OpModes/MainTeleOp.java`
- `Swerve/Hardware/SwerveDrivetrain.java`
- `Swerve/Hardware/SwerveModule.java`
- `Swerve/Logic/Kinematics/SwerveKinematics.java`

The drivetrain follows the Kooky-style direct command pipeline. The only custom
pieces retained are the drivetrain FSM and second-order kinematics.

Run local unit tests with:

```powershell
.\gradlew :TeamCode:testDebugUnitTest
```
