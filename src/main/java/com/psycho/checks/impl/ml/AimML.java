package com.psycho.checks.impl.ml;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.ml.gru.FeatureNormalizer;
import com.psycho.ml.gru.GRU;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.buffer.VlBuffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class AimML extends Check {

    private final Deque<double[]> recentData = new ArrayDeque<>();
    private final Deque<Double> probHistory = new ArrayDeque<>();
    private final double[][] raw;
    private final double[][] normalized;
    private final VlBuffer buffer = new VlBuffer();

    private final int seqLength = 60;
    private final int probHistorySize = 20;

    private final GRU gru;
    private final FeatureNormalizer normalizer;

    public AimML(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
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

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION &&
                event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION) return;

        if (player.getTimeSinceLastHit() > 2000) {
            recentData.clear();
            probHistory.clear();
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

        double currentProb = gru.forward(normalized)[0];

        probHistory.add(currentProb);
        if (probHistory.size() > probHistorySize) probHistory.removeFirst(); else return;

        double[] arr = probHistory.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(arr);
        double median = arr[arr.length / 2];
        if (arr.length % 2 == 0) {
            median = (arr[arr.length / 2 - 1] + median) / 2.0;
        }

        if (median > getCfg().probThreshold()) {
            buffer.fail(1);
        } else {
            buffer.decay(getCfg().decay());
        }

        if (buffer.getVl() > getCfg().bufferThreshold()) {
            flag(String.format("median=%.2f", median * 100));
        }

        probHistory.clear();
        recentData.clear();
    }
}