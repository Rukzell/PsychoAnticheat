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
    private final Map<UUID, Deque<double[]>> playerSequences = new ConcurrentHashMap<>();
    private final Map<UUID, Double> playerVlBuffer = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Double>> playerProbHistory = new ConcurrentHashMap<>();
    private final int seqLength = 50;
    private final int probHistorySize = 10;
    private FeatureNormalizer normalizer;
    private double decay;

    public AimML(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
        File dir = new File(Psycho.get().getDataFolder(), "ml");
        File modelFile = new File(dir, "model.bin");
        File normFile  = new File(dir, "normalizer.bin");
        if (modelFile.exists()) {
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
        if (gru == null || !getCfg().enabled()) return;

        UUID uuid = player.getBukkitPlayer().getUniqueId();

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {

            if (player.getTimeSinceLastHit() > 2000) {
                cleanupPlayer(uuid);
                return;
            }

            Deque<double[]> recentData = playerSequences.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            Deque<Double> probHistory = playerProbHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            double vlBuffer = playerVlBuffer.getOrDefault(uuid, 0.0);
            VlBuffer buffer = player.getBuffer(getName());

            double dy = player.getDeltaYaw();
            double dp = player.getDeltaPitch();
            double ay = player.getAccelYaw();
            double ap = player.getAccelPitch();
            double jy = player.getJerkYaw();
            double jp = player.getJerkPitch();

            recentData.add(new double[]{dy, dp, ay, ap, jy, jp});

            if (recentData.size() > seqLength) {
                recentData.removeFirst();

                double[][] raw        = recentData.toArray(new double[seqLength][]);
                double[][] normalized = normalizer.transform(raw);
                double[]   result     = gru.forward(normalized);
                double currentProb    = result[0];

                probHistory.add(currentProb);
                if (probHistory.size() > probHistorySize) {
                    probHistory.removeFirst();
                }

                double avgProb = probHistory.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(currentProb);

                if (avgProb >= 0.99 && MathUtil.min(probHistory) > 0.8) {
                    buffer.fail(1);
                } else {
                    buffer.decay(decay);
                }

//                player.getBukkitPlayer().sendMessage("prob=" + avgProb);

                if (buffer.getVl() > 5) {
                    flag(player, String.format("avg=%.2f", avgProb * 100));
                    buffer.setVl(0);
                }

                playerVlBuffer.put(uuid, vlBuffer);
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        playerSequences.remove(uuid);
        playerVlBuffer.remove(uuid);
        playerProbHistory.remove(uuid);
    }
}