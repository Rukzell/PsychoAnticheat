package net.rukzell.tac.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.player.TornadoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class Check {
    private final TornadoAC plugin;
    private CheckCfg cfg;
    private final String name;
    private String cfgPath;

    public Check(String cfgPath, CheckCfg cfg) {
        this.plugin = TornadoAC.get();
        this.name = getClass().getSimpleName();
        this.cfg = cfg;
        this.cfgPath = cfgPath;
    }

    public abstract void handle(TornadoPlayer player, PacketReceiveEvent event);

    public void flag(TornadoPlayer player) {
        long now = System.currentTimeMillis();

        if (now - player.getLastFlagTime() < 1000) return;
        player.setLastFlagTime(now);

        int vl = player.getViolation(name) + 1;
        player.addViolation(name, 1);

        if (vl >= cfg.vlThreshold()) {
            Player bukkitPlayer = player.getBukkitPlayer();
            plugin.getConnectionListener().removePlayer(bukkitPlayer.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(plugin.getServer().getConsoleSender(), cfg.punishCommand().replace("{player}", bukkitPlayer.getName()));
            });
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("tac.admin")) {
                online.sendMessage(plugin.getMessagesCfg().prefix() +
                        " §7" + player.getBukkitPlayer().getName() +
                        " §cfailed §7" + name +
                        " §cVL: §7" + vl +
                        "/" + cfg.vlThreshold()
                        );
            }
        }
    }

    public void setback(TornadoPlayer player) {
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
