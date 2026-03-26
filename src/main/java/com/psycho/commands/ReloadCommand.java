package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.services.ConfigService;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {
    private final Psycho plugin;

    public ReloadCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "psycho.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();

        plugin.getCheckService().reload();

        ConfigService cfg = plugin.getConfigService();

        for (PsychoPlayer psychoPlayer : plugin.getConnectionListener().getPlayers().values()) {
            for (Check check : psychoPlayer.getChecks()) {
                CheckCfg newCfg = cfg.loadCheck(check.getCfgPath(), 10);
                check.getCfg().updateFromConfig(
                        newCfg.vlThreshold(),
                        newCfg.punishCommand(),
                        newCfg.decay(),
                        newCfg.bufferThreshold(),
                        newCfg.probThreshold(),
                        newCfg.enabled(),
                        newCfg.vlDecayInterval()
                );
            }
        }

        sender.sendMessage("§aPsycho reloaded.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}