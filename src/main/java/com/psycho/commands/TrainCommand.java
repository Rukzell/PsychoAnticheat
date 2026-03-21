package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.ml.gru.GRU;
import com.psycho.ml.gru.FeatureNormalizer;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrainCommand implements SubCommand {
    private final Psycho plugin;

    public TrainCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "train";
    }

    @Override
    public String getPermission() {
        return "psycho.command.train";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // args: [train] [epochs] [learning_rate]
        int epochs = 500;
        double lr = 0.001;

        if (args.length >= 2) {
            try {
                epochs = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid epochs number: " + args[1]);
                return;
            }
        }

        if (args.length >= 3) {
            try {
                lr = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid learning rate: " + args[2]);
                return;
            }
        }

        final int finalEpochs = epochs;
        final double finalLr = lr;

        sender.sendMessage("§eStarting GRU training...");
        sender.sendMessage("§7Epochs: §f" + finalEpochs + " §7| LR: §f" + finalLr);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File dir = new File(plugin.getDataFolder(), "ml");
                File file = new File(dir, "dataset.csv");
                if (!file.exists()) {
                    sender.sendMessage("§cDataset CSV not found!");
                    return;
                }

                List<double[]> dataRows = new ArrayList<>();
                List<double[]> targetRows = new ArrayList<>();

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    boolean isFirst = true;

                    while ((line = br.readLine()) != null) {
                        if (isFirst) { isFirst = false; continue; }
                        String[] parts = line.split(",");
                        if (parts.length < 7) continue;

                        double[] x = new double[6];
                        for (int i = 0; i < 6; i++) {
                            try { x[i] = Double.parseDouble(parts[i]); }
                            catch (Exception e) { x[i] = 0; }
                        }

                        double label = 0;
                        try { label = Double.parseDouble(parts[6]); } catch (Exception ignored) {}

                        dataRows.add(x);
                        targetRows.add(new double[]{label});
                    }
                }

                int seqLength = 70;
                int sequenceCount = dataRows.size() / seqLength;
                if (sequenceCount == 0) {
                    sender.sendMessage("§cNot enough data for a single sequence.");
                    return;
                }

                double[][][] sequences = new double[sequenceCount][seqLength][6];
                double[][] targets = new double[sequenceCount][1];

                for (int i = 0; i < sequenceCount; i++) {
                    double finalLabel = 0;
                    for (int j = 0; j < seqLength; j++) {
                        sequences[i][j] = dataRows.get(i * seqLength + j);
                        if (targetRows.get(i * seqLength + j)[0] == 1.0) {
                            finalLabel = 1.0;
                        }
                    }
                    targets[i][0] = finalLabel;
                }

                sender.sendMessage("§eData loaded: " + sequenceCount + " sequences.");

                // ── Feature normalization ──────────────────────────────
                FeatureNormalizer normalizer = new FeatureNormalizer(6);
                normalizer.fit(sequences);
                sequences = normalizer.transform(sequences);
                normalizer.save(new File(dir, "normalizer.bin"));

                // ── GRU training ──────────────────────────────────────
                GRU gru = new GRU(6, 16, 1);
                gru.train(sequences, targets, finalLr, finalEpochs);

                // ── Save model ───────────────────────────────────────
                File modelFile = new File(dir, "model.bin");
                gru.save(modelFile);

                sender.sendMessage("§aTraining completed! Model saved to model.bin, normalizer saved to normalizer.bin");

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("§cAn error occurred during training.");
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}