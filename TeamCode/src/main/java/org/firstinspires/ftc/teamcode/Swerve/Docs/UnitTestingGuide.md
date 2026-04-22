# The Beginner's Guide to Unit Testing for FTC Swerve

Unit testing is like having a "digital twin" in your computer that double-checks your math every time you change a line of code. It allows you to verify that your swerve kinematics and control loops are working perfectly **before** you even turn on the real robot.

---

## 1. Why Unit Test?
Our swerve system operates in **Physical Units** (meters per second, radians per second). Unit tests ensure that:
- A command of $1.0$ m/s North actually calculates the correct wheel speeds.
- The `Vector` rotation math doesn't have "flipped" axes.
- The `MotionSmoother` correctly ramps speed without jumping.

## 2. Setting Up Your Environment
The repository is pre-configured for **JUnit 5**.

### Running Tests:
- **Android Studio**: Right-click on the `test` folder and select **Run 'Tests in Swerve'**.
- **Terminal**: Run `./gradlew test` for a full system validation.

## 3. Testing the Vector Math
Since we replaced legacy types with a unified `Vector` class, it is the most critical part of our test suite.
```java
@Test
public void testVectorRotation() {
    Vector start = new Vector(1, 0); // Pointing North
    Vector rotated = start.rotate(Math.PI / 2); // Rotate 90 degrees
    
    assertEquals(0, rotated.x(), 1e-6);
    assertEquals(1, rotated.y(), 1e-6);
}
```

## 4. The Swerve Pipeline Test
For our swerve library, we test the "Logic" without the "Motors." We simulate a loop and check the output.

1. **Arrange**: Create a `SwerveKinematics` object with your robot's trackwidth.
2. **Act**: Pass a 3D `Vector(1, 0, 0)` into `inverseKinematics`.
3. **Assert**: Verify that all 4 `SwerveModuleState` objects have a speed of $1.0$ m/s and an angle of $0$.

## 5. Testing the "Brain"
You can test the `SwerveController` by simulating a "disturbance":
1. Set a snap target of $0$.
2. Simulate a current heading of $0.1$ radians.
3. Assert that the controller outputs a negative angular velocity to correct the error.

---

## 6. Best Practices
- **Standard Units**: Always write tests in m/s and rad/s.
- **Delta Thresholds**: When comparing doubles, always use a small delta (e.g., `1e-6`) to account for floating-point math.
- **Fail Fast**: If your tests fail on your laptop, **DO NOT** download the code to the robot.

> [!IMPORTANT]
> A green test suite is your "License to Drive." Never test new movement logic on the field without a passing test suite.
