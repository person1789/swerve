/*
 * Paste this into the existing Juniper MeepMeep preview project.
 *
 * The current robot auto is Kooky-style pose-to-pose driving, not Road Runner path following.
 * This preview intentionally draws straight line segments between the same waypoints the robot
 * scheduler uses so the visual matches the command queue.
 */

import com.noahbres.meepmeep.MeepMeep;
import com.noahbres.meepmeep.core.colorscheme.scheme.ColorSchemeBlueDark;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class KookyAutoMeepMeepPreview {
    private static final double CLOSE_START_X = 120.0;
    private static final double CLOSE_START_Y = 127.87;
    private static final double CLOSE_START_HEADING = Math.toRadians(319.6);
    private static final double CLOSE_SCORE_X = 86.720;
    private static final double CLOSE_SCORE_Y = 90.0;
    private static final double CLOSE_SCORE_HEADING = 0.0;

    private static final double FAR_START_X = 89.0;
    private static final double FAR_START_Y = 8.0;
    private static final double FAR_START_HEADING = 0.0;
    private static final double FAR_SCORE_X = 105.0;
    private static final double FAR_SCORE_Y = 30.5;
    private static final double FAR_SCORE_HEADING = 0.0;

    public static void main(String[] args) {
        MeepMeep meepMeep = new MeepMeep(800);

        RoadRunnerBotEntity closeRoute = new DefaultBotBuilder(meepMeep)
                .setColorScheme(new ColorSchemeBlueDark())
                .setConstraints(52.95, 52.95, Math.toRadians(180), Math.toRadians(180), 9.63)
                .build();

        closeRoute.runAction(closeRoute.getDrive().actionBuilder(
                        new com.acmerobotics.roadrunner.Pose2d(CLOSE_START_X, CLOSE_START_Y, CLOSE_START_HEADING))
                .strafeToLinearHeading(
                        new com.acmerobotics.roadrunner.Vector2d(CLOSE_SCORE_X, CLOSE_SCORE_Y),
                        CLOSE_SCORE_HEADING)
                .build());

        RoadRunnerBotEntity farRoute = new DefaultBotBuilder(meepMeep)
                .setConstraints(52.95, 52.95, Math.toRadians(180), Math.toRadians(180), 9.63)
                .build();

        farRoute.runAction(farRoute.getDrive().actionBuilder(
                        new com.acmerobotics.roadrunner.Pose2d(FAR_START_X, FAR_START_Y, FAR_START_HEADING))
                .strafeToLinearHeading(
                        new com.acmerobotics.roadrunner.Vector2d(FAR_SCORE_X, FAR_SCORE_Y),
                        FAR_SCORE_HEADING)
                .build());

        meepMeep.setBackground(MeepMeep.Background.FIELD_INTO_THE_DEEP_JUICE_DARK)
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(closeRoute)
                .addEntity(farRoute)
                .start();
    }
}
