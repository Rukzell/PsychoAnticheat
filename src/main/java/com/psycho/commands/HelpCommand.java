package com.psycho.commands;

import com.psycho.Psycho;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements SubCommand {
    private final Psycho plugin;

    public HelpCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return "psycho.command.help";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§aAvailable commands:" +
                "\n§c/psycho reload§7 - Reload the plugin configuration" +
                "\n§c/psycho model <load|unload|reload>§7 - Manage the ML model" +
                "\n§c/psycho alerts§7 - Enable/disable alerts" +
                "\n§c/psycho suspects§7 - Open suspects menu" +
                "\n§c/psycho help§7 - Show this help message" +
                "\n§c/psycho stats {player}§7 - Show stats for player" +
                "\n§c/psycho train {epochs} {learning-rate}§7 - Train model" +
                "\n§c/psycho collect {player} {legit|cheat|stop}§7 - Collect data" +
                "\n§c/psycho collect status§7 - Show active data collection sessions"
        );
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
