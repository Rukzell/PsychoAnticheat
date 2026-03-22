package com.psycho.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Hex;
import com.psycho.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class Check {
    private final Psycho plugin;
    private final String name;
    private final CheckCfg cfg;
    private final String cfgPath;

    public Check(String cfgPath, CheckCfg cfg) {
        this.plugin = Psycho.get();
        this.name = getClass().getSimpleName();
        this.cfg = cfg;
        this.cfgPath = cfgPath;
    }

    public abstract void handle(PsychoPlayer player, PacketReceiveEvent event);

    public void flag(PsychoPlayer player) {
        flag(player, "");
    }

    public void flag(PsychoPlayer player, String info) {
        long now = System.currentTimeMillis();
        if (now - player.getLastFlagTime() < 1000) return;
        player.setLastFlagTime(now);

        int vl = player.getViolation(name) + 1;
        player.addViolation(name, 1);

        String vlBar = buildVlBar(vl, cfg.vlThreshold());

        if (vl >= cfg.vlThreshold()) {
            Logger.log("executing punishment");
            Player bukkitPlayer = player.getBukkitPlayer();
            plugin.getConnectionListener().removePlayer(bukkitPlayer.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(plugin.getServer().getConsoleSender(),
                                cfg.punishCommand().replace("{player}", bukkitPlayer.getName()));
                        Logger.log("§a✓ punished");
                    }
            );
        }

        String message = Hex.translateHexColors((plugin.getMessagesCfg().formatAlert(
                player.getBukkitPlayer().getName(),
                name,
                vlBar,
                vl,
                cfg.vlThreshold(),
                info
        )));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("psycho.admin")) {
                online.sendMessage(message);
            }
        }
    }

    public void setback(PsychoPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        Location safe = player.getLastSafeLocation();

        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        if (player.getLastSafeLocation() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                bukkitPlayer.teleport(safe);
                bukkitPlayer.setVelocity(new Vector(0, 0, 0));

                Location modified = safe.clone();

                bukkitPlayer.teleport(modified);
            });
        }
    }

    private String buildVlBar(int vl, int maxVl) {
        int totalBars = 10;
        int filled = Math.min((int) Math.round((double) vl / maxVl * totalBars), totalBars);

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                if (i < totalBars * 0.4) bar.append("§a|");
                else if (i < totalBars * 0.7) bar.append("§e|");
                else bar.append("§c|");
            } else {
                bar.append("§8|");
            }
        }
        bar.append("§7]");
        return bar.toString();
    }

    public String getName() {
        return name;
    }

    public CheckCfg getCfg() {
        return cfg;
    }

    public String getCfgPath() {
        return cfgPath;
    }
}
