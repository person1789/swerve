package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.LinkedHashMap;
import java.util.Map;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;

/**
 * Browser-sim approximation of the DECODE lane autonomous.
 * Uses simple waypoint tracking against the simulator's field-centric teleop path.
 */
public class SimDecodeLaneAuto implements SimOpMode {
    private static final double POS_KP = 0.065;
    private static final double TURN_KP = 1.6;
    private static final double MAX_TRANSLATION_CMD = 0.85;
    private static final double MAX_TURN_CMD = 0.60;
    private static final double POSITION_TOLERANCE_M = 0.09;
    private static final double HEADING_TOLERANCE_RAD = Math.toRadians(7.0);

    private final TelemetryBundler bundler = new TelemetryBundler();
    private final Waypoint[] waypoints = new Waypoint[] {
            new Waypoint(inToM(-30.0), inToM(-60.0), Math.toRadians(0.0), "Leave BASE lane"),
            new Waypoint(inToM(18.0), inToM(-28.0), Math.toRadians(32.0), "Approach GOAL side"),
            new Waypoint(inToM(28.0), inToM(-6.0), Math.toRadians(90.0), "Pass CLASSIFIER side"),
            new Waypoint(inToM(-52.0), inToM(-44.0), Math.toRadians(180.0), "Return to BASE")
    };

    private SwerveSimulator sim;
    private int activeWaypoint = 0;
    private double elapsedSec = 0.0;
    private boolean complete = false;

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        this.activeWaypoint = 0;
        this.elapsedSec = 0.0;
        this.complete = false;
        bundler.clear();
        bundler.add("Status", "Status", "Initialized");
        bundler.add("Status", "OpMode", "DECODE Lane Auto");
        bundler.add("Path", "Waypoint", waypoints[0].label);
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        elapsedSec += dt;
        Map<String, Object> snapshot = sim.snapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> pose = (Map<String, Object>) snapshot.get("pose");

        double x = ((Number) pose.get("xMeters")).doubleValue();
        double y = ((Number) pose.get("yMeters")).doubleValue();
        double heading = ((Number) pose.get("headingRadians")).doubleValue();

        BrowserGamepadState synth = new BrowserGamepadState();

        if (!complete) {
            Waypoint waypoint = waypoints[activeWaypoint];
            double errorX = waypoint.xMeters - x;
            double errorY = waypoint.yMeters - y;
            double distance = Math.hypot(errorX, errorY);
            double headingError = MathUtil.angleError(heading, waypoint.headingRad);

            if (distance < POSITION_TOLERANCE_M && Math.abs(headingError) < HEADING_TOLERANCE_RAD) {
                activeWaypoint++;
                if (activeWaypoint >= waypoints.length) {
                    complete = true;
                } else {
                    waypoint = waypoints[activeWaypoint];
                    errorX = waypoint.xMeters - x;
                    errorY = waypoint.yMeters - y;
                    distance = Math.hypot(errorX, errorY);
                    headingError = MathUtil.angleError(heading, waypoint.headingRad);
                }
            }

            if (!complete) {
                synth.leftY = clamp(-errorX * POS_KP, -MAX_TRANSLATION_CMD, MAX_TRANSLATION_CMD);
                synth.leftX = clamp(-errorY * POS_KP, -MAX_TRANSLATION_CMD, MAX_TRANSLATION_CMD);
                synth.rightX = clamp(-headingError * TURN_KP, -MAX_TURN_CMD, MAX_TURN_CMD);
            }
        }

        sim.updateInput(synth);
        sim.step(dt);

        bundler.clear();
        bundler.add("Status", "OpMode", "DECODE Lane Auto");
        bundler.add("Status", "Status", complete ? "Complete" : "Running");
        bundler.add("Status", "Elapsed", String.format("%.1fs", elapsedSec));
        bundler.add("Path", "Waypoint #", String.valueOf(Math.min(activeWaypoint + 1, waypoints.length)));
        bundler.add("Path", "Waypoint", complete ? "Done" : waypoints[activeWaypoint].label);
        bundler.add("Pose", "X", String.format("%.2f m", x));
        bundler.add("Pose", "Y", String.format("%.2f m", y));
        bundler.add("Pose", "Heading", String.format("%.1f deg", Math.toDegrees(heading)));
        bundler.add("Drivetrain", "State", String.valueOf(snapshot.get("drivetrainState")));
    }

    @Override
    public void stop() {
        bundler.clear();
        bundler.add("Status", "Status", "Stopped");
        activeWaypoint = 0;
        elapsedSec = 0.0;
        complete = false;
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double inToM(double inches) {
        return inches * 0.0254;
    }

    private static class Waypoint {
        final double xMeters;
        final double yMeters;
        final double headingRad;
        final String label;

        Waypoint(double xMeters, double yMeters, double headingRad, String label) {
            this.xMeters = xMeters;
            this.yMeters = yMeters;
            this.headingRad = headingRad;
            this.label = label;
        }
    }
}
