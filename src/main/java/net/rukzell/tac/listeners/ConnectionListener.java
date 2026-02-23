package net.rukzell.tac.listeners;

import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.player.TornadoPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionListener implements Listener {
    private final Map<UUID, TornadoPlayer> players;

    public ConnectionListener() {
        this.players = new HashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        players.put(event.getPlayer().getUniqueId(), new TornadoPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public TornadoPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public Map<UUID, TornadoPlayer> getPlayers() {
        return players;
    }
}
