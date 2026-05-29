// Paste this inside MiniDriveAuto after drivetrain/localizer creation.
// Replace each waypoint with the straight-line points from Pedro Visualizer.

localizer.setPose(120.0, 127.87, Math.toRadians(319.6));

DriveScheduler scheduler = new DriveScheduler(5)
        .addMoveToPose(86.72, 90.0, Math.toRadians(0.0), true, 0.0, 2500.0)
        .addMoveToPose(105.0, 84.0, Math.toRadians(0.0), true, 0.0, 2500.0)
        .add(new StopDriveCommand());
