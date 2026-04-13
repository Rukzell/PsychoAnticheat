package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.ml.DataCollector;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        if (args.length == 2 && args[1].equalsIgnoreCase("status")) {
            sendStatus(sender);
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /psycho collect <player> <legit|cheat|stop> or /psycho collect status");
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

    private void sendStatus(CommandSender sender) {
        Map<UUID, DataCollector.SessionStatus> activeCollections = DataCollector.getActiveCollections();
        if (activeCollections.isEmpty()) {
            sender.sendMessage("§eThere are no active data collection sessions.");
            return;
        }

        sender.sendMessage("§aActive data collection sessions: §f" + activeCollections.size());

        activeCollections.entrySet().stream()
                .sorted(Comparator.comparing(entry -> resolveTargetName(entry.getKey())))
                .forEach(entry -> sender.sendMessage(
                        "§7- §f" + resolveTargetName(entry.getKey())
                                + " §7(" + labelName(entry.getValue().label()) + "§7)"
                                + " §7- ticks: §f" + entry.getValue().collectedTicks()
                ));
    }

    private String resolveTargetName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }

        return uuid.toString();
    }

    private String labelName(int label) {
        return label == 1 ? "cheat" : "legit";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            if ("status".startsWith(args[1].toLowerCase())) {
                completions.add("status");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("status")) {
                return completions;
            }
            String input = args[2].toLowerCase();
            if ("legit".startsWith(input)) completions.add("legit");
            if ("cheat".startsWith(input)) completions.add("cheat");
            if ("stop".startsWith(input)) completions.add("stop");
        }
        return completions;
    }
}
