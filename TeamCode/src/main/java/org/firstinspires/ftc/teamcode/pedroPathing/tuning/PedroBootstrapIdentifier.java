package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import java.util.List;

public final class PedroBootstrapIdentifier {
    private PedroBootstrapIdentifier() {
    }

    public enum Axis {
        FORWARD,
        LATERAL,
        TURN
    }

    public static final class MotionSample {
        public final String phaseName;
        public final double timeSec;
        public final double commandForward;
        public final double commandStrafe;
        public final double commandTurn;
        public final double forwardVelocityInS;
        public final double lateralVelocityInS;
        public final double turnVelocityRadS;

        public MotionSample(String phaseName, double timeSec,
                            double commandForward, double commandStrafe, double commandTurn,
                            double forwardVelocityInS, double lateralVelocityInS, double turnVelocityRadS) {
            this.phaseName = phaseName;
            this.timeSec = timeSec;
            this.commandForward = commandForward;
            this.commandStrafe = commandStrafe;
            this.commandTurn = commandTurn;
            this.forwardVelocityInS = forwardVelocityInS;
            this.lateralVelocityInS = lateralVelocityInS;
            this.turnVelocityRadS = turnVelocityRadS;
        }
    }

    public static final class AxisSummary {
        public final Axis axis;
        public final double deadzoneCommand;
        public final double maxVelocity;
        public final double accelRate;
        public final double brakeRate;
        public final double riseTimeSec;

        public AxisSummary(Axis axis, double deadzoneCommand, double maxVelocity,
                           double accelRate, double brakeRate, double riseTimeSec) {
            this.axis = axis;
            this.deadzoneCommand = deadzoneCommand;
            this.maxVelocity = maxVelocity;
            this.accelRate = accelRate;
            this.brakeRate = brakeRate;
            this.riseTimeSec = riseTimeSec;
        }
    }

    public static final class BootstrapSeeds {
        public final double forwardZeroPowerAcceleration;
        public final double lateralZeroPowerAcceleration;
        public final double primaryTranslationP;
        public final double primaryTranslationI;
        public final double primaryTranslationD;
        public final double secondaryTranslationP;
        public final double secondaryTranslationI;
        public final double secondaryTranslationD;
        public final double primaryHeadingP;
        public final double primaryHeadingI;
        public final double primaryHeadingD;
        public final double secondaryHeadingP;
        public final double secondaryHeadingI;
        public final double secondaryHeadingD;
        public final double primaryDriveP;
        public final double primaryDriveI;
        public final double primaryDriveD;
        public final double secondaryDriveP;
        public final double secondaryDriveI;
        public final double secondaryDriveD;
        public final double centripetalScaling;

        public BootstrapSeeds(double forwardZeroPowerAcceleration, double lateralZeroPowerAcceleration,
                              double primaryTranslationP, double primaryTranslationI, double primaryTranslationD,
                              double secondaryTranslationP, double secondaryTranslationI, double secondaryTranslationD,
                              double primaryHeadingP, double primaryHeadingI, double primaryHeadingD,
                              double secondaryHeadingP, double secondaryHeadingI, double secondaryHeadingD,
                              double primaryDriveP, double primaryDriveI, double primaryDriveD,
                              double secondaryDriveP, double secondaryDriveI, double secondaryDriveD,
                              double centripetalScaling) {
            this.forwardZeroPowerAcceleration = forwardZeroPowerAcceleration;
            this.lateralZeroPowerAcceleration = lateralZeroPowerAcceleration;
            this.primaryTranslationP = primaryTranslationP;
            this.primaryTranslationI = primaryTranslationI;
            this.primaryTranslationD = primaryTranslationD;
            this.secondaryTranslationP = secondaryTranslationP;
            this.secondaryTranslationI = secondaryTranslationI;
            this.secondaryTranslationD = secondaryTranslationD;
            this.primaryHeadingP = primaryHeadingP;
            this.primaryHeadingI = primaryHeadingI;
            this.primaryHeadingD = primaryHeadingD;
            this.secondaryHeadingP = secondaryHeadingP;
            this.secondaryHeadingI = secondaryHeadingI;
            this.secondaryHeadingD = secondaryHeadingD;
            this.primaryDriveP = primaryDriveP;
            this.primaryDriveI = primaryDriveI;
            this.primaryDriveD = primaryDriveD;
            this.secondaryDriveP = secondaryDriveP;
            this.secondaryDriveI = secondaryDriveI;
            this.secondaryDriveD = secondaryDriveD;
            this.centripetalScaling = centripetalScaling;
        }
    }

    public static AxisSummary summarize(Axis axis, List<MotionSample> samples) {
        double deadzoneCommand = 0.0;
        double maxVelocity = 0.0;
        double riseTimeSec = 0.0;
        double firstMotionTime = -1.0;
        double firstCommandTime = -1.0;
        double previousVelocity = 0.0;
        double previousTime = 0.0;
        double peakPositiveAccel = 0.0;
        double peakNegativeAccel = 0.0;

        for (int i = 0; i < samples.size(); i++) {
            MotionSample sample = samples.get(i);
            double command = Math.abs(commandForAxis(axis, sample));
            double velocity = Math.abs(velocityForAxis(axis, sample));
            maxVelocity = Math.max(maxVelocity, velocity);

            if (command > 1e-4 && firstCommandTime < 0.0) {
                firstCommandTime = sample.timeSec;
            }
            if (deadzoneCommand == 0.0 && velocity > thresholdForAxis(axis)) {
                deadzoneCommand = command;
            }
            if (firstMotionTime < 0.0 && velocity > thresholdForAxis(axis)) {
                firstMotionTime = sample.timeSec;
            }

            if (i > 0) {
                double dt = Math.max(1e-6, sample.timeSec - previousTime);
                double accel = (velocity - previousVelocity) / dt;
                peakPositiveAccel = Math.max(peakPositiveAccel, accel);
                peakNegativeAccel = Math.min(peakNegativeAccel, accel);
            }

            previousVelocity = velocity;
            previousTime = sample.timeSec;
        }

        if (firstCommandTime >= 0.0 && firstMotionTime >= firstCommandTime) {
            riseTimeSec = firstMotionTime - firstCommandTime;
        }

        return new AxisSummary(
                axis,
                deadzoneCommand,
                maxVelocity,
                peakPositiveAccel,
                Math.abs(peakNegativeAccel),
                riseTimeSec);
    }

    public static BootstrapSeeds seedFromIdentification(AxisSummary forward, AxisSummary lateral, AxisSummary turn) {
        double forwardBrake = Math.max(10.0, forward.brakeRate);
        double lateralBrake = Math.max(10.0, lateral.brakeRate);

        double translationalResponsiveness = safeDiv(forward.accelRate + lateral.accelRate,
                Math.max(1.0, forward.maxVelocity + lateral.maxVelocity));
        double headingResponsiveness = safeDiv(turn.accelRate, Math.max(0.2, turn.maxVelocity));

        double translationP = clamp(0.05 + translationalResponsiveness * 0.10, 0.04, 0.35);
        double translationI = clamp(translationP * 0.04, 0.0, 0.03);
        double translationD = clamp(0.002 + forwardBrake * 0.00025 + lateralBrake * 0.00015, 0.001, 0.06);

        double secondaryTranslationP = clamp(translationP * 0.45, 0.01, 0.20);
        double secondaryTranslationI = clamp(translationI * 0.50, 0.0, 0.02);
        double secondaryTranslationD = clamp(translationD * 0.40, 0.0, 0.04);

        double headingP = clamp(0.8 + headingResponsiveness * 0.40 + safeDiv(1.0, Math.max(0.10, turn.riseTimeSec)) * 0.05, 0.5, 3.0);
        double headingI = clamp(headingP * 0.03, 0.0, 0.12);
        double headingD = clamp(0.01 + turn.brakeRate * 0.002, 0.005, 0.25);

        double secondaryHeadingP = clamp(headingP * 0.50, 0.2, 1.5);
        double secondaryHeadingI = clamp(headingI * 0.50, 0.0, 0.08);
        double secondaryHeadingD = clamp(headingD * 0.66, 0.0, 0.20);

        double averageLinearSpeed = Math.max(1.0, (forward.maxVelocity + lateral.maxVelocity) * 0.5);
        double driveP = clamp(0.008 + safeDiv(1.0, averageLinearSpeed) * 0.18, 0.006, 0.05);
        double driveI = clamp(driveP * 0.03, 0.0, 0.01);
        double driveD = clamp(0.0002 + safeDiv(forwardBrake + lateralBrake, 2.0) * 0.00003, 0.0001, 0.01);

        double secondaryDriveP = clamp(driveP * 0.55, 0.003, 0.03);
        double secondaryDriveI = clamp(driveI * 0.50, 0.0, 0.006);
        double secondaryDriveD = clamp(driveD * 0.60, 0.0, 0.008);

        double centripetalScaling = clamp(0.0002 + Math.abs(forward.maxVelocity - lateral.maxVelocity) * 0.00001, 0.0001, 0.0025);

        return new BootstrapSeeds(
                -forwardBrake,
                -lateralBrake,
                translationP,
                translationI,
                translationD,
                secondaryTranslationP,
                secondaryTranslationI,
                secondaryTranslationD,
                headingP,
                headingI,
                headingD,
                secondaryHeadingP,
                secondaryHeadingI,
                secondaryHeadingD,
                driveP,
                driveI,
                driveD,
                secondaryDriveP,
                secondaryDriveI,
                secondaryDriveD,
                centripetalScaling);
    }

    public static void applySeeds(BootstrapSeeds seeds) {
        PedroFollowerModelTuning.FORWARD_ZERO_POWER_ACCELERATION = seeds.forwardZeroPowerAcceleration;
        PedroFollowerModelTuning.LATERAL_ZERO_POWER_ACCELERATION = seeds.lateralZeroPowerAcceleration;

        PedroPrimaryTranslationTuning.ENABLED = true;
        PedroPrimaryTranslationTuning.P = seeds.primaryTranslationP;
        PedroPrimaryTranslationTuning.I = seeds.primaryTranslationI;
        PedroPrimaryTranslationTuning.D = seeds.primaryTranslationD;

        PedroSecondaryTranslationTuning.ENABLED = true;
        PedroSecondaryTranslationTuning.P = seeds.secondaryTranslationP;
        PedroSecondaryTranslationTuning.I = seeds.secondaryTranslationI;
        PedroSecondaryTranslationTuning.D = seeds.secondaryTranslationD;

        PedroPrimaryHeadingTuning.ENABLED = true;
        PedroPrimaryHeadingTuning.P = seeds.primaryHeadingP;
        PedroPrimaryHeadingTuning.I = seeds.primaryHeadingI;
        PedroPrimaryHeadingTuning.D = seeds.primaryHeadingD;

        PedroSecondaryHeadingTuning.ENABLED = true;
        PedroSecondaryHeadingTuning.P = seeds.secondaryHeadingP;
        PedroSecondaryHeadingTuning.I = seeds.secondaryHeadingI;
        PedroSecondaryHeadingTuning.D = seeds.secondaryHeadingD;

        PedroPrimaryDriveTuning.ENABLED = true;
        PedroPrimaryDriveTuning.P = seeds.primaryDriveP;
        PedroPrimaryDriveTuning.I = seeds.primaryDriveI;
        PedroPrimaryDriveTuning.D = seeds.primaryDriveD;

        PedroSecondaryDriveTuning.ENABLED = true;
        PedroSecondaryDriveTuning.P = seeds.secondaryDriveP;
        PedroSecondaryDriveTuning.I = seeds.secondaryDriveI;
        PedroSecondaryDriveTuning.D = seeds.secondaryDriveD;

        PedroPathControlTuning.CENTRIPETAL_SCALING = seeds.centripetalScaling;
    }

    private static double commandForAxis(Axis axis, MotionSample sample) {
        switch (axis) {
            case FORWARD:
                return sample.commandForward;
            case LATERAL:
                return sample.commandStrafe;
            case TURN:
            default:
                return sample.commandTurn;
        }
    }

    private static double velocityForAxis(Axis axis, MotionSample sample) {
        switch (axis) {
            case FORWARD:
                return sample.forwardVelocityInS;
            case LATERAL:
                return sample.lateralVelocityInS;
            case TURN:
            default:
                return sample.turnVelocityRadS;
        }
    }

    private static double thresholdForAxis(Axis axis) {
        return axis == Axis.TURN ? 0.15 : 1.0;
    }

    private static double safeDiv(double numerator, double denominator) {
        return Math.abs(denominator) < 1e-6 ? 0.0 : numerator / denominator;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
