package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.checks.impl.ml.AimML;
import com.psycho.player.PlayerStats;
import com.psycho.player.PsychoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCommand implements SubCommand {
    private final Psycho plugin;

    public StatsCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getPermission() {
        return "psycho.command.stats";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /psycho stats <player>");
            return;
        }

        Player targetPlayer = plugin.getServer().getPlayerExact(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer §f" + args[1] + " §cnot found or offline.");
            return;
        }

        PsychoPlayer psychoPlayer = plugin.getConnectionListener().getPlayer(targetPlayer.getUniqueId());
        if (psychoPlayer == null) {
            sender.sendMessage("§cNo stats available for §f" + args[1]);
            return;
        }

        PlayerStats playerStats = psychoPlayer.getStats();

        Map<String, Integer> counts = new HashMap<>();
        for (String check : playerStats.getFailedChecks()) {
            counts.put(check, counts.getOrDefault(check, 0) + 1);
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!result.isEmpty()) result.append(", ");
            result.append(entry.getKey()).append("(x").append(entry.getValue()).append(")");
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§aStats for §f" + targetPlayer.getName());
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§aFailed Checks: §f" + (!result.isEmpty() ? result : "None"));
        sender.sendMessage("§aTotal: §f" + playerStats.getFailedChecks().size());

        AimML aimML = psychoPlayer.getCheck(AimML.class);
        if (aimML != null) {
            Deque<Double> history = aimML.getProbHistory();
            double avg = history.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            String avgColor = avg > 0.8 ? "§c" : avg > 0.5 ? "§e" : "§a";

            int totalBars = 29;
            int filled = (int) (avg * totalBars);
            StringBuilder bar = new StringBuilder("§7[");
            for (int i = 0; i < totalBars; i++) {
                if (i < filled) bar.append(avgColor).append("|");
                else bar.append("§8|");
            }
            bar.append("§7]");

            sender.sendMessage("§8§m                                        ");
            sender.sendMessage("§aML avg prob: " + avgColor + String.format("%.2f", avg) + " §7" + bar);
            sender.sendMessage("§aML max prob: §f" + String.format("%.2f", max));
        } else {
            sender.sendMessage("§bAimML: §7no data");
        }

        sender.sendMessage("§8§m                                        ");
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
        }
        return completions;
    }
}
