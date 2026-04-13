package com.psycho.ml;

import com.psycho.Psycho;
import com.psycho.player.PsychoPlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

public class DataCollector {
    private static final Map<UUID, Integer> collectingTarget = new HashMap<>();
    private static final Map<UUID, Long> collectedTicks = new HashMap<>();
    private static BufferedWriter writer;

    public static void startCollecting(UUID uuid, int label) {
        collectingTarget.put(uuid, label);
        collectedTicks.put(uuid, 0L);
        try {
            if (writer == null) {
                File dir = new File(Psycho.get().getDataFolder(), "ml");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "dataset.csv");
                boolean writeHeader = !file.exists();
                writer = new BufferedWriter(new FileWriter(file, true));
                if (writeHeader) {
                    writer.write("deltaYaw,deltaPitch,accelYaw,accelPitch,jerkYaw,jerkPitch,label\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopCollecting(UUID uuid) {
        collectingTarget.remove(uuid);
        collectedTicks.remove(uuid);
        if (collectingTarget.isEmpty() && writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isCollecting(UUID uuid) {
        return collectingTarget.containsKey(uuid);
    }

    public static Integer getCollectingLabel(UUID uuid) {
        return collectingTarget.get(uuid);
    }

    public static Map<UUID, SessionStatus> getActiveCollections() {
        Map<UUID, SessionStatus> activeCollections = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : collectingTarget.entrySet()) {
            UUID uuid = entry.getKey();
            activeCollections.put(uuid, new SessionStatus(
                    entry.getValue(),
                    collectedTicks.getOrDefault(uuid, 0L)
            ));
        }
        return activeCollections;
    }

    public static void collect(PsychoPlayer player) {
        if (!isCollecting(player.getBukkitPlayer().getUniqueId())) return;

        UUID uuid = player.getBukkitPlayer().getUniqueId();
        int label = collectingTarget.get(uuid);

        String dataLine = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%d\n",
                player.getDeltaYaw(), player.getDeltaPitch(),
                player.getAccelYaw(), player.getAccelPitch(),
                player.getJerkYaw(), player.getJerkPitch(),
                label);

        try {
            if (writer != null) {
                writer.write(dataLine);
                writer.flush();
                collectedTicks.merge(uuid, 1L, Long::sum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public record SessionStatus(int label, long collectedTicks) {
    }
}
