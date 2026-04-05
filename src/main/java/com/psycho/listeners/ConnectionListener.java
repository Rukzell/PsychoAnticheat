package com.psycho.listeners;

import com.psycho.Psycho;
import com.psycho.hologram.Holograms;
import com.psycho.ml.DataCollector;
import com.psycho.player.PsychoPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener implements Listener {
    private final Map<UUID, PsychoPlayer> players;

    public ConnectionListener() {
        this.players = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PsychoPlayer psychoPlayer = new PsychoPlayer(event.getPlayer());
        players.put(event.getPlayer().getUniqueId(), psychoPlayer);
        Psycho.get().getPlayerTrackerService().trackJoin(psychoPlayer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Holograms h = Psycho.get().getNametagManager();
        if (h != null) h.handlePlayerQuit(event.getPlayer());
        PsychoPlayer player = players.get(event.getPlayer().getUniqueId());
        Psycho.get().getPlayerTrackerService().trackDisconnect(event.getPlayer(), player);

        if (player != null) {
            players.remove(player.getBukkitPlayer().getUniqueId());
        }

        DataCollector.stopCollecting(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Holograms h = Psycho.get().getNametagManager();
        if (h != null) h.handlePlayerQuit(event.getPlayer());
        PsychoPlayer removed = players.remove(event.getPlayer().getUniqueId());
        Psycho.get().getPlayerTrackerService().trackDisconnect(event.getPlayer(), removed);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PsychoPlayer psychoPlayer = players.get(player.getUniqueId());
        if (psychoPlayer == null) {
            return;
        }

        psychoPlayer.registerDamage();
        Psycho.get().getPlayerTrackerService().trackSnapshot(psychoPlayer);
    }

    public void removePlayer(UUID uuid) {
        PsychoPlayer removed = players.remove(uuid);
        Player bukkitPlayer = removed != null ? removed.getBukkitPlayer() : null;
        if (bukkitPlayer != null) {
            Psycho.get().getPlayerTrackerService().trackDisconnect(bukkitPlayer, removed);
        }
    }

    public PsychoPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public Map<UUID, PsychoPlayer> getPlayers() {
        return players;
    }
}
