package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import java.util.ArrayList;
import java.util.List;

public final class PedroAutoTuner {
    private PedroAutoTuner() {
    }

    public static final class PidVector {
        public final double p;
        public final double i;
        public final double d;

        public PidVector(double p, double i, double d) {
            this.p = p;
            this.i = i;
            this.d = d;
        }

        public PidVector offset(double dp, double di, double dd) {
            return new PidVector(p + dp, i + di, d + dd);
        }
    }

    public static final class SearchConfig {
        public final int depth;
        public final double pStep;
        public final double iStep;
        public final double dStep;
        public final boolean tuneI;

        public SearchConfig(int depth, double pStep, double iStep, double dStep, boolean tuneI) {
            this.depth = depth;
            this.pStep = pStep;
            this.iStep = iStep;
            this.dStep = dStep;
            this.tuneI = tuneI;
        }

        public SearchConfig nextDepth() {
            return new SearchConfig(
                    depth - 1,
                    Math.max(1e-5, pStep * 0.5),
                    Math.max(1e-5, iStep * 0.5),
                    Math.max(1e-5, dStep * 0.5),
                    tuneI);
        }
    }

    public static final class SearchResult {
        public final PidVector bestPid;
        public final double bestScore;
        public final int evaluations;

        public SearchResult(PidVector bestPid, double bestScore, int evaluations) {
            this.bestPid = bestPid;
            this.bestScore = bestScore;
            this.evaluations = evaluations;
        }
    }

    public interface Evaluator {
        double evaluate(PidVector pid);
    }

    public static final class Sample {
        public final String phaseName;
        public final double timeSec;
        public final double targetXIn;
        public final double targetYIn;
        public final double targetHeadingDeg;
        public final double actualXIn;
        public final double actualYIn;
        public final double actualHeadingDeg;
        public final double actualVxInS;
        public final double actualVyInS;
        public final double actualOmegaDegS;
        public final double distanceRemaining;
        public final double translationAuthority;
        public final boolean steerReady;
        public final double batteryVoltage;
        public final boolean usingPinpoint;
        public final int invalidPinpointLoops;
        public final double rawPinpointXIn;
        public final double rawPinpointYIn;
        public final double rawPinpointHeadingDeg;

        public Sample(String phaseName, double timeSec, double targetXIn, double targetYIn, double targetHeadingDeg,
                      double actualXIn, double actualYIn, double actualHeadingDeg,
                      double actualVxInS, double actualVyInS, double actualOmegaDegS,
                      double distanceRemaining, double translationAuthority, boolean steerReady,
                      double batteryVoltage, boolean usingPinpoint, int invalidPinpointLoops,
                      double rawPinpointXIn, double rawPinpointYIn, double rawPinpointHeadingDeg) {
            this.phaseName = phaseName;
            this.timeSec = timeSec;
            this.targetXIn = targetXIn;
            this.targetYIn = targetYIn;
            this.targetHeadingDeg = targetHeadingDeg;
            this.actualXIn = actualXIn;
            this.actualYIn = actualYIn;
            this.actualHeadingDeg = actualHeadingDeg;
            this.actualVxInS = actualVxInS;
            this.actualVyInS = actualVyInS;
            this.actualOmegaDegS = actualOmegaDegS;
            this.distanceRemaining = distanceRemaining;
            this.translationAuthority = translationAuthority;
            this.steerReady = steerReady;
            this.batteryVoltage = batteryVoltage;
            this.usingPinpoint = usingPinpoint;
            this.invalidPinpointLoops = invalidPinpointLoops;
            this.rawPinpointXIn = rawPinpointXIn;
            this.rawPinpointYIn = rawPinpointYIn;
            this.rawPinpointHeadingDeg = rawPinpointHeadingDeg;
        }
    }

    public static final class PhaseSummary {
        public static final double TRANSLATION_SETTLE_BAND_IN = 1.0;
        public static final double HEADING_SETTLE_BAND_DEG = 3.0;

        public final String phaseName;
        public final int sampleCount;
        public final double durationSec;
        public final double rmsTranslationErrorIn;
        public final double maxTranslationErrorIn;
        public final double finalTranslationErrorIn;
        public final double rmsHeadingErrorDeg;
        public final double maxHeadingErrorDeg;
        public final double finalHeadingErrorDeg;
        public final double meanSpeedInS;
        public final double peakSpeedInS;
        public final int headingSignChanges;
        public final double meanDistanceRemainingIn;
        public final double maxDistanceRemainingIn;
        public final double meanTranslationAuthority;
        public final double minTranslationAuthority;
        public final double pinpointInvalidRatio;
        public final double meanBatteryVoltage;
        public final double translationOvershootIn;
        public final double headingOvershootDeg;
        public final double translationSettlingTimeSec;
        public final double headingSettlingTimeSec;
        public final double integratedAbsoluteTranslationError;
        public final double integratedAbsoluteHeadingError;

        public PhaseSummary(String phaseName, int sampleCount, double durationSec,
                            double rmsTranslationErrorIn, double maxTranslationErrorIn, double finalTranslationErrorIn,
                            double rmsHeadingErrorDeg, double maxHeadingErrorDeg, double finalHeadingErrorDeg,
                            double meanSpeedInS, double peakSpeedInS, int headingSignChanges,
                            double meanDistanceRemainingIn, double maxDistanceRemainingIn,
                            double meanTranslationAuthority, double minTranslationAuthority,
                            double pinpointInvalidRatio, double meanBatteryVoltage,
                            double translationOvershootIn, double headingOvershootDeg,
                            double translationSettlingTimeSec, double headingSettlingTimeSec,
                            double integratedAbsoluteTranslationError, double integratedAbsoluteHeadingError) {
            this.phaseName = phaseName;
            this.sampleCount = sampleCount;
            this.durationSec = durationSec;
            this.rmsTranslationErrorIn = rmsTranslationErrorIn;
            this.maxTranslationErrorIn = maxTranslationErrorIn;
            this.finalTranslationErrorIn = finalTranslationErrorIn;
            this.rmsHeadingErrorDeg = rmsHeadingErrorDeg;
            this.maxHeadingErrorDeg = maxHeadingErrorDeg;
            this.finalHeadingErrorDeg = finalHeadingErrorDeg;
            this.meanSpeedInS = meanSpeedInS;
            this.peakSpeedInS = peakSpeedInS;
            this.headingSignChanges = headingSignChanges;
            this.meanDistanceRemainingIn = meanDistanceRemainingIn;
            this.maxDistanceRemainingIn = maxDistanceRemainingIn;
            this.meanTranslationAuthority = meanTranslationAuthority;
            this.minTranslationAuthority = minTranslationAuthority;
            this.pinpointInvalidRatio = pinpointInvalidRatio;
            this.meanBatteryVoltage = meanBatteryVoltage;
            this.translationOvershootIn = translationOvershootIn;
            this.headingOvershootDeg = headingOvershootDeg;
            this.translationSettlingTimeSec = translationSettlingTimeSec;
            this.headingSettlingTimeSec = headingSettlingTimeSec;
            this.integratedAbsoluteTranslationError = integratedAbsoluteTranslationError;
            this.integratedAbsoluteHeadingError = integratedAbsoluteHeadingError;
        }
    }

    public static final class TuneRecommendations {
        public final double primaryTranslationP;
        public final double primaryTranslationD;
        public final double secondaryTranslationP;
        public final double secondaryTranslationD;
        public final double primaryHeadingP;
        public final double primaryHeadingD;
        public final double secondaryHeadingP;
        public final double secondaryHeadingD;
        public final double primaryDriveP;
        public final double primaryDriveD;
        public final double secondaryDriveP;
        public final double secondaryDriveD;
        public final double centripetalScaling;

        public TuneRecommendations(double primaryTranslationP, double primaryTranslationD,
                                   double secondaryTranslationP, double secondaryTranslationD,
                                   double primaryHeadingP, double primaryHeadingD,
                                   double secondaryHeadingP, double secondaryHeadingD,
                                   double primaryDriveP, double primaryDriveD,
                                   double secondaryDriveP, double secondaryDriveD,
                                   double centripetalScaling) {
            this.primaryTranslationP = primaryTranslationP;
            this.primaryTranslationD = primaryTranslationD;
            this.secondaryTranslationP = secondaryTranslationP;
            this.secondaryTranslationD = secondaryTranslationD;
            this.primaryHeadingP = primaryHeadingP;
            this.primaryHeadingD = primaryHeadingD;
            this.secondaryHeadingP = secondaryHeadingP;
            this.secondaryHeadingD = secondaryHeadingD;
            this.primaryDriveP = primaryDriveP;
            this.primaryDriveD = primaryDriveD;
            this.secondaryDriveP = secondaryDriveP;
            this.secondaryDriveD = secondaryDriveD;
            this.centripetalScaling = centripetalScaling;
        }
    }

    public static PhaseSummary summarize(String phaseName, List<Sample> samples) {
        if (samples.isEmpty()) {
            return new PhaseSummary(phaseName, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0,
                    0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        double sumTranslationSq = 0.0;
        double sumHeadingSq = 0.0;
        double maxTranslation = 0.0;
        double maxHeading = 0.0;
        double sumSpeed = 0.0;
        double peakSpeed = 0.0;
        double sumDistanceRemaining = 0.0;
        double maxDistanceRemaining = 0.0;
        double sumTranslationAuthority = 0.0;
        double minTranslationAuthority = Double.POSITIVE_INFINITY;
        double sumBatteryVoltage = 0.0;
        double integratedAbsoluteTranslationError = 0.0;
        double integratedAbsoluteHeadingError = 0.0;
        int invalidPinpointSamples = 0;
        double lastHeadingError = 0.0;
        double lastTimeSec = samples.get(0).timeSec;
        int headingSignChanges = 0;

        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            double dx = sample.targetXIn - sample.actualXIn;
            double dy = sample.targetYIn - sample.actualYIn;
            double translationError = Math.hypot(dx, dy);
            double headingError = angleErrorDeg(sample.targetHeadingDeg, sample.actualHeadingDeg);
            double speed = Math.hypot(sample.actualVxInS, sample.actualVyInS);

            sumTranslationSq += translationError * translationError;
            sumHeadingSq += headingError * headingError;
            maxTranslation = Math.max(maxTranslation, translationError);
            maxHeading = Math.max(maxHeading, Math.abs(headingError));
            sumSpeed += speed;
            peakSpeed = Math.max(peakSpeed, speed);
            sumDistanceRemaining += sample.distanceRemaining;
            maxDistanceRemaining = Math.max(maxDistanceRemaining, sample.distanceRemaining);
            sumTranslationAuthority += sample.translationAuthority;
            minTranslationAuthority = Math.min(minTranslationAuthority, sample.translationAuthority);
            sumBatteryVoltage += sample.batteryVoltage;
            double dt = i == 0 ? 0.0 : Math.max(0.0, sample.timeSec - lastTimeSec);
            integratedAbsoluteTranslationError += translationError * dt;
            integratedAbsoluteHeadingError += Math.abs(headingError) * dt;
            if (!sample.usingPinpoint || sample.invalidPinpointLoops > 0) {
                invalidPinpointSamples++;
            }

            if (i > 0 && Math.signum(lastHeadingError) != Math.signum(headingError) && Math.abs(headingError) > 1.0) {
                headingSignChanges++;
            }
            lastHeadingError = headingError;
            lastTimeSec = sample.timeSec;
        }

        Sample last = samples.get(samples.size() - 1);
        double finalTranslationError = Math.hypot(last.targetXIn - last.actualXIn, last.targetYIn - last.actualYIn);
        double finalHeadingError = angleErrorDeg(last.targetHeadingDeg, last.actualHeadingDeg);
        double durationSec = Math.max(0.0, last.timeSec - samples.get(0).timeSec);
        double translationOvershootIn = translationOvershoot(samples);
        double headingOvershootDeg = headingOvershoot(samples);
        double translationSettlingTimeSec = settlingTime(samples, true);
        double headingSettlingTimeSec = settlingTime(samples, false);

        return new PhaseSummary(
                phaseName,
                samples.size(),
                durationSec,
                Math.sqrt(sumTranslationSq / samples.size()),
                maxTranslation,
                finalTranslationError,
                Math.sqrt(sumHeadingSq / samples.size()),
                maxHeading,
                finalHeadingError,
                sumSpeed / samples.size(),
                peakSpeed,
                headingSignChanges,
                sumDistanceRemaining / samples.size(),
                maxDistanceRemaining,
                sumTranslationAuthority / samples.size(),
                minTranslationAuthority == Double.POSITIVE_INFINITY ? 1.0 : minTranslationAuthority,
                invalidPinpointSamples / (double) samples.size(),
                sumBatteryVoltage / samples.size(),
                translationOvershootIn,
                headingOvershootDeg,
                translationSettlingTimeSec,
                headingSettlingTimeSec,
                integratedAbsoluteTranslationError,
                integratedAbsoluteHeadingError);
    }

    public static TuneRecommendations recommend(List<PhaseSummary> summaries) {
        double translationRms = meanOf(summaries, true, false);
        double headingRms = meanOf(summaries, false, true);
        double meanSpeed = meanSpeed(summaries);
        double curvePenalty = curvePenalty(summaries);
        double meanAuthority = meanTranslationAuthority(summaries);
        double invalidPinpointRatio = meanPinpointInvalidRatio(summaries);

        double primaryTranslationP = scale(PedroPrimaryTranslationTuning.P, translationRms > 2.5 ? 1.18 : translationRms < 0.8 ? 0.96 : 1.06);
        double primaryTranslationD = scale(PedroPrimaryTranslationTuning.D,
                translationRms > 3.5 || meanAuthority < 0.55 ? 1.18 : 1.05);

        double secondaryTranslationP = primaryTranslationP * 0.42;
        double secondaryTranslationD = primaryTranslationD * 0.35;

        double primaryHeadingP = scale(PedroPrimaryHeadingTuning.P, headingRms > 6.0 ? 1.15 : headingRms < 2.0 ? 0.97 : 1.05);
        double primaryHeadingD = scale(PedroPrimaryHeadingTuning.D,
                headingRms > 8.0 || invalidPinpointRatio > 0.15 ? 1.20 : 1.05);
        double secondaryHeadingP = primaryHeadingP * 0.5;
        double secondaryHeadingD = primaryHeadingD * 0.66;

        double primaryDriveP = scale(PedroPrimaryDriveTuning.P, meanSpeed < 14.0 ? 1.18 : meanSpeed > 28.0 ? 0.96 : 1.04);
        double primaryDriveD = scale(PedroPrimaryDriveTuning.D, translationRms > 2.5 ? 1.12 : 1.03);
        double secondaryDriveP = primaryDriveP * 0.5;
        double secondaryDriveD = primaryDriveD * 0.6;

        double centripetalScaling = clamp(PedroPathControlTuning.CENTRIPETAL_SCALING * (1.0 + curvePenalty * 0.15), 0.0, 0.01);

        return new TuneRecommendations(
                primaryTranslationP, primaryTranslationD,
                secondaryTranslationP, secondaryTranslationD,
                primaryHeadingP, primaryHeadingD,
                secondaryHeadingP, secondaryHeadingD,
                primaryDriveP, primaryDriveD,
                secondaryDriveP, secondaryDriveD,
                centripetalScaling);
    }

    public static void apply(TuneRecommendations recommendations) {
        PedroPrimaryTranslationTuning.P = recommendations.primaryTranslationP;
        PedroPrimaryTranslationTuning.D = recommendations.primaryTranslationD;
        PedroSecondaryTranslationTuning.P = recommendations.secondaryTranslationP;
        PedroSecondaryTranslationTuning.D = recommendations.secondaryTranslationD;

        PedroPrimaryHeadingTuning.P = recommendations.primaryHeadingP;
        PedroPrimaryHeadingTuning.D = recommendations.primaryHeadingD;
        PedroSecondaryHeadingTuning.P = recommendations.secondaryHeadingP;
        PedroSecondaryHeadingTuning.D = recommendations.secondaryHeadingD;

        PedroPrimaryDriveTuning.P = recommendations.primaryDriveP;
        PedroPrimaryDriveTuning.D = recommendations.primaryDriveD;
        PedroSecondaryDriveTuning.P = recommendations.secondaryDriveP;
        PedroSecondaryDriveTuning.D = recommendations.secondaryDriveD;

        PedroPathControlTuning.CENTRIPETAL_SCALING = recommendations.centripetalScaling;
    }

    public static List<PhaseSummary> splitAndSummarize(List<Sample> allSamples) {
        List<PhaseSummary> summaries = new ArrayList<PhaseSummary>();
        List<Sample> phaseBuffer = new ArrayList<Sample>();
        String currentPhase = null;
        for (Sample sample : allSamples) {
            if (currentPhase == null || !currentPhase.equals(sample.phaseName)) {
                if (!phaseBuffer.isEmpty()) {
                    summaries.add(summarize(currentPhase, phaseBuffer));
                    phaseBuffer = new ArrayList<Sample>();
                }
                currentPhase = sample.phaseName;
            }
            phaseBuffer.add(sample);
        }
        if (!phaseBuffer.isEmpty()) {
            summaries.add(summarize(currentPhase, phaseBuffer));
        }
        return summaries;
    }

    public static SearchConfig defaultSearch(PidVector current, boolean tuneI) {
        return new SearchConfig(
                3,
                Math.max(0.01, Math.abs(current.p) * 0.5),
                Math.max(0.0005, Math.abs(current.i) * 0.5 + 0.0005),
                Math.max(0.001, Math.abs(current.d) * 0.5 + 0.001),
                tuneI);
    }

    public static SearchResult recursiveSearch(PidVector start, SearchConfig config, Evaluator evaluator) {
        SearchAccumulator accumulator = new SearchAccumulator();
        SearchNode result = recursiveSearchInternal(start, config, evaluator, accumulator);
        return new SearchResult(result.pid, result.score, accumulator.evaluations);
    }

    private static SearchNode recursiveSearchInternal(PidVector center, SearchConfig config, Evaluator evaluator, SearchAccumulator accumulator) {
        SearchNode best = evaluateNode(center, evaluator, accumulator);
        if (config.depth <= 0) {
            return best;
        }

        PidVector[] candidates = config.tuneI
                ? new PidVector[] {
                        center.offset(config.pStep, 0.0, 0.0),
                        center.offset(-config.pStep, 0.0, 0.0),
                        center.offset(0.0, config.iStep, 0.0),
                        center.offset(0.0, -config.iStep, 0.0),
                        center.offset(0.0, 0.0, config.dStep),
                        center.offset(0.0, 0.0, -config.dStep)
                }
                : new PidVector[] {
                        center.offset(config.pStep, 0.0, 0.0),
                        center.offset(-config.pStep, 0.0, 0.0),
                        center.offset(0.0, 0.0, config.dStep),
                        center.offset(0.0, 0.0, -config.dStep)
                };

        for (PidVector candidate : candidates) {
            SearchNode candidateNode = evaluateNode(clampPid(candidate), evaluator, accumulator);
            if (candidateNode.score < best.score) {
                best = candidateNode;
            }
        }

        SearchNode deeper = recursiveSearchInternal(best.pid, config.nextDepth(), evaluator, accumulator);
        return deeper.score < best.score ? deeper : best;
    }

    private static SearchNode evaluateNode(PidVector pid, Evaluator evaluator, SearchAccumulator accumulator) {
        accumulator.evaluations++;
        return new SearchNode(pid, evaluator.evaluate(pid));
    }

    private static PidVector clampPid(PidVector pid) {
        return new PidVector(
                clamp(pid.p, 0.0, 100.0),
                clamp(pid.i, 0.0, 100.0),
                clamp(pid.d, 0.0, 100.0));
    }

    private static final class SearchNode {
        private final PidVector pid;
        private final double score;

        private SearchNode(PidVector pid, double score) {
            this.pid = pid;
            this.score = score;
        }
    }

    private static final class SearchAccumulator {
        private int evaluations = 0;
    }

    private static double meanOf(List<PhaseSummary> summaries, boolean translation, boolean heading) {
        double total = 0.0;
        int count = 0;
        for (PhaseSummary summary : summaries) {
            if (translation) {
                total += summary.rmsTranslationErrorIn;
                count++;
            }
            if (heading) {
                total += summary.rmsHeadingErrorDeg;
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    private static double meanSpeed(List<PhaseSummary> summaries) {
        double total = 0.0;
        for (PhaseSummary summary : summaries) {
            total += summary.meanSpeedInS;
        }
        return summaries.isEmpty() ? 0.0 : total / summaries.size();
    }

    private static double curvePenalty(List<PhaseSummary> summaries) {
        double penalty = 0.0;
        for (PhaseSummary summary : summaries) {
            if (summary.phaseName.toLowerCase().contains("curve")) {
                penalty += summary.rmsTranslationErrorIn;
            }
        }
        return penalty;
    }

    private static double translationOvershoot(List<Sample> samples) {
        Sample last = samples.get(samples.size() - 1);
        double targetDx = last.targetXIn - samples.get(0).actualXIn;
        double targetDy = last.targetYIn - samples.get(0).actualYIn;
        double targetMag = Math.hypot(targetDx, targetDy);
        if (targetMag < 1e-6) {
            return 0.0;
        }

        double dirX = targetDx / targetMag;
        double dirY = targetDy / targetMag;
        double startProj = samples.get(0).actualXIn * dirX + samples.get(0).actualYIn * dirY;
        double targetProj = last.targetXIn * dirX + last.targetYIn * dirY;
        double overshoot = 0.0;
        for (Sample sample : samples) {
            double proj = sample.actualXIn * dirX + sample.actualYIn * dirY;
            overshoot = Math.max(overshoot, Math.abs(proj - startProj) - Math.abs(targetProj - startProj));
        }
        return Math.max(0.0, overshoot);
    }

    private static double headingOvershoot(List<Sample> samples) {
        double maxOvershoot = 0.0;
        Sample first = samples.get(0);
        double desiredDelta = angleErrorDeg(first.targetHeadingDeg, first.actualHeadingDeg);
        double desiredDirection = Math.signum(desiredDelta);
        if (desiredDirection == 0.0) {
            return 0.0;
        }
        for (Sample sample : samples) {
            double currentDelta = angleErrorDeg(sample.targetHeadingDeg, sample.actualHeadingDeg);
            if (Math.signum(currentDelta) != 0.0 && Math.signum(currentDelta) != desiredDirection) {
                maxOvershoot = Math.max(maxOvershoot, Math.abs(currentDelta));
            }
        }
        return maxOvershoot;
    }

    private static double settlingTime(List<Sample> samples, boolean translation) {
        double band = translation ? PhaseSummary.TRANSLATION_SETTLE_BAND_IN : PhaseSummary.HEADING_SETTLE_BAND_DEG;
        double settleTime = samples.get(samples.size() - 1).timeSec - samples.get(0).timeSec;
        for (int i = 0; i < samples.size(); i++) {
            boolean settled = true;
            for (int j = i; j < samples.size(); j++) {
                Sample sample = samples.get(j);
                double error = translation
                        ? Math.hypot(sample.targetXIn - sample.actualXIn, sample.targetYIn - sample.actualYIn)
                        : Math.abs(angleErrorDeg(sample.targetHeadingDeg, sample.actualHeadingDeg));
                if (error > band) {
                    settled = false;
                    break;
                }
            }
            if (settled) {
                settleTime = samples.get(i).timeSec - samples.get(0).timeSec;
                break;
            }
        }
        return Math.max(0.0, settleTime);
    }

    private static double meanTranslationAuthority(List<PhaseSummary> summaries) {
        double total = 0.0;
        for (PhaseSummary summary : summaries) {
            total += summary.meanTranslationAuthority;
        }
        return summaries.isEmpty() ? 1.0 : total / summaries.size();
    }

    private static double meanPinpointInvalidRatio(List<PhaseSummary> summaries) {
        double total = 0.0;
        for (PhaseSummary summary : summaries) {
            total += summary.pinpointInvalidRatio;
        }
        return summaries.isEmpty() ? 0.0 : total / summaries.size();
    }

    private static double angleErrorDeg(double targetDeg, double actualDeg) {
        double error = targetDeg - actualDeg;
        while (error > 180.0) {
            error -= 360.0;
        }
        while (error < -180.0) {
            error += 360.0;
        }
        return error;
    }

    private static double scale(double base, double multiplier) {
        return clamp(base * multiplier, 0.0, 100.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
