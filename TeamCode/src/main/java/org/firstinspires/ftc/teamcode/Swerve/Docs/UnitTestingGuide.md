# The Beginner's Guide to Unit Testing for FTC Swerve

Unit testing is like having a "digital robot" in your computer that double-checks your math every time you change a line of code. It allows you to verify that your swerve kinematics and control loops are working perfectly **before** you even turn on the real robot.

---

## 1. What are Unit Tests?
Instead of downloading code to a Control Hub and watching the robot move, a Unit Test runs only a small piece of code (like a single function) on your laptop. 
- **Goal**: Make sure `Input A` always produces `Desired Output B`.
- **Benefit**: You catch math errors in seconds instead of spending hours debugging on the field.

## 2. Setting Up Your Environment
Your repository is already pre-configured for **JUnit 5**, the industry standard for Java testing.

### Requirements:
- **Android Studio**: Ensure you have Android Studio installed.
- **Gradle**: The build system (configured in `build.gradle`) handles the "Runner."

---

## 3. How to Run Your Tests

### Method A: Using Android Studio (Recommended)
1. In the Project pane, navigate to: `TeamCode > src > test > java > org.firstinspires.ftc.teamcode.Swerve.Tests`.
2. Right-click on a file (e.g., `GeometryTest`).
3. Select **"Run 'GeometryTest'"** (with the green play icon).
4. A window will appear at the bottom showing green checkmarks for passed tests and red X's for failed ones.

### Method B: Using the Terminal (Fastest)
Open the terminal at the bottom of Android Studio and type:
```powershell
./gradlew test
```
This will run every single unit test in your project and provide a summary of the results.

---

## 4. Writing Your First Test
A test is just a regular Java function with a `@Test` label on top. We use **Assertions** to check if the code is doing the right thing.

```java
@Test
public void testingAddition() {
    int result = 2 + 2;
    // We "Assert" that the result should be 4
    assertEquals(4, result); 
}
```

## 5. Anatomy of a Swerve Test
For our swerve library, we test the "Logic" without the "Motors." 

1. **Arrange**: Create the controller (e.g., `PIDController`).
2. **Act**: Tell it to calculate something (e.g., `pid.calculate(current, dt)`).
3. **Assert**: Check if the power output is what you expected.

---

## 6. Best Practices
- **Test Edge Cases**: What happens if the robot's heading is exactly 180°? What if the joystick is at (0,0)?
- **Keep Tests Small**: Each test should only check one specific behavior.
- **Run Often**: Run your tests every time you make a change to the core math files.

> [!TIP]
> If a test fails, don't panic! It's doing its job. It found a bug on your laptop so it doesn't break your robot on the field.
