package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.config.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Config
public class RobotSettings {
    private static final double FIELD_SIZE_IN = 144.0;
    private static final double RED_GOAL_X_IN = 131.0;
    private static final double RED_GOAL_Y_IN = 128.0;
    private static final double RED_GOAL_HEADING_DEG = 136.4;
    private static final double CLOSE_RED_X_IN = 120.0;
    private static final double CLOSE_RED_Y_IN = 127.87;
    private static final double CLOSE_RED_HEADING_DEG = 319.6;
    private static final double FAR_RED_X_IN = 89.0;
    private static final double FAR_RED_Y_IN = 8.0;
    private static final double FAR_RED_HEADING_DEG = 0.0;

    public enum Alliance{
        RED(poseInches(RED_GOAL_X_IN, RED_GOAL_Y_IN, RED_GOAL_HEADING_DEG)),
        BLUE(mirrorAcrossFieldY(RED_GOAL_X_IN, RED_GOAL_Y_IN, RED_GOAL_HEADING_DEG));

        private final Pose2D pose2D;

        Alliance(Pose2D pos) {
            this.pose2D = pos;
        }

        public Pose2D getGoalPos() {
            return pose2D;
        }
    }

    public enum StartPos{
        CLOSE_RED(poseInches(CLOSE_RED_X_IN, CLOSE_RED_Y_IN, CLOSE_RED_HEADING_DEG)),
        FAR_RED(poseInches(FAR_RED_X_IN, FAR_RED_Y_IN, FAR_RED_HEADING_DEG)),
        CLOSE_BLUE(mirrorAcrossFieldY(CLOSE_RED_X_IN, CLOSE_RED_Y_IN, CLOSE_RED_HEADING_DEG)),
        FAR_BLUE(mirrorAcrossFieldY(FAR_RED_X_IN, FAR_RED_Y_IN, FAR_RED_HEADING_DEG));

        private final Pose2D pose2D;

        StartPos(Pose2D pos) {
            this.pose2D = pos;
        }

        public Pose2D getPose2D() {
            return pose2D;
        }
    }

    public Alliance alliance;
    public StartPos startPosState;

    private static final String FILENAME = "RobotSettings.json";

    // Writes file
    public void save() {
        File file = AppUtil.getInstance().getSettingsFile(FILENAME);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // reads file and loads into program
    // TODO: add a safety condition here it self so no null object refs
    public static RobotSettings load() {
        File file = AppUtil.getInstance().getSettingsFile(FILENAME);
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            RobotSettings loaded = gson.fromJson(reader, RobotSettings.class);
            return sanitize(loaded);
        } catch (IOException e) {
            // If file not found or error, return default settings
            return new RobotSettings();
        }
    }

    public RobotSettings () {
        alliance = Alliance.RED;
        startPosState = StartPos.CLOSE_RED;
    }

    private static Pose2D poseInches(double x, double y, double headingDeg) {
        return new Pose2D(DistanceUnit.INCH, x, y, AngleUnit.DEGREES, headingDeg);
    }

    private static Pose2D mirrorAcrossFieldY(double x, double y, double headingDeg) {
        return poseInches(x, FIELD_SIZE_IN - y, mirrorHeadingDegrees(headingDeg));
    }

    private static double mirrorHeadingDegrees(double headingDeg) {
        double mirrored = -headingDeg;
        while (mirrored <= -180.0) {
            mirrored += 360.0;
        }
        while (mirrored > 180.0) {
            mirrored -= 360.0;
        }
        return mirrored;
    }

    private static RobotSettings sanitize(RobotSettings loaded) {
        RobotSettings safe = loaded != null ? loaded : new RobotSettings();
        if (safe.alliance == null) {
            safe.alliance = Alliance.RED;
        }
        if (safe.startPosState == null) {
            safe.startPosState = StartPos.CLOSE_RED;
        }
        return safe;
    }

}
