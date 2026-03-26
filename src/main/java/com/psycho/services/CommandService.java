package com.psycho.services;

import com.psycho.Psycho;
import com.psycho.commands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandService implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final Psycho plugin;

    public CommandService(Psycho plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        register(new ReloadCommand(plugin));
        register(new CollectCommand(plugin));
        register(new TrainCommand(plugin));
        register(new StatsCommand(plugin));
    }

    private void register(SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§c/psycho help");
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());

        if (sub == null) {
            sender.sendMessage("§cUnknown subcommand.");
            return true;
        }

        if (!sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(plugin.getMessagesCfg().noPermission());
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (SubCommand sub : subCommands.values()) {
                if (!sender.hasPermission(sub.getPermission())) continue;

                if (sub.getName().toLowerCase().startsWith(input)) {
                    completions.add(sub.getName());
                }
            }
        }

        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.tabComplete(sender, args);
            }
        }

        return completions;
    }
}