package com.psycho.checks.impl.combat.aim.ml;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.ml.models.GRU;
import com.psycho.player.PsychoPlayer;
import com.psycho.services.MlModelService;
import com.psycho.utils.buffer.VlBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AimAssistML extends Check {
    private final Deque<Double> probHistory = new ArrayDeque<>();
    private final Deque<Double> avgHistory = new ArrayDeque<>();
    private final double[][] raw;
    private final double[][] normalized;
    private final VlBuffer buffer = new VlBuffer();

    private final int seqLength = 80;
    private final int probHistorySize = 30;
    private final int avgHistorySize = 20;
    private final double emaAlpha = 0.25;

    private int sampleCount;
    private double emaProbability;
    private boolean emaInitialized;
    private GRU inferenceGru;
    private GRU.InferenceContext inferenceContext;

    public AimAssistML(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
        raw = new double[seqLength][6];
        normalized = new double[seqLength][6];
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        MlModelService.LoadedModel loadedModel = Psycho.get().getMlModelService().getLoadedModel();
        if (loadedModel == null || !getCfg().enabled()) return;
        ensureInferenceContext(loadedModel.gru());

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION && event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION)
            return;

        if (player.getTimeSinceLastHit() > 3000) {
            return;
        }

        if (Math.abs(player.getPitch()) > 89) {
            return;
        }

        appendSample(
                player.getDeltaYaw(),
                player.getDeltaPitch(),
                player.getAccelYaw(),
                player.getAccelPitch(),
                player.getJerkYaw(),
                player.getJerkPitch()
        );

        if (sampleCount < seqLength) return;

        loadedModel.normalizer().transform(raw, normalized);
        double currentProb = clampProbability(loadedModel.gru().predictScalar(normalized, inferenceContext));

        push(probHistory, currentProb, probHistorySize);
        if (probHistory.size() < probHistorySize) return;

        ProbabilityStats stats = analyzeProbabilities(getCfg().probThreshold());
        double stableProbability = computeStableProbability(currentProb, stats);
        push(avgHistory, stableProbability, avgHistorySize);

        if (shouldAccumulate(stableProbability, stats)) {
            buffer.fail(computeBufferIncrement(stableProbability, stats));
        } else {
            buffer.decay(getCfg().decay());
        }

        if (buffer.getVl() > getCfg().bufferThreshold()) {
            flag();
            buffer.setVl(0);
        }
    }

    public Deque<Double> getProbHistory() {
        return new ArrayDeque<>(probHistory);
    }

    public Deque<Double> getAvgHistory() {
        return new ArrayDeque<>(avgHistory);
    }

    private void appendSample(double deltaYaw,
                              double deltaPitch,
                              double accelYaw,
                              double accelPitch,
                              double jerkYaw,
                              double jerkPitch) {
        double[] row;

        if (sampleCount < seqLength) {
            row = raw[sampleCount++];
        } else {
            row = raw[0];
            System.arraycopy(raw, 1, raw, 0, seqLength - 1);
            raw[seqLength - 1] = row;
        }

        row[0] = deltaYaw;
        row[1] = deltaPitch;
        row[2] = accelYaw;
        row[3] = accelPitch;
        row[4] = jerkYaw;
        row[5] = jerkPitch;
    }

    private void ensureInferenceContext(GRU gru) {
        if (inferenceGru == gru && inferenceContext != null) {
            return;
        }

        inferenceGru = gru;
        inferenceContext = gru.createInferenceContext();
    }

    private void push(Deque<Double> deque, double value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private double computeStableProbability(double currentProb, ProbabilityStats stats) {
        if (!emaInitialized) {
            emaProbability = currentProb;
            emaInitialized = true;
        } else {
            emaProbability = emaProbability * (1.0 - emaAlpha) + currentProb * emaAlpha;
        }

        return clampProbability(stats.median() * 0.55 + stats.trimmedMean() * 0.30 + emaProbability * 0.15);
    }

    private boolean shouldAccumulate(double stableProbability, ProbabilityStats stats) {
        double threshold = getCfg().probThreshold();

        boolean sustainedHigh = stableProbability >= threshold
                && stats.median() >= threshold - 0.05
                && stats.trimmedMean() >= threshold - 0.06
                && stats.supportRatioLoose() >= 0.70
                && stats.highStreakLoose() >= 6;

        boolean hardLock = stableProbability >= threshold - 0.02
                && stats.upperQuartile() >= threshold
                && stats.supportRatioStrict() >= 0.60
                && stats.highStreakLoose() >= 10;

        return sustainedHigh || hardLock;
    }

    private double computeBufferIncrement(double stableProbability, ProbabilityStats stats) {
        if (stableProbability >= 0.985 && stats.supportRatioBuffer() >= 0.85 && stats.highStreakBuffer() >= 12) {
            return 2.0;
        }

        if (stableProbability >= getCfg().probThreshold() + 0.04
                && stats.supportRatioBuffer() >= 0.80
                && stats.highStreakBuffer() >= 8) {
            return 1.5;
        }

        return 1.0;
    }

    private ProbabilityStats analyzeProbabilities(double threshold) {
        List<Double> ordered = new ArrayList<>(probHistory);
        List<Double> sorted = new ArrayList<>(ordered);
        Collections.sort(sorted);

        double looseThreshold = threshold - 0.08;
        double bufferThreshold = threshold - 0.05;

        int aboveLoose = 0;
        int aboveStrict = 0;
        int aboveBuffer = 0;

        for (double value : ordered) {
            if (value >= looseThreshold) aboveLoose++;
            if (value >= threshold) aboveStrict++;
            if (value >= bufferThreshold) aboveBuffer++;
        }

        int highStreakLoose = trailingAbove(ordered, looseThreshold);
        int highStreakBuffer = trailingAbove(ordered, bufferThreshold);

        double size = ordered.size();
        return new ProbabilityStats(
                percentileSorted(sorted, 50.0),
                percentileSorted(sorted, 75.0),
                trimmedMeanSorted(sorted, 0.15),
                aboveLoose / size,
                aboveStrict / size,
                aboveBuffer / size,
                highStreakLoose,
                highStreakBuffer
        );
    }

    private double ratioAbove(Deque<Double> values, double threshold) {
        if (values.isEmpty()) {
            return 0.0;
        }

        int count = 0;
        for (double value : values) {
            if (value >= threshold) {
                count++;
            }
        }

        return count / (double) values.size();
    }

    private int trailingAbove(Deque<Double> values, double threshold) {
        return trailingAbove(new ArrayList<>(values), threshold);
    }

    private int trailingAbove(List<Double> values, double threshold) {
        int streak = 0;
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) < threshold) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private double trimmedMean(Deque<Double> values, double trimFraction) {
        if (values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return trimmedMeanSorted(sorted, trimFraction);
    }

    private double trimmedMeanSorted(List<Double> sorted, double trimFraction) {
        int trim = (int) Math.floor(sorted.size() * trimFraction);
        int from = Math.min(trim, sorted.size() - 1);
        int to = Math.max(from + 1, sorted.size() - trim);

        double sum = 0.0;
        int count = 0;
        for (int i = from; i < to; i++) {
            sum += sorted.get(i);
            count++;
        }

        return count == 0 ? 0.0 : sum / count;
    }

    private double percentile(Deque<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return percentileSorted(sorted, percentile);
    }

    private double percentileSorted(List<Double> sorted, double percentile) {
        if (sorted.size() == 1) {
            return sorted.get(0);
        }

        double index = (percentile / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double weight = index - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }

    private double clampProbability(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record ProbabilityStats(double median,
                                    double upperQuartile,
                                    double trimmedMean,
                                    double supportRatioLoose,
                                    double supportRatioStrict,
                                    double supportRatioBuffer,
                                    int highStreakLoose,
                                    int highStreakBuffer) {
    }
}
