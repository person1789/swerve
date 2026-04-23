package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;

class SwerveSimulator {
    private static final double TRACK_WIDTH_METERS = SwerveConfig.TRACK_WIDTH_IN * 0.0254;
    private static final double WHEEL_BASE_METERS = SwerveConfig.WHEEL_BASE_IN * 0.0254;

    private final MockSwerveModuleIO[] moduleIo;
    private final SwerveModule[] modules;
    private final SwerveDrivetrain drivetrain;
    private final SwerveController controller;
    private final SwerveKinematics kinematics;

    private final BrowserGamepadState gamepadState = new BrowserGamepadState();
    private Vector pose = new Vector(0.0, 0.0, 0.0);
    private Vector actualVelocity = new Vector(0.0, 0.0, 0.0);
    private boolean lastResetHeading;
    private String activeSnap = "none";

    SwerveSimulator() {
        double halfLength = WHEEL_BASE_METERS / 2.0;
        double halfWidth = TRACK_WIDTH_METERS / 2.0;

        moduleIo = new MockSwerveModuleIO[] {
                new MockSwerveModuleIO("Front Left", halfLength, halfWidth),
                new MockSwerveModuleIO("Front Right", halfLength, -halfWidth),
                new MockSwerveModuleIO("Back Right", -halfLength, -halfWidth),
                new MockSwerveModuleIO("Back Left", -halfLength, halfWidth)
        };

        modules = new SwerveModule[] {
                new SwerveModule(moduleIo[0], SwerveConfig.OFFSETS[0], SwerveConfig.INVERSIONS[0], null),
                new SwerveModule(moduleIo[1], SwerveConfig.OFFSETS[1], SwerveConfig.INVERSIONS[1], null),
                new SwerveModule(moduleIo[2], SwerveConfig.OFFSETS[2], SwerveConfig.INVERSIONS[2], null),
                new SwerveModule(moduleIo[3], SwerveConfig.OFFSETS[3], SwerveConfig.INVERSIONS[3], null)
        };

        drivetrain = new SwerveDrivetrain(modules, () -> 12.4, null);
        controller = new SwerveController();
        kinematics = new SwerveKinematics();
    }

    synchronized void updateInput(BrowserGamepadState incoming) {
        if (incoming == null) {
            return;
        }
        gamepadState.leftX = incoming.leftX;
        gamepadState.leftY = incoming.leftY;
        gamepadState.rightX = incoming.rightX;
        gamepadState.dpadUp = incoming.dpadUp;
        gamepadState.dpadRight = incoming.dpadRight;
        gamepadState.dpadDown = incoming.dpadDown;
        gamepadState.dpadLeft = incoming.dpadLeft;
        gamepadState.resetHeading = incoming.resetHeading;
        gamepadState.connected = incoming.connected;
    }

    synchronized void step(double dtSeconds) {
        applySnapInput();
        applyHeadingReset();

        double heading = pose.omega();
        double vx = -gamepadState.leftY;
        double vy = -gamepadState.leftX;
        double turn = -gamepadState.rightX;

        double cos = Math.cos(-heading);
        double sin = Math.sin(-heading);
        double rawTranslationX = vx * cos - vy * sin;
        double rawTranslationY = vx * sin + vy * cos;

        Vector chassisSpeeds = controller.update(rawTranslationX, rawTranslationY, turn, heading, dtSeconds);
        drivetrain.setVelocity(chassisSpeeds, dtSeconds);

        for (MockSwerveModuleIO io : moduleIo) {
            io.step(dtSeconds);
        }

        actualVelocity = currentVelocityFromModules();
        Vector worldVelocity = new Vector(actualVelocity.x(), actualVelocity.y()).rotate(heading);
        pose = new Vector(
                pose.x() + worldVelocity.x() * dtSeconds,
                pose.y() + worldVelocity.y() * dtSeconds,
                MathUtil.normalizeAngle(heading + actualVelocity.omega() * dtSeconds));
    }

    synchronized Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("pose", poseMap());
        state.put("drivetrainState", drivetrain.getState().name());
        state.put("headingHold", controller.isMaintaining());
        state.put("snapping", controller.isSnapping());
        state.put("snapTargetRadians", controller.getTargetHeading());
        state.put("activeSnap", activeSnap);
        state.put("gamepadConnected", gamepadState.connected);
        state.put("actualVelocity", velocityMap(actualVelocity));
        state.put("modules", moduleMaps());
        return state;
    }

    private void applySnapInput() {
        if (gamepadState.dpadUp) {
            controller.setSnapTarget(0.0);
            activeSnap = "up";
        } else if (gamepadState.dpadRight) {
            controller.setSnapTarget(-Math.PI / 2.0);
            activeSnap = "right";
        } else if (gamepadState.dpadDown) {
            controller.setSnapTarget(Math.PI);
            activeSnap = "down";
        } else if (gamepadState.dpadLeft) {
            controller.setSnapTarget(Math.PI / 2.0);
            activeSnap = "left";
        } else {
            activeSnap = "none";
        }
    }

    private void applyHeadingReset() {
        if (gamepadState.resetHeading && !lastResetHeading) {
            pose = new Vector(pose.x(), pose.y(), 0.0);
            controller.resetHeading(0.0);
        }
        lastResetHeading = gamepadState.resetHeading;
    }

    private Map<String, Object> poseMap() {
        Map<String, Object> poseMap = new LinkedHashMap<>();
        poseMap.put("xMeters", pose.x());
        poseMap.put("yMeters", pose.y());
        poseMap.put("headingRadians", pose.omega());
        poseMap.put("headingDegrees", Math.toDegrees(pose.omega()));
        return poseMap;
    }

    private Map<String, Object> velocityMap(Vector velocity) {
        Map<String, Object> velocityMap = new LinkedHashMap<>();
        velocityMap.put("x", velocity.x());
        velocityMap.put("y", velocity.y());
        velocityMap.put("omega", velocity.omega());
        velocityMap.put("linearSpeed", Math.hypot(velocity.x(), velocity.y()));
        return velocityMap;
    }

    private Vector currentVelocityFromModules() {
        SwerveModuleState[] currentStates = new SwerveModuleState[modules.length];
        for (int i = 0; i < modules.length; i++) {
            currentStates[i] = modules[i].getCurrentState();
        }
        return kinematics.forwardKinematics(currentStates);
    }

    private List<Map<String, Object>> moduleMaps() {
        List<Map<String, Object>> modulesState = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            SwerveModule module = modules[i];
            MockSwerveModuleIO io = moduleIo[i];
            Map<String, Object> moduleMap = new LinkedHashMap<>();
            moduleMap.put("name", io.getName());
            moduleMap.put("xMeters", io.getModuleX());
            moduleMap.put("yMeters", io.getModuleY());
            moduleMap.put("currentAngleRadians", module.getCurrentRotation());
            moduleMap.put("currentAngleDegrees", Math.toDegrees(module.getCurrentRotation()));
            moduleMap.put("targetAngleRadians", module.getLastTargetAngleRad());
            moduleMap.put("targetAngleDegrees", Math.toDegrees(module.getLastTargetAngleRad()));
            moduleMap.put("actualVelocityMps", module.getVelocityMps());
            moduleMap.put("targetVelocityMps", module.getLastTargetVelocityMps());
            moduleMap.put("drivePower", io.getDrivePower());
            moduleMap.put("steerPower", io.getSteerPower());
            moduleMap.put("stalled", module.isStalled());
            modulesState.add(moduleMap);
        }
        return modulesState;
    }
}
