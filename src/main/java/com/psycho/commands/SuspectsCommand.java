package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.menu.SuspectsMenu;
import com.psycho.utils.Hex;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SuspectsCommand implements SubCommand {
    private final Psycho plugin;

    public SuspectsCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "suspects";
    }

    @Override
    public String getPermission() {
        return "psycho.command.suspects";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Hex.translateHexColors(plugin.getConfigService().getString(
                    "messages.suspects-player-only",
                    "§cOnly players can use this command."
            )));
            return;
        }

        new SuspectsMenu(plugin, player).open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
