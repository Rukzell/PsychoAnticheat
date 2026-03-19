package com.psycho.listeners;

import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.checks.impl.ml.AimML;
import com.psycho.player.PsychoPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionListener implements Listener {
    private final Map<UUID, PsychoPlayer> players;

    public ConnectionListener() {
        this.players = new HashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        players.put(event.getPlayer().getUniqueId(), new PsychoPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
        Check check = Psycho.get().getCheckService().getCheck("AimML");
        if (check instanceof AimML) {
            ((AimML) check).cleanupPlayer(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public PsychoPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public Map<UUID, PsychoPlayer> getPlayers() {
        return players;
    }
}
