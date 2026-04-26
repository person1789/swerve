package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockCommand;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroBlockRouteBuilder;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroDecodeRoute;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroStartPose;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimPedroTaggedAuto implements SimOpMode {
    private static final Pattern START_CUSTOM_PATTERN = Pattern.compile(
            "Pose\\s+startPose\\s*=\\s*PedroStartPose\\.custom\\(([^,]+),\\s*([^,]+),\\s*([^\\)]+)\\)");
    private static final Pattern START_PRESET_PATTERN = Pattern.compile(
            "Pose\\s+startPose\\s*=\\s*(?:PedroDecodeRoute\\.startPose\\()??PedroStartPose\\.([A-Z0-9_]+)\\)?");
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "PedroBlockCommand\\.(straight|curved)\\(([^\\)]*)\\)");

    private final String className;
    private final String displayName;
    private final Path sourceFile;
    private final TelemetryBundler bundler = new TelemetryBundler();

    private SwerveSimulator sim;
    private Follower follower;
    private SimPedroDrivetrain drivetrain;
    private boolean started;
    private double elapsedSec;

    public SimPedroTaggedAuto(String className, String displayName, Path sourceFile) {
        this.className = className;
        this.displayName = displayName;
        this.sourceFile = sourceFile;
    }

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        this.drivetrain = new SimPedroDrivetrain(sim);
        SimPedroLocalizer localizer = new SimPedroLocalizer(sim);
        this.follower = new Follower(PedroSwerveFactory.createFollowerConstants(), localizer, drivetrain);
        PedroSwerveFactory.applyLiveTuning(follower);

        try {
            SimAutoDefinition definition = loadDefinition(follower);
            Pose startPose = definition.startPose;
            PathChain routine = definition.pathChain;

            sim.setPoseInches(new org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose(
                    startPose.getX(),
                    startPose.getY(),
                    startPose.getHeading()));
            follower.setStartingPose(startPose);
            follower.setPose(startPose);
            follower.followPath(routine, true);

            started = true;
            elapsedSec = 0.0;
            bundler.clear();
            bundler.add("Status", "Status", "Initialized");
            bundler.add("Status", "OpMode", displayName);
        } catch (Exception e) {
            bundler.clear();
            bundler.add("Status", "Status", "Init Failed");
            bundler.add("Status", "OpMode", displayName);
            bundler.add("Status", "Error", e.getClass().getSimpleName());
            bundler.add("Status", "Message", e.getMessage() == null ? "" : e.getMessage());
            started = false;
        }
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        if (!started) {
            return;
        }

        elapsedSec += dt;
        PedroSwerveFactory.applyLiveTuning(follower);
        follower.update();
        sim.step(dt);

        Pose pose = follower.getPose();
        bundler.clear();
        bundler.add("Status", "OpMode", displayName);
        bundler.add("Status", "Status", follower.isBusy() ? "Running" : "Complete");
        bundler.add("Status", "Elapsed", String.format("%.1fs", elapsedSec));
        bundler.add("Pose", "X", String.format("%.2f in", pose.getX()));
        bundler.add("Pose", "Y", String.format("%.2f in", pose.getY()));
        bundler.add("Pose", "Heading", String.format("%.1f deg", Math.toDegrees(pose.getHeading())));
        bundler.add("Path", "Distance Remaining", String.format("%.2f", follower.getDistanceRemaining()));
        bundler.add("Path", "At Parametric End", String.valueOf(follower.atParametricEnd()));
        bundler.add("Drive", "Busy", String.valueOf(follower.isBusy()));
        bundler.add("Drive", "Debug", drivetrain.debugString());
    }

    @Override
    public void stop() {
        if (drivetrain != null) {
            drivetrain.breakFollowing();
        }
        bundler.clear();
        bundler.add("Status", "Status", "Stopped");
        bundler.add("Status", "OpMode", displayName);
        started = false;
        elapsedSec = 0.0;
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }

    public static String readDisplayName(String className, String fallback) {
        try {
            Class<?> autoClass = Class.forName(className);
            Field field = autoClass.getField("SIM_OPMODE_NAME");
            Object value = field.get(null);
            return value instanceof String ? (String) value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private SimAutoDefinition loadDefinition(Follower follower) throws Exception {
        try {
            Class<?> autoClass = Class.forName(className);
            Method startPoseMethod = autoClass.getMethod("buildSimStartPose");
            Method routeMethod = autoClass.getMethod("buildSimPath", Follower.class, Pose.class);
            Pose startPose = (Pose) startPoseMethod.invoke(null);
            PathChain routine = (PathChain) routeMethod.invoke(null, follower, startPose);
            return new SimAutoDefinition(startPose, routine);
        } catch (ClassNotFoundException ignored) {
            return loadDefinitionFromSource(follower);
        }
    }

    private SimAutoDefinition loadDefinitionFromSource(Follower follower) throws Exception {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IllegalStateException("Generated source file not found for " + displayName);
        }

        String source = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        Pose startPose = parseStartPose(source);
        PedroBlockCommand[] commands = parseCommands(source);
        if (commands.length == 0) {
            throw new IllegalStateException("No PedroBlockCommand entries found in " + displayName);
        }

        PathChain pathChain = PedroBlockRouteBuilder.build(follower, startPose, commands);
        return new SimAutoDefinition(startPose, pathChain);
    }

    private static Pose parseStartPose(String source) {
        Matcher custom = START_CUSTOM_PATTERN.matcher(source);
        if (custom.find()) {
            return PedroStartPose.custom(
                    parseDouble(custom.group(1)),
                    parseDouble(custom.group(2)),
                    parseDouble(custom.group(3)));
        }

        Matcher preset = START_PRESET_PATTERN.matcher(source);
        if (preset.find()) {
            PedroStartPose startPose = PedroStartPose.fromName(preset.group(1), PedroStartPose.RED_BASE_CORNER);
            return PedroDecodeRoute.startPose(startPose);
        }

        throw new IllegalStateException("Could not parse start pose from generated auto source");
    }

    private static PedroBlockCommand[] parseCommands(String source) {
        List<PedroBlockCommand> commands = new ArrayList<>();
        Matcher commandMatcher = COMMAND_PATTERN.matcher(source);
        while (commandMatcher.find()) {
            String type = commandMatcher.group(1);
            String args = commandMatcher.group(2);
            if ("straight".equals(type)) {
                double[] values = parseArgumentList(args, 4);
                commands.add(PedroBlockCommand.straight(values[0], values[1], values[2], values[3]));
            } else {
                double[] values = parseArgumentList(args, 8);
                commands.add(PedroBlockCommand.curved(
                        values[0], values[1], values[2],
                        values[3], values[4], values[5], values[6], values[7]));
            }
        }

        return commands.toArray(new PedroBlockCommand[0]);
    }

    private static double[] parseArgumentList(String text, int expectedCount) {
        String[] rawParts = text.split(",");
        if (rawParts.length != expectedCount) {
            throw new IllegalStateException("Expected " + expectedCount + " arguments but found " + rawParts.length);
        }

        double[] values = new double[expectedCount];
        for (int i = 0; i < expectedCount; i++) {
            values[i] = parseDouble(rawParts[i]);
        }
        return values;
    }

    private static double parseDouble(String text) {
        return Double.parseDouble(text.trim());
    }

    private static final class SimAutoDefinition {
        final Pose startPose;
        final PathChain pathChain;

        SimAutoDefinition(Pose startPose, PathChain pathChain) {
            this.startPose = startPose;
            this.pathChain = pathChain;
        }
    }
}
