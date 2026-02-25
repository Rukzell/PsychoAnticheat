package net.rukzell.tac.commands;

import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {
    private final TornadoAC plugin;

    public ReloadCommand(TornadoAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "tac.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var configService = plugin.getConfigService();
        var checkService = plugin.getCheckService();
        plugin.reloadConfig();
        plugin.getCheckService().reload();
        for (Check check : checkService.getRegisteredChecks()) {
            CheckCfg newCfg = configService.loadCheck(check.getCfgPath(), 10);
            check.getCfg().updateFromConfig(newCfg.vlThreshold(), newCfg.punishCommand(), newCfg.enabled());
        }
        sender.sendMessage("§aTornadoAC reloaded.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}