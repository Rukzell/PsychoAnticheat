package com.psycho.services;

import com.psycho.Psycho;
import com.psycho.ml.FeatureNormalizer;
import com.psycho.ml.models.GRU;
import com.psycho.utils.Logger;

import java.io.File;
import java.io.IOException;

public class MlModelService {
    private final Psycho plugin;
    private volatile LoadedModel loadedModel;

    public MlModelService(Psycho plugin) {
        this.plugin = plugin;
    }

    public synchronized Result load() {
        if (loadedModel != null) {
            return new Result(true, "§eML model is already loaded.");
        }

        try {
            loadedModel = loadFromDisk();
            Logger.log("ML model loaded");
            return new Result(true, "§aML model loaded.");
        } catch (IllegalStateException e) {
            Logger.warn(e.getMessage());
            return new Result(false, "§c" + e.getMessage());
        } catch (IOException e) {
            Logger.error("Failed to load ML model: " + e.getMessage());
            return new Result(false, "§cFailed to load ML model: " + e.getMessage());
        }
    }

    public synchronized Result unload() {
        if (loadedModel == null) {
            return new Result(true, "§eML model is already unloaded.");
        }

        loadedModel = null;
        Logger.log("ML model unloaded");
        return new Result(true, "§aML model unloaded.");
    }

    public synchronized Result reload() {
        try {
            LoadedModel reloadedModel = loadFromDisk();
            loadedModel = reloadedModel;
            Logger.log("ML model reloaded");
            return new Result(true, "§aML model reloaded.");
        } catch (IllegalStateException e) {
            Logger.warn(e.getMessage());
            return new Result(false, "§c" + e.getMessage());
        } catch (IOException e) {
            Logger.error("Failed to reload ML model: " + e.getMessage());
            return new Result(false, "§cFailed to reload ML model: " + e.getMessage());
        }
    }

    public LoadedModel getLoadedModel() {
        return loadedModel;
    }

    private LoadedModel loadFromDisk() throws IOException {
        File modelFile = new File(new File(plugin.getDataFolder(), "ml"), "model.bin");
        File normalizerFile = new File(new File(plugin.getDataFolder(), "ml"), "normalizer.bin");

        if (!modelFile.exists() || !normalizerFile.exists()) {
            throw new IllegalStateException("ML files not found. Expected model.bin and normalizer.bin in /plugins/Psycho/ml.");
        }

        GRU gru = GRU.load(modelFile);
        FeatureNormalizer normalizer = FeatureNormalizer.load(normalizerFile);
        return new LoadedModel(gru, normalizer);
    }

    public record LoadedModel(GRU gru, FeatureNormalizer normalizer) {
    }

    public record Result(boolean success, String message) {
    }
}
