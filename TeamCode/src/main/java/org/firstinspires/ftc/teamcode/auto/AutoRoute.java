package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

public class AutoRoute {
    public static final AutoRoute CLOSE = new AutoRoute(
            "close",
            SwerveConfig.AUTO_CLOSE_START_X_IN,
            SwerveConfig.AUTO_CLOSE_START_Y_IN,
            SwerveConfig.AUTO_CLOSE_START_HEADING_RAD,
            new AutoWaypoint[] {
                    new AutoWaypoint(SwerveConfig.AUTO_CLOSE_SCORE_X_IN,
                            SwerveConfig.AUTO_CLOSE_SCORE_Y_IN,
                            SwerveConfig.AUTO_CLOSE_SCORE_HEADING_RAD,
                            SwerveConfig.AUTO_SETTLE_DELAY_MS,
                            SwerveConfig.AUTO_MOVE_TIMEOUT_MS)
            });

    public static final AutoRoute FAR = new AutoRoute(
            "far",
            SwerveConfig.AUTO_FAR_START_X_IN,
            SwerveConfig.AUTO_FAR_START_Y_IN,
            SwerveConfig.AUTO_FAR_START_HEADING_RAD,
            new AutoWaypoint[] {
                    new AutoWaypoint(SwerveConfig.AUTO_FAR_SCORE_X_IN,
                            SwerveConfig.AUTO_FAR_SCORE_Y_IN,
                            SwerveConfig.AUTO_FAR_SCORE_HEADING_RAD,
                            SwerveConfig.AUTO_SETTLE_DELAY_MS,
                            SwerveConfig.AUTO_MOVE_TIMEOUT_MS)
            });

    public final String name;
    public final double startXInches;
    public final double startYInches;
    public final double startHeadingRadians;
    public final AutoWaypoint[] waypoints;

    private AutoRoute(String name, double startXInches, double startYInches,
            double startHeadingRadians, AutoWaypoint[] waypoints) {
        this.name = name;
        this.startXInches = startXInches;
        this.startYInches = startYInches;
        this.startHeadingRadians = startHeadingRadians;
        this.waypoints = waypoints;
    }

    public static AutoRoute selected(boolean closeRoute) {
        return closeRoute ? CLOSE : FAR;
    }

    public void seedPose(PinpointLocalizer localizer) {
        localizer.setPose(startXInches, startYInches, startHeadingRadians);
    }

    public DriveScheduler buildScheduler(boolean waitForAzimuth) {
        DriveScheduler scheduler = new DriveScheduler(waypoints.length * 2 + 1);
        for (AutoWaypoint waypoint : waypoints) {
            scheduler.addMoveToPose(
                    waypoint.xInches,
                    waypoint.yInches,
                    waypoint.headingRadians,
                    waitForAzimuth,
                    waypoint.settleDelayMs,
                    waypoint.timeoutMs);
        }
        return scheduler.add(new StopDriveCommand());
    }
}
