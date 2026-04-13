package com.psycho.tracker;

import com.psycho.checks.impl.combat.aim.ml.AimAssistML;
import com.psycho.player.PsychoPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackedPlayer {
    private final UUID uuid;
    private final long firstSeenAt;
    private String lastKnownName;
    private boolean online;
    private long lastJoinAt;
    private long lastQuitAt;
    private long lastUpdateAt;
    private long updateCount;
    private int sessionCount;
    private int peakCps;
    private int peakTotalViolations;
    private int peakFailedChecks;
    private double peakMlProbability;
    private Snapshot snapshot;

    public TrackedPlayer(Player player, PsychoPlayer psychoPlayer, long now, boolean online) {
        this.uuid = player.getUniqueId();
        this.firstSeenAt = now;
        this.lastKnownName = player.getName();
        this.online = online;
        this.lastJoinAt = online ? now : 0L;
        this.lastQuitAt = online ? 0L : now;
        this.lastUpdateAt = now;
        this.updateCount = 1L;
        this.sessionCount = online ? 1 : 0;
        this.snapshot = Snapshot.capture(player, psychoPlayer, now);
        refreshPeaks();
    }

    public synchronized void markJoin(PsychoPlayer psychoPlayer) {
        long now = System.currentTimeMillis();
        Player player = psychoPlayer.getBukkitPlayer();
        this.lastKnownName = player.getName();
        this.online = true;
        this.lastJoinAt = now;
        this.sessionCount++;
        updateFrom(psychoPlayer, now);
    }

    public synchronized void updateFrom(PsychoPlayer psychoPlayer) {
        updateFrom(psychoPlayer, System.currentTimeMillis());
    }

    public synchronized void markQuit(Player player, PsychoPlayer psychoPlayer) {
        long now = System.currentTimeMillis();
        this.lastKnownName = player.getName();
        this.snapshot = Snapshot.capture(player, psychoPlayer, now);
        this.online = false;
        this.lastQuitAt = now;
        this.lastUpdateAt = now;
        this.updateCount++;
        refreshPeaks();
    }

    private void updateFrom(PsychoPlayer psychoPlayer, long now) {
        Player player = psychoPlayer.getBukkitPlayer();
        this.lastKnownName = player.getName();
        this.snapshot = Snapshot.capture(player, psychoPlayer, now);
        this.lastUpdateAt = now;
        this.updateCount++;
        refreshPeaks();
    }

    private void refreshPeaks() {
        if (snapshot == null) {
            return;
        }

        peakCps = Math.max(peakCps, snapshot.cps());
        peakTotalViolations = Math.max(peakTotalViolations, snapshot.totalViolations());
        peakFailedChecks = Math.max(peakFailedChecks, snapshot.totalFailedChecks());
        peakMlProbability = Math.max(peakMlProbability, snapshot.mlMaxProbability());
    }

    public synchronized UUID getUuid() {
        return uuid;
    }

    public synchronized String getLastKnownName() {
        return lastKnownName;
    }

    public synchronized boolean isOnline() {
        return online;
    }

    public synchronized long getLastUpdateAt() {
        return lastUpdateAt;
    }

    public synchronized long getLastJoinAt() {
        return lastJoinAt;
    }

    public synchronized long getLastQuitAt() {
        return lastQuitAt;
    }

    public synchronized long getFirstSeenAt() {
        return firstSeenAt;
    }

    public synchronized long getUpdateCount() {
        return updateCount;
    }

    public synchronized int getSessionCount() {
        return sessionCount;
    }

    public synchronized int getPeakCps() {
        return peakCps;
    }

    public synchronized int getPeakTotalViolations() {
        return peakTotalViolations;
    }

    public synchronized int getPeakFailedChecks() {
        return peakFailedChecks;
    }

    public synchronized double getPeakMlProbability() {
        return peakMlProbability;
    }

    public synchronized Snapshot getSnapshot() {
        return snapshot;
    }

    public synchronized double getRiskScore() {
        if (snapshot == null) {
            return 0.0;
        }

        return snapshot.totalViolations() * 4.0
                + snapshot.totalFailedChecks() * 1.5
                + snapshot.cps() * 2.0
                + snapshot.mlAverageProbability() * 100.0
                + snapshot.mlMaxProbability() * 50.0
                + (snapshot.sprinting() ? 5.0 : 0.0);
    }

    public record Snapshot(
            long capturedAt,
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            double deltaX,
            double deltaY,
            double deltaZ,
            float deltaYaw,
            float deltaPitch,
            int cps,
            boolean sprinting,
            long sprintDelayNanos,
            long timeSinceLastHitMillis,
            long lastFlyingMillis,
            long clickDelayMillis,
            int hitCancelTicks,
            boolean sendAlerts,
            double sensitivity,
            double mcpSensitivity,
            double finalSensitivity,
            boolean validSensitivity,
            int totalViolations,
            Map<String, Integer> violations,
            int totalFailedChecks,
            List<String> recentFailedChecks,
            double mlAverageProbability,
            double mlMaxProbability
    ) {
        public static Snapshot capture(Player player, PsychoPlayer psychoPlayer, long now) {
            Location location = player.getLocation();

            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            double deltaX = 0.0;
            double deltaY = 0.0;
            double deltaZ = 0.0;
            float deltaYaw = 0.0F;
            float deltaPitch = 0.0F;
            int cps = 0;
            boolean sprinting = false;
            long sprintDelayNanos = Long.MAX_VALUE;
            long timeSinceLastHitMillis = Long.MAX_VALUE;
            long lastFlyingMillis = 0L;
            long clickDelayMillis = -1L;
            int hitCancelTicks = 0;
            boolean sendAlerts = false;
            double sensitivity = 0.0;
            double mcpSensitivity = 0.0;
            double finalSensitivity = 0.0;
            boolean validSensitivity = false;
            int totalViolations = 0;
            Map<String, Integer> violations = Map.of();
            int totalFailedChecks = 0;
            List<String> recentFailedChecks = List.of();
            double mlAverageProbability = 0.0;
            double mlMaxProbability = 0.0;

            if (psychoPlayer != null) {
                x = psychoPlayer.getX() == 0.0 && psychoPlayer.getLastX() == 0.0 ? x : psychoPlayer.getX();
                y = psychoPlayer.getY() == 0.0 && psychoPlayer.getLastY() == 0.0 ? y : psychoPlayer.getY();
                z = psychoPlayer.getZ() == 0.0 && psychoPlayer.getLastZ() == 0.0 ? z : psychoPlayer.getZ();
                yaw = psychoPlayer.getYaw() == 0.0F && psychoPlayer.getLastYaw() == 0.0F ? yaw : psychoPlayer.getYaw();
                pitch = psychoPlayer.getPitch() == 0.0F && psychoPlayer.getLastPitch() == 0.0F ? pitch : psychoPlayer.getPitch();
                deltaX = psychoPlayer.getDeltaX();
                deltaY = psychoPlayer.getDeltaY();
                deltaZ = psychoPlayer.getDeltaZ();
                deltaYaw = psychoPlayer.getDeltaYaw();
                deltaPitch = psychoPlayer.getDeltaPitch();
                cps = psychoPlayer.getCps();
                sprinting = psychoPlayer.isSprinting();
                sprintDelayNanos = psychoPlayer.getSprintDelay();
                timeSinceLastHitMillis = psychoPlayer.getTimeSinceLastHit();
                lastFlyingMillis = psychoPlayer.getLastFlying();
                clickDelayMillis = psychoPlayer.getClickDelay();
                hitCancelTicks = psychoPlayer.getHitCancelTicks();
                sendAlerts = psychoPlayer.isSendAlerts();
                sensitivity = psychoPlayer.getSensitivity();
                mcpSensitivity = psychoPlayer.getMcpSensitivity();
                finalSensitivity = psychoPlayer.getFinalSensitivity();
                validSensitivity = psychoPlayer.hasValidSensitivity();
                violations = copyViolations(psychoPlayer.getViolations());
                totalViolations = violations.values().stream().mapToInt(Integer::intValue).sum();
                totalFailedChecks = psychoPlayer.getStats().getFailedChecks().size();
                recentFailedChecks = recentFailedChecks(psychoPlayer.getStats().getFailedChecks(), 5);

                AimAssistML aimAssistML = psychoPlayer.getCheck(AimAssistML.class);
                if (aimAssistML != null) {
                    Deque<Double> history = aimAssistML.getAvgHistory();
                    mlAverageProbability = history.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    mlMaxProbability = history.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                }
            }

            return new Snapshot(
                    now,
                    player.getWorld().getName(),
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    deltaX,
                    deltaY,
                    deltaZ,
                    deltaYaw,
                    deltaPitch,
                    cps,
                    sprinting,
                    sprintDelayNanos,
                    timeSinceLastHitMillis,
                    lastFlyingMillis,
                    clickDelayMillis,
                    hitCancelTicks,
                    sendAlerts,
                    sensitivity,
                    mcpSensitivity,
                    finalSensitivity,
                    validSensitivity,
                    totalViolations,
                    violations,
                    totalFailedChecks,
                    recentFailedChecks,
                    mlAverageProbability,
                    mlMaxProbability
            );
        }

        private static Map<String, Integer> copyViolations(Map<String, Integer> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }

            LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
            source.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
            return Collections.unmodifiableMap(ordered);
        }

        private static List<String> recentFailedChecks(List<String> failedChecks, int limit) {
            if (failedChecks == null || failedChecks.isEmpty()) {
                return List.of();
            }

            int fromIndex = Math.max(0, failedChecks.size() - limit);
            return List.copyOf(new ArrayList<>(failedChecks.subList(fromIndex, failedChecks.size())));
        }
    }
}
