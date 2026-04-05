package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.ml.DataCollector;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CollectCommand implements SubCommand {
    private final Psycho plugin;

    public CollectCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "collect";
    }

    @Override
    public String getPermission() {
        return "psycho.command.collect";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /psycho collect <player> <legit|cheat|stop>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        String type = args[2].toLowerCase();

        if (type.equals("stop")) {
            if (DataCollector.isCollecting(target.getUniqueId())) {
                DataCollector.stopCollecting(target.getUniqueId());
                sender.sendMessage("§aStopped collecting data for " + target.getName());
            } else {
                sender.sendMessage("§cPlayer is not currently participating in data collection.");
            }
        } else if (type.equals("legit")) {
            if (!DataCollector.isCollecting(target.getUniqueId())) {
                DataCollector.startCollecting(target.getUniqueId(), 0);
                sender.sendMessage("§aStarted collecting legit data for " + target.getName());
            } else {
                sender.sendMessage("§cPlayer is already participating in data collection.");
            }
        } else if (type.equals("cheat")) {
            if (!DataCollector.isCollecting(target.getUniqueId())) {
                DataCollector.startCollecting(target.getUniqueId(), 1);
                sender.sendMessage("§aStarted collecting cheat data for " + target.getName());
            } else {
                sender.sendMessage("§cPlayer is already participating in data collection.");
            }
        } else {
            sender.sendMessage("§cInvalid type. Use <legit|cheat|stop>");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            String input = args[2].toLowerCase();
            if ("legit".startsWith(input)) completions.add("legit");
            if ("cheat".startsWith(input)) completions.add("cheat");
            if ("stop".startsWith(input)) completions.add("stop");
        }
        return completions;
    }
}
