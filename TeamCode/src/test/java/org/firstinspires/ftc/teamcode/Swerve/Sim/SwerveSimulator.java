package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;

class SwerveSimulator {
    private static final double TRACK_WIDTH_METERS = SwerveConfig.TRACK_WIDTH_IN * 0.0254;
    private static final double WHEEL_BASE_METERS = SwerveConfig.WHEEL_BASE_IN * 0.0254;

    private final MockSwerveModuleIO[] moduleIo;
    private final SwerveModule[] modules;
    private final SwerveDrivetrain drivetrain;
    private final SwerveKinematics kinematics;
    private final SwerveModuleState[] moduleStateCache = {
            new SwerveModuleState(),
            new SwerveModuleState(),
            new SwerveModuleState(),
            new SwerveModuleState()
    };
    private final double[] chassisVelocity = new double[3];
    private final double[] command = new double[3];

    private final BrowserGamepadState gamepadState = new BrowserGamepadState();
    private Vector pose = new Vector(0.0, 0.0, 0.0);
    private Vector actualVelocity = new Vector(0.0, 0.0, 0.0);
    private String activeSnap = "none";
    private boolean directDriveEnabled;
    private Vector directDriveCommand = new Vector(0.0, 0.0, 0.0);
    private double snapTargetRadians = 0.0;

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

    synchronized BrowserGamepadState getGamepadState() {
        return gamepadState;
    }

    synchronized void step(double dtSeconds) {
        if (directDriveEnabled) {
            drivetrain.refreshSensors();
            drivetrain.setAutonomousVelocity(directDriveCommand, dtSeconds);

            for (MockSwerveModuleIO io : moduleIo) {
                io.step(dtSeconds);
            }
            drivetrain.refreshSensors();

            actualVelocity = currentVelocityFromModules();
            Vector worldVelocity = new Vector(actualVelocity.x(), actualVelocity.y()).rotate(pose.omega());
            pose = new Vector(
                    pose.x() + worldVelocity.x() * dtSeconds,
                    pose.y() + worldVelocity.y() * dtSeconds,
                    MathUtil.normalizeAngle(pose.omega() + actualVelocity.omega() * dtSeconds));
            return;
        }

        applySnapInput();
        applyHeadingReset();

        double heading = pose.omega();
        double vx = -gamepadState.leftY;
        double vy = -gamepadState.leftX;
        double turn = applyRotationIntent(heading, -gamepadState.rightX);
        drivetrain.refreshSensors();
        createRobotRelativeCommand(vx, vy, turn, heading, command);
        drivetrain.setVelocity(command[0], command[1], command[2], dtSeconds);

        for (MockSwerveModuleIO io : moduleIo) {
            io.step(dtSeconds);
        }
        drivetrain.refreshSensors();

        actualVelocity = currentVelocityFromModules();
        Vector worldVelocity = new Vector(actualVelocity.x(), actualVelocity.y()).rotate(heading);
        pose = new Vector(
                pose.x() + worldVelocity.x() * dtSeconds,
                pose.y() + worldVelocity.y() * dtSeconds,
                MathUtil.normalizeAngle(heading + actualVelocity.omega() * dtSeconds));
    }

    synchronized void setDirectDriveCommand(double forward, double strafe, double rotation) {
        directDriveEnabled = true;
        directDriveCommand = new Vector(forward, strafe, rotation);
    }

    synchronized void clearDirectDriveCommand() {
        directDriveEnabled = false;
        directDriveCommand = new Vector(0.0, 0.0, 0.0);
    }

    synchronized Pose getPoseInches() {
        return new Pose(metersToInches(pose.x()), metersToInches(pose.y()), pose.omega());
    }

    synchronized Pose getVelocityInches() {
        return new Pose(metersToInches(actualVelocity.x()), metersToInches(actualVelocity.y()), actualVelocity.omega());
    }

    synchronized void setPoseInches(Pose poseInches) {
        pose = new Vector(inchesToMeters(poseInches.x), inchesToMeters(poseInches.y), poseInches.heading);
        actualVelocity = new Vector(0.0, 0.0, 0.0);
        drivetrain.resetSmoother();
        snapTargetRadians = poseInches.heading;
    }

    synchronized void resetPose() {
        pose = new Vector(0.0, 0.0, 0.0);
        actualVelocity = new Vector(0.0, 0.0, 0.0);
        directDriveEnabled = false;
        directDriveCommand = new Vector(0.0, 0.0, 0.0);
        gamepadState.leftX = 0.0;
        gamepadState.leftY = 0.0;
        gamepadState.rightX = 0.0;
        gamepadState.dpadUp = false;
        gamepadState.dpadRight = false;
        gamepadState.dpadDown = false;
        gamepadState.dpadLeft = false;
        gamepadState.resetHeading = false;
        drivetrain.resetSmoother();
        snapTargetRadians = 0.0;
    }

    synchronized Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("pose", poseMap());
        state.put("drivetrainState", drivetrain.getState().name());
        state.put("headingHold", false);
        state.put("snapping", !"none".equals(activeSnap));
        state.put("snapTargetRadians", snapTargetRadians);
        state.put("activeSnap", activeSnap);
        state.put("gamepadConnected", gamepadState.connected);
        state.put("actualVelocity", velocityMap(actualVelocity));
        state.put("modules", moduleMaps());
        return state;
    }

    private void applySnapInput() {
        if (gamepadState.dpadUp) {
            snapTargetRadians = 0.0;
            activeSnap = "up";
        } else if (gamepadState.dpadRight) {
            snapTargetRadians = -Math.PI / 2.0;
            activeSnap = "right";
        } else if (gamepadState.dpadDown) {
            snapTargetRadians = Math.PI;
            activeSnap = "down";
        } else if (gamepadState.dpadLeft) {
            snapTargetRadians = Math.PI / 2.0;
            activeSnap = "left";
        } else {
            activeSnap = "none";
        }
    }

    private void applyHeadingReset() {
        if (gamepadState.resetHeading) {
            pose = new Vector(pose.x(), pose.y(), 0.0);
            snapTargetRadians = 0.0;
        }
    }

    private double applyRotationIntent(double heading, double manualTurn) {
        if (Math.abs(manualTurn) >= SwerveConfig.INPUT_DEADBAND) {
            return manualTurn;
        }
        if ("none".equals(activeSnap)) {
            return 0.0;
        }
        double error = MathUtil.angleError(heading, snapTargetRadians);
        double turn = error * 0.8;
        return MathUtil.clamp(turn, -1.0, 1.0);
    }

    private void createRobotRelativeCommand(double fieldForward, double fieldStrafe, double turn, double heading, double[] commandOut) {
        double translationMagnitude = Math.hypot(fieldForward, fieldStrafe);
        if (translationMagnitude < SwerveConfig.INPUT_DEADBAND) {
            fieldForward = 0.0;
            fieldStrafe = 0.0;
        } else {
            double scaledMagnitude = (translationMagnitude - SwerveConfig.INPUT_DEADBAND)
                    / (1.0 - SwerveConfig.INPUT_DEADBAND);
            double ratio = scaledMagnitude / translationMagnitude;
            fieldForward *= ratio;
            fieldStrafe *= ratio;
        }

        double cos = Math.cos(-heading);
        double sin = Math.sin(-heading);
        commandOut[0] = fieldForward * cos - fieldStrafe * sin;
        commandOut[1] = fieldForward * sin + fieldStrafe * cos;
        commandOut[2] = turn;
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
        for (int i = 0; i < modules.length; i++) {
            modules[i].copyCurrentStateInto(moduleStateCache[i]);
        }
        kinematics.forwardKinematics(moduleStateCache, chassisVelocity);
        return new Vector(chassisVelocity[0], chassisVelocity[1], chassisVelocity[2]);
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

    private static double metersToInches(double meters) {
        return meters / 0.0254;
    }

    private static double inchesToMeters(double inches) {
        return inches * 0.0254;
    }
}
