package com.psycho.ml;

import com.psycho.Psycho;
import com.psycho.player.PsychoPlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataCollector {
    private static final Map<UUID, Integer> collectingTarget = new HashMap<>();
    private static BufferedWriter writer;

    public static void startCollecting(UUID uuid, int label) {
        collectingTarget.put(uuid, label);
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

    public static void collect(PsychoPlayer player) {
        if (!isCollecting(player.getBukkitPlayer().getUniqueId())) return;
        
        int label = collectingTarget.get(player.getBukkitPlayer().getUniqueId());
        
        String dataLine = String.format("%f,%f,%f,%f,%f,%f,%d\n",
                player.getDeltaYaw(),
                player.getDeltaPitch(),
                player.getAccelYaw(),
                player.getAccelPitch(),
                player.getJerkYaw(),
                player.getJerkPitch(),
                label
        ).replace(",", "."); // Fix locale issues with comma in float, but we need comma for csv... wait.

        // Correct formatting for CSV
        dataLine = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%d\n",
                player.getDeltaYaw(), player.getDeltaPitch(),
                player.getAccelYaw(), player.getAccelPitch(),
                player.getJerkYaw(), player.getJerkPitch(),
                label);

        try {
            if (writer != null) {
                writer.write(dataLine);
                writer.flush(); // Flush so we don't lose data on crash
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
