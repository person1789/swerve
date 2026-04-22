# Unit Testing for Swerve Controls

Unit tests are essential for verifying the complex math involved in swerve drive without waiting for a build to upload to the robot.

## Setting Up JUnit
In FTC (Android Studio), unit tests go in `TeamCode/src/test/java`. These run on your computer (JVM), not the robot.

### What to Test
1. **Math Utilities**: Any custom angle wrapping or vector math.
2. **Kinematics**: 
   - If I give a robot velocity of (1, 0, 0), do all modules point forward?
   - If I give (0, 0, 1), do the modules form a circle?
3. **PID Loops**: Verify that the output decreases as error decreases.
4. **Motion Profiling**: Ensure trajectories are smooth and continuous.

## Example Test Case (JUnit 4/5)
```java
public class KinematicsTest {
    @Test
    public void testForwardKinematics() {
        SwerveKinematics kinematics = new SwerveKinematics(TRACK_WIDTH, WHEEL_BASE);
        
        // Robot wants to move forward at 1.0 power
        ModuleState[] states = kinematics.calculate(new Pose(1.0, 0, 0));
        
        for (ModuleState state : states) {
            assertEquals(1.0, state.power, 1e-6);
            assertEquals(0.0, state.angle, 1e-6);
        }
    }
}
```

## Mocking Hardware
To test OpModes or Hardware logic, use a Mocking framework (like Mockito) or create simple interfaces.
- **Don't** try to instantiate `DcMotorImpl` in a unit test.
- **Do** create a `SwerveModule` interface and test the `SwerveDrivetrain` logic using mocks.

## Running Tests
Run the following command in the terminal to execute all unit tests:
```bash
./gradlew test
```
Or right-click the `test/java` directory in Android Studio and select "Run All Tests".
