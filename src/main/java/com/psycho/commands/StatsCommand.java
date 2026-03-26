package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.player.PlayerStats;
import com.psycho.player.PsychoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        sender.sendMessage("§fStats for §a" + targetPlayer.getName());
        sender.sendMessage("§aFailed Checks: §c" + (!result.isEmpty() ? result : "None"));
        sender.sendMessage("§aTotal: " + playerStats.getFailedChecks().size());
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
