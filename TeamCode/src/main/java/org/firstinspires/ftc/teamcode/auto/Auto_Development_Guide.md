# Swerve Autonomous Development & Tuning Guide

This document outlines the procedure for tuning the autonomous PID variables, provides a template for building custom autonomous OpModes, and details the internal architecture of the autonomous execution pipeline.

---

## 1. How to Tune the Auto PID Variables

Tuning should be done incrementally by isolating variables. Use the `AutoTuner` OpMode and the FTC Dashboard to adjust parameters live.

### Tuning Translation P
**Objective:** Achieve rapid, direct acceleration toward the target coordinate.
1. In Dashboard, set `TUNE_MODE` to `TRANSLATION_P`.
2. Ensure `AUTO_X_D` and `AUTO_Y_D` are set to `0.0`.
3. Start the OpMode. The robot will attempt to drive 48 inches forward and then return.
4. Increase `AUTO_X_P` until the robot accelerates quickly and reaches the target. If significant oscillation occurs, lower the value.
5. Swap the target axes (X=0, Y=48) and repeat the process for `AUTO_Y_P`.

### Tuning Translation D
**Objective:** Dampen velocity near the target to prevent overshoot.
1. Set `TUNE_MODE` to `TRANSLATION_D`.
2. Start the OpMode. The robot will likely overshoot the target due to the aggressive P gain.
3. Gradually increase `AUTO_X_D`. The robot will begin decelerating earlier.
4. Continue increasing `AUTO_X_D` until overshoot is eliminated and the robot stops precisely at the target. Excessive D gain will cause stuttering.
5. Repeat for the Y axis.

### Tuning Heading P and D
**Objective:** Achieve precise, stable rotation to a target angle.
1. Set `TUNE_MODE` to `HEADING`.
2. Start the OpMode. The robot will rotate 90 degrees back and forth.
3. Set `AUTO_HEADING_D` to 0. 
4. Increase `AUTO_HEADING_P` until the rotation is responsive. 
5. Increase `AUTO_HEADING_D` to dampen any over-rotation or oscillation at the end of the turn.

---

## 2. Building an Auto OpMode from Scratch

Below is a standardized template for creating custom autonomous routines without modifying `AutoRoute` or `SwerveConfig` presets.

```java
package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.auto.DriveContext;
import org.firstinspires.ftc.teamcode.auto.DriveScheduler;
import org.firstinspires.ftc.teamcode.auto.PinpointLocalizer;
import org.firstinspires.ftc.teamcode.auto.StopDriveCommand;

@Autonomous(name = "Custom Auto Template", group = "Auto")
public class CustomAutoTemplate extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {
        
        // 1. Initialize Hardware Interfaces
        HWMap hwMap = new HWMap(hardwareMap);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap);
        PinpointLocalizer localizer = new PinpointLocalizer(hwMap);
        
        // 2. Set Initial Pose (X, Y, Heading in Radians)
        localizer.setPose(24.0, 0.0, Math.toRadians(90.0));
        
        // 3. Initialize Context and Scheduler
        DriveContext context = new DriveContext(drivetrain, localizer);
        DriveScheduler scheduler = new DriveScheduler(10); 
        
        // 4. Define the Path
        // The boolean parameter dictates whether the robot should wait for the steering
        // modules to reach their target azimuth before applying drive power.
        scheduler.addMoveToPose(48.0, 24.0, Math.toRadians(90.0), true);
        scheduler.addMoveToPose(48.0, -24.0, Math.toRadians(0.0), false);
        scheduler.addMoveToPose(60.0, -24.0, Math.toRadians(0.0), false);
        
        // Append a stop command to lock the drivetrain at the final position
        scheduler.add(new StopDriveCommand());

        ElapsedTime timer = new ElapsedTime();

        // 5. Pre-Start Loop: Read sensors and pre-align steering modules
        while (!isStarted() && !isStopRequested()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            double dt = Math.max(1e-3, timer.seconds());
            timer.reset();
            
            drivetrain.pointModulesForCommand(1.0, 0.0, 0.0, dt);
            drivetrain.write(dt);
        }

        if (!opModeIsActive()) return;
        timer.reset();

        // 6. Active Execution Loop
        while (opModeIsActive() && !scheduler.isFinished()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            
            double dt = Math.max(1e-3, timer.seconds());
            timer.reset();

            // Execute path logic
            scheduler.tick(context, dt);
            
            // Output to hardware
            drivetrain.write(dt);
        }
    }
}
```

---

## 3. Internal Architecture: Scheduler to Hardware

This section details the execution pipeline for how autonomous paths are calculated and translated into physical motor commands.

### Step 1: The Scheduler Loop (`DriveScheduler.java`)
The scheduler is responsible for iterating through the queued sequence of `DriveCommand`s. On every tick, it executes the current command and checks if its completion criteria are met.

```java
DriveCommand current = commands[index];
if (!currentInitialized) {
    current.init(context);
    currentInitialized = true;
}

current.tick(context, dt);

if (current.isFinished(context)) {
    current.end(context, false);
    index++;
    currentInitialized = false;
}
```

### Step 2: Translating Error to Power (`MoveToPoseCommand.java` & `AutoMath.java`)
`MoveToPoseCommand` does not use pre-calculated motion profiles. It continuously calculates the difference between the robot's current absolute field position and the target waypoint. It passes these deltas to `AutoMath.calculateKookyPowers()`, which applies three independent PID controllers.

```java
double deltaX = targetX - pose.getXInches();
double deltaY = targetY - pose.getYInches();
double headingError = MathUtil.angleError(pose.getHeadingRadians(), targetHeading);

double xPower = xController.calculateFromError(deltaX, dt);
double yPower = yController.calculateFromError(deltaY, dt);
double headingPower = headingController.calculateFromError(headingError, dt);
```

### Step 3: Field-Centric to Robot-Centric Transformation (`AutoMath.java`)
Because the swerve modules act relative to the robot's chassis, the field-centric PID outputs must be converted into robot-centric commands using a 2D rotation matrix inverse to the robot's heading. The resulting vectors are clamped to the maximum allowed physical powers.

```java
private static void rotateAndClamp(double robotHeading, double fieldXPower, double fieldYPower, double headingPower, double[] output) {
    double xRotated = fieldXPower * Math.cos(robotHeading) - fieldYPower * Math.sin(robotHeading);
    double yRotated = fieldXPower * Math.sin(robotHeading) + fieldYPower * Math.cos(robotHeading);
    
    double xClamped = Range.clip(-xRotated, -SwerveConfig.AUTO_MAX_TRANSLATION_POWER, SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
    double yClamped = Range.clip(-yRotated, -SwerveConfig.AUTO_MAX_TRANSLATION_POWER, SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
    double headingClamped = Range.clip(headingPower, -SwerveConfig.AUTO_MAX_TURN_POWER, SwerveConfig.AUTO_MAX_TURN_POWER);
    
    // ... deadband application
    output[0] = -yClamped;
    output[1] = xClamped;
    output[2] = -headingClamped;
}
```

### Step 4: Inverse Kinematics (`SwerveKinematics.java`)
`SwerveDrivetrain` receives the `[Vx, Vy, Omega]` vector and passes it to `SwerveKinematics.inverseKinematics()`. This function applies a second-order discretization to account for curvilinear motion arcs, then distributes the specific target speed and azimuth to each of the four modules based on their physical distance from the center of the robot.

```java
// Second-order curvilinear offset
double angleRad = omega * dt;
double sin = Math.sin(angleRad);
double cos = Math.cos(angleRad);
double s = sin / angleRad;
double c = (1.0 - cos) / angleRad;
chassisVx = vx * s - vy * c;
chassisVy = vx * c + vy * s;

// Resolution for individual modules
for (int i = 0; i < 4; i++) {
    Vector offset = moduleOffsets[i];
    double moduleVx = chassisVx - omega * offset.y();
    double moduleVy = chassisVy + omega * offset.x();
    states[i].speedMetersPerSecond = Math.hypot(moduleVx, moduleVy); // Magnitude
    states[i].angleRadians = Math.atan2(moduleVy, moduleVx);         // Azimuth
}
```

### Step 5: Drivetrain Desaturation (`SwerveDrivetrain.java`)
If the kinematics equation requests a motor speed that exceeds the physical limitations of the hardware, capping it directly would distort the geometric trajectory of the robot. Instead, `desaturate()` scales the commanded speeds of all four modules down by an identical ratio, ensuring the robot travels the exact same path, just at the maximum physically possible speed.

```java
private static void desaturate(SwerveModuleState[] states) {
    double max = 0.0;
    for (SwerveModuleState state : states) {
        max = Math.max(max, Math.abs(state.speedMetersPerSecond));
    }

    double maxAllowed = SwerveConfig.getMaxLinearSpeedMPS();
    if (max <= maxAllowed || max < 1e-9) {
        return;
    }

    double scale = maxAllowed / max;
    for (SwerveModuleState state : states) {
        state.speedMetersPerSecond *= scale;
    }
}
```

### Step 6: Hardware Execution (`SwerveModule.java`)
The target `[Speed, Angle]` is passed to the individual `SwerveModule` objects. The module calculates the shortest rotational path (flipping the wheel 180 degrees and reversing drive polarity if the required turn is > 90 degrees). It then uses a dedicated PID loop to command the steering CRServo based on feedback from the absolute analog encoder, while applying the drive speed directly to the motor.

```java
public void update(double targetAngleRadians, double targetSpeedMps, double dt) {
    double optimizedAngle = MathUtil.normalizeAngle(targetAngleRadians);
    double optimizedSpeed = targetSpeedMps;
    double error = MathUtil.angleError(currentRotationRadians, optimizedAngle);

    // Module flip optimization
    if (Math.abs(error) > SwerveConfig.FLIP_THRESHOLD) {
        optimizedAngle = MathUtil.normalizeAngle(optimizedAngle + Math.PI);
        optimizedSpeed *= -1.0;
        error = MathUtil.angleError(currentRotationRadians, optimizedAngle);
    }

    // Steering PID Calculation
    rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
    rotationController.setSetpoint(0.0);
    double steerPower = Math.abs(error) < SwerveConfig.STEER_DEADBAND_RAD ? 0.0 
        : Range.clip(rotationController.calculate(-error, Math.max(1e-3, dt)), -1.0, 1.0);
        
    // Drive Power Calculation
    double drivePower = Range.clip(targetSpeedMpsToPower(optimizedSpeed), -1.0, 1.0);

    // Hardware write
    hardware.setSteerPower(steerPower);
    hardware.setDrivePower(drivePower);
}
```
