package net.rukzell.tac.services;

import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.cfg.MessagesCfg;
import org.bukkit.configuration.Configuration;

public class ConfigService {
    private final TornadoAC plugin;
    private final Configuration configuration;

    public ConfigService(TornadoAC plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfig();
    }

    public String getString(String path, String def) {
        return configuration.getString(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return configuration.getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return configuration.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return configuration.getDouble(path, def);
    }

    public long getLong(String path, long def) {
        return configuration.getLong(path, def);
    }

    public MessagesCfg getMessagesCfg() {
        return new MessagesCfg(
                getString("messages.prefix", "§c[TAC]"),
                getString("messages.no-permission", "§cYou do not have permission to do this!")
        );
    }

    public CheckCfg loadCheck(String path, int defVl) {
        return new CheckCfg(
                getInt(path + ".vl-threshold", defVl),
                getString(path + ".punish-command", "kick {player} §4Unfair Advantage"),
                getBoolean(path + ".enabled", true)
        );
    }

    public void reload() {
        plugin.reloadConfig();
    }
}