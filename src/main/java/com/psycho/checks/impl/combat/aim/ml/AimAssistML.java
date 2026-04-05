package com.psycho.checks.impl.combat.aim.ml;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.ml.FeatureNormalizer;
import com.psycho.ml.models.GRU;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AimAssistML extends Check {
    private final Deque<double[]> recentData = new ArrayDeque<>();
    private final Deque<Double> probHistory = new ArrayDeque<>();
    private final Deque<Double> avgHistory = new ArrayDeque<>();
    private final double[][] raw;
    private final double[][] normalized;
    private final VlBuffer buffer = new VlBuffer();

    private final int seqLength = 80;
    private final int probHistorySize = 30;
    private final int avgHistorySize = 20;
    private final double emaAlpha = 0.25;

    private final GRU gru;
    private final FeatureNormalizer normalizer;
    private double emaProbability;
    private boolean emaInitialized;

    public AimAssistML(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
        raw = new double[seqLength][6];
        normalized = new double[seqLength][6];

        File dir = new File(Psycho.get().getDataFolder(), "ml");
        File modelFile = new File(dir, "model.bin");
        File normFile = new File(dir, "normalizer.bin");
        GRU loadedGru = null;
        FeatureNormalizer loadedNorm = null;
        if (modelFile.exists() && normFile.exists()) {
            try {
                loadedGru = GRU.load(modelFile);
                loadedNorm = FeatureNormalizer.load(normFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.gru = loadedGru;
        this.normalizer = loadedNorm;
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (gru == null || normalizer == null || !getCfg().enabled()) return;

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION && event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION)
            return;

        if (player.getTimeSinceLastHit() > 3000) {
            resetInferenceState();
            return;
        }

        if (Math.abs(player.getPitch()) > 89) {
            resetInferenceState();
            return;
        }

        recentData.add(new double[]{
                player.getDeltaYaw(),
                player.getDeltaPitch(),
                player.getAccelYaw(),
                player.getAccelPitch(),
                player.getJerkYaw(),
                player.getJerkPitch()
        });

        if (recentData.size() < seqLength) return;
        if (recentData.size() > seqLength) recentData.removeFirst();

        int i = 0;
        for (double[] row : recentData) raw[i++] = row;

        normalizer.transform(raw, normalized);

        double currentProb = clampProbability(gru.forward(normalized)[0]);

        push(probHistory, currentProb, probHistorySize);
        if (probHistory.size() < probHistorySize) return;

        double stableProbability = computeStableProbability(currentProb);
        push(avgHistory, stableProbability, avgHistorySize);

        if (shouldAccumulate(stableProbability)) {
            buffer.fail(computeBufferIncrement(stableProbability));
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

    private void resetInferenceState() {
        recentData.clear();
        probHistory.clear();
        emaProbability = 0.0;
        emaInitialized = false;
        buffer.decay(getCfg().decay());
    }

    private void push(Deque<Double> deque, double value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private double computeStableProbability(double currentProb) {
        if (!emaInitialized) {
            emaProbability = currentProb;
            emaInitialized = true;
        } else {
            emaProbability = emaProbability * (1.0 - emaAlpha) + currentProb * emaAlpha;
        }

        double median = percentile(probHistory, 50.0);
        double trimmedMean = trimmedMean(probHistory, 0.15);

        return clampProbability(median * 0.55 + trimmedMean * 0.30 + emaProbability * 0.15);
    }

    private boolean shouldAccumulate(double stableProbability) {
        double threshold = getCfg().probThreshold();
        double median = percentile(probHistory, 50.0);
        double trimmedMean = trimmedMean(probHistory, 0.15);
        double supportRatio = ratioAbove(probHistory, threshold - 0.08);
        double strictRatio = ratioAbove(probHistory, threshold);
        int highStreak = trailingAbove(probHistory, threshold - 0.08);

        boolean sustainedHigh = stableProbability >= threshold
                && median >= threshold - 0.05
                && trimmedMean >= threshold - 0.06
                && supportRatio >= 0.70
                && highStreak >= 6;

        boolean hardLock = stableProbability >= threshold - 0.02
                && percentile(probHistory, 75.0) >= threshold
                && strictRatio >= 0.60
                && highStreak >= 10;

        return sustainedHigh || hardLock;
    }

    private double computeBufferIncrement(double stableProbability) {
        double threshold = getCfg().probThreshold();
        double supportRatio = ratioAbove(probHistory, threshold - 0.05);
        int highStreak = trailingAbove(probHistory, threshold - 0.05);

        if (stableProbability >= 0.985 && supportRatio >= 0.85 && highStreak >= 12) {
            return 2.0;
        }

        if (stableProbability >= threshold + 0.04 && supportRatio >= 0.80 && highStreak >= 8) {
            return 1.5;
        }

        return 1.0;
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
        int streak = 0;
        List<Double> ordered = new ArrayList<>(values);
        for (int i = ordered.size() - 1; i >= 0; i--) {
            if (ordered.get(i) < threshold) {
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
}
