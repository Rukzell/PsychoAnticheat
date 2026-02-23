package net.rukzell.tac.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.player.TornadoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class Check {
    private final TornadoAC plugin;
    private final CheckCfg cfg;
    private final String name;

    public Check(CheckCfg cfg) {
        this.plugin = TornadoAC.get();
        this.name = getClass().getSimpleName();
        this.cfg = cfg;
    }

    public abstract void handle(TornadoPlayer player, PacketReceiveEvent event);

    public void flag(TornadoPlayer player) {
        int vl = player.getViolation(name) + 1;
        player.addViolation(name, 1);

        if (vl > cfg.vlThreshold()) {
            Player bukkitPlayer = player.getBukkitPlayer();
            plugin.getConnectionListener().removePlayer(bukkitPlayer.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                bukkitPlayer.kickPlayer("§4Unfair Advantage");
            });
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("tac.admin")) {
                online.sendMessage(plugin.getMessagesCfg().prefix() +
                        " §7" + player.getBukkitPlayer().getName() +
                        " §cfailed §7" + name +
                        " §cVL: §7" + vl);
            }
        }
    }
}
