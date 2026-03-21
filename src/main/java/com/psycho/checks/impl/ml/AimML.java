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
import com.psycho.utils.math.MathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AimML extends Check {
    private GRU gru;
    private FeatureNormalizer normalizer;

    private final Map<UUID, Deque<double[]>> playerSequences = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Double>> playerProbHistory = new ConcurrentHashMap<>();

    private final Map<UUID, double[][]> rawCache = new ConcurrentHashMap<>();
    private final Map<UUID, double[][]> normalizedCache = new ConcurrentHashMap<>();

    private final int seqLength = 50;
    private final int probHistorySize = 10;
    private final double decay;

    public AimML(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
        File dir       = new File(Psycho.get().getDataFolder(), "ml");
        File modelFile = new File(dir, "model.bin");
        File normFile  = new File(dir, "normalizer.bin");
        if (modelFile.exists() && normFile.exists()) {
            try {
                gru = GRU.load(modelFile);
                normalizer = FeatureNormalizer.load(normFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        decay = cfg.decay();
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (gru == null || normalizer == null || !getCfg().enabled()) return;

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION &&
                event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION) return;

        UUID uuid = player.getBukkitPlayer().getUniqueId();

        if (player.getTimeSinceLastHit() > 2000) {
            cleanupPlayer(uuid);
            return;
        }

        Deque<double[]> recentData = playerSequences.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        Deque<Double> probHistory = playerProbHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());

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

        double[][] raw = rawCache.computeIfAbsent(uuid, k -> new double[seqLength][6]);
        double[][] normalized = normalizedCache.computeIfAbsent(uuid, k -> new double[seqLength][6]);

        int i = 0;
        for (double[] row : recentData) raw[i++] = row;

        normalizer.transform(raw, normalized);

        double currentProb = gru.forward(normalized)[0];

        probHistory.add(currentProb);
        if (probHistory.size() > probHistorySize) probHistory.removeFirst();

        double sum = 0;
        for (double p : probHistory) sum += p;
        double avgProb = sum / probHistory.size();

        VlBuffer buffer = player.getBuffer(getName());

        if (avgProb >= 0.99 && MathUtil.min(probHistory) > 0.95) {
            buffer.fail(1);
        } else {
            buffer.decay(decay);
        }

        if (buffer.getVl() > 5) {
            flag(player, String.format("avg=%.2f", avgProb * 100));
            buffer.setVl(0);
        }
    }

    public void cleanupPlayer(UUID uuid) {
        playerSequences.remove(uuid);
        playerProbHistory.remove(uuid);
        rawCache.remove(uuid);
        normalizedCache.remove(uuid);
    }
}