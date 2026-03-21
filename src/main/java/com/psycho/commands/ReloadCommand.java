package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
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
        var configService = plugin.getConfigService();
        var checkService = plugin.getCheckService();
        plugin.reloadConfig();
        plugin.getCheckService().reload();
        for (Check check : checkService.getRegisteredChecks()) {
            CheckCfg newCfg = configService.loadCheck(check.getCfgPath(), 10);
            check.getCfg().updateFromConfig(newCfg.vlThreshold(), newCfg.punishCommand(), newCfg.decay(), newCfg.bufferThreshold(), newCfg.probThreshold(), newCfg.enabled());
        }
        sender.sendMessage("§aPsycho reloaded.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}