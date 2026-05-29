package org.firstinspires.ftc.teamcode.auto;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

public final class DashboardAutoVisualizer {
    private static final double ROBOT_RADIUS_IN = 8.0;
    private static final double HEADING_LINE_IN = 10.0;

    private DashboardAutoVisualizer() {
    }

    public static TelemetryPacket packet(AutoRoute route, AutoPoseProvider pose, String commandName) {
        TelemetryPacket packet = new TelemetryPacket();
        packet.put("autoRoute", route.name);
        packet.put("autoCommand", commandName);
        packet.put("autoX", pose.getXInches());
        packet.put("autoY", pose.getYInches());
        packet.put("autoHeadingDeg", Math.toDegrees(pose.getHeadingRadians()));

        Canvas field = packet.fieldOverlay();
        drawRoute(field, route);
        drawRobot(field, pose.getXInches(), pose.getYInches(), pose.getHeadingRadians());
        return packet;
    }

    private static void drawRoute(Canvas field, AutoRoute route) {
        double prevX = route.startXInches;
        double prevY = route.startYInches;

        field.setStroke("#2b6cb0");
        field.setStrokeWidth(2);
        for (AutoWaypoint waypoint : route.waypoints) {
            field.strokeLine(prevX, prevY, waypoint.xInches, waypoint.yInches);
            prevX = waypoint.xInches;
            prevY = waypoint.yInches;
        }

        field.setFill("#2f855a");
        field.fillCircle(route.startXInches, route.startYInches, 2.5);
        field.setFill("#c53030");
        for (AutoWaypoint waypoint : route.waypoints) {
            field.fillCircle(waypoint.xInches, waypoint.yInches, 2.5);
        }
    }

    private static void drawRobot(Canvas field, double x, double y, double heading) {
        double headingX = x + HEADING_LINE_IN * Math.cos(heading);
        double headingY = y + HEADING_LINE_IN * Math.sin(heading);

        field.setStroke("#1a202c");
        field.setStrokeWidth(2);
        field.strokeCircle(x, y, ROBOT_RADIUS_IN);
        field.strokeLine(x, y, headingX, headingY);
    }
}
