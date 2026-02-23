package net.rukzell.tac.services;

import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.cfg.MessagesCfg;
import org.bukkit.configuration.Configuration;

public class ConfigService {
    private final TornadoAC plugin;
    private final Configuration configuration;
    private final String aimAssistCfgPath = "checks.aim-assist";
    private final String badPacketsCfgPath = "checks.bad-packets";
    private final String killAuraCfgPath = "checks.kill-aura";
    private final String sprintCfgPath = "checks.sprint";
    private final String inventoryCfgPath = "checks.inventory";

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
                getString("messages.prefix", "&c[TAC]"),
                getString("messages.no-permission", "&cНет прав")
        );
    }

    public CheckCfg loadAimAssistAngleLockingCfg() {
        return new CheckCfg(
                getInt(aimAssistCfgPath + ".anglelocking.vl-threshold", 5),
                getString(aimAssistCfgPath + ".anglelocking.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadAimAssistConsistencyCfg() {
        return new CheckCfg(
                getInt(aimAssistCfgPath + ".consistency.vl-threshold", 5),
                getString(aimAssistCfgPath + ".consistency.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadAimAssistSpikeCfg() {
        return new CheckCfg(
                getInt(aimAssistCfgPath + ".spike.vl-threshold", 5),
                getString(aimAssistCfgPath + ".spike.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadBadPacketsACfg() {
        return new CheckCfg(
                getInt(badPacketsCfgPath + ".a.vl-threshold", 5),
                getString(badPacketsCfgPath + ".a.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadKillAuraInvalidCfg() {
        return new CheckCfg(
                getInt(killAuraCfgPath + ".invalid.vl-threshold", 5),
                getString(killAuraCfgPath + ".invalid.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadKillAuraSnapCfg() {
        return new CheckCfg(
                getInt(killAuraCfgPath + ".snap.vl-threshold", 5),
                getString(killAuraCfgPath + ".snap.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadSprintACfg() {
        return new CheckCfg(
                getInt(sprintCfgPath + ".a.vl-threshold", 5),
                getString(sprintCfgPath + ".a.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public CheckCfg loadInventoryACfg() {
        return new CheckCfg(
                getInt(inventoryCfgPath + ".a.vl-threshold", 5),
                getString(inventoryCfgPath + ".a.punish-command", "kick {player} §4Unfair Advantage")
        );
    }

    public void reload() {
        plugin.reloadConfig();
    }
}