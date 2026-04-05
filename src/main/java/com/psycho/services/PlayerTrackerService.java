package com.psycho.services;

import com.psycho.player.PsychoPlayer;
import com.psycho.tracker.TrackedPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTrackerService {
    private final Map<UUID, TrackedPlayer> trackedPlayers = new ConcurrentHashMap<>();

    public void trackJoin(PsychoPlayer psychoPlayer) {
        Player player = psychoPlayer.getBukkitPlayer();
        trackedPlayers.compute(player.getUniqueId(), (uuid, tracked) -> {
            if (tracked == null) {
                return new TrackedPlayer(player, psychoPlayer, System.currentTimeMillis(), true);
            }

            tracked.markJoin(psychoPlayer);
            return tracked;
        });
    }

    public void trackSnapshot(PsychoPlayer psychoPlayer) {
        Player player = psychoPlayer.getBukkitPlayer();
        trackedPlayers.compute(player.getUniqueId(), (uuid, tracked) -> {
            if (tracked == null) {
                return new TrackedPlayer(player, psychoPlayer, System.currentTimeMillis(), true);
            }

            tracked.updateFrom(psychoPlayer);
            return tracked;
        });
    }

    public void trackDisconnect(Player player, PsychoPlayer psychoPlayer) {
        trackedPlayers.compute(player.getUniqueId(), (uuid, tracked) -> {
            if (tracked == null) {
                tracked = new TrackedPlayer(player, psychoPlayer, System.currentTimeMillis(), false);
            }

            tracked.markQuit(player, psychoPlayer);
            return tracked;
        });
    }

    public TrackedPlayer getTrackedPlayer(UUID uuid) {
        return trackedPlayers.get(uuid);
    }

    public TrackedPlayer findTrackedPlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        return trackedPlayers.values().stream()
                .filter(tracked -> tracked.getLastKnownName() != null)
                .filter(tracked -> tracked.getLastKnownName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> trackedPlayers.values().stream()
                        .filter(tracked -> tracked.getLastKnownName() != null)
                        .filter(tracked -> tracked.getLastKnownName().toLowerCase().startsWith(name.toLowerCase()))
                        .findFirst()
                        .orElse(null));
    }

    public Collection<TrackedPlayer> getTrackedPlayers() {
        return trackedPlayers.values();
    }

    public List<TrackedPlayer> getTrackedPlayersSorted() {
        List<TrackedPlayer> result = new ArrayList<>(trackedPlayers.values());
        result.sort(Comparator
                .comparing(TrackedPlayer::isOnline).reversed()
                .thenComparingDouble(TrackedPlayer::getRiskScore).reversed()
                .thenComparingLong(TrackedPlayer::getLastUpdateAt).reversed()
                .thenComparing(TrackedPlayer::getLastKnownName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public int getOnlineTrackedCount() {
        return (int) trackedPlayers.values().stream().filter(TrackedPlayer::isOnline).count();
    }
}
