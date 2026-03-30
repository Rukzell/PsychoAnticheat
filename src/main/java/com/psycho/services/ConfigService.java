package com.psycho.services;

import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.cfg.MessagesCfg;

public class ConfigService {
    private final Psycho plugin;

    public ConfigService(Psycho plugin) {
        this.plugin = plugin;
    }

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return plugin.getConfig().getDouble(path, def);
    }

    public long getLong(String path, long def) {
        return plugin.getConfig().getLong(path, def);
    }

    public MessagesCfg getMessagesCfg() {
        return new MessagesCfg(
                getString("messages.prefix", "§c[Psycho]"),
                getString("messages.no-permission", "§cYou do not have permission to do this!"),
                getString("messages.alert", "{prefix} #ffe1c9{player} &7› #ff4500{check} &8| §7[#ffe1c9{vlBar}§7] &8| #ff4500{vl}&8/#ff4500{maxVl} &8({info}&8)")
        );
    }

    public CheckCfg loadCheck(String path, int defVl) {
        return new CheckCfg(
                getInt(path + ".vl-threshold", defVl),
                getString(path + ".punish-command", "kick {player} §4Unfair Advantage"),
                getDouble(path + ".decay", 0),
                getDouble(path + ".buffer-threshold", 0),
                getDouble(path + ".prob-threshold", 0.85),
                getBoolean(path + ".enabled", true),
                getLong(path + ".vl-decay-interval", 60) * 1000L
        );
    }
}