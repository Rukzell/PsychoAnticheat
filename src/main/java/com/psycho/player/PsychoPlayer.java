package com.psycho.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.ml.DataCollector;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Data
public class PsychoPlayer {
    private final Player bukkitPlayer;
    private final User user;
    private final PlayerStats stats = new PlayerStats(this);
    private final Map<String, Integer> violations = new HashMap<>();
    private final Map<String, Long> lastDecayTime = new HashMap<>();
    private final List<Check> checks;
    private int hitCancelTicks;

    // hits
    private final Deque<Long> hitTimestamps;
    private final Deque<Long> hitDelays;
    private long lastFlagTime;
    private long lastHit;

    // rotation
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float deltaYaw;
    private float deltaPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private float accelYaw;
    private float accelPitch;
    private float lastAccelYaw;
    private float lastAccelPitch;
    private float jerkYaw;
    private float jerkPitch;

    // position
    private double x, y, z;
    private double lastX, lastY, lastZ;
    private double deltaX, deltaY, deltaZ;

    // ml
    private double avg;

    // entity action
    private boolean startSprint;
    private boolean stopSprint;
    private long lastSprintPacket = -1L;

    // inventory
    private long lastInventoryClick = -1L;
    private long clickDelay = -1L;

    // setback tracking
    private Location lastSafeLocation;

    // alerts
    private boolean sendAlerts;

    public PsychoPlayer(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(bukkitPlayer);
        this.hitTimestamps = new ArrayDeque<>();
        this.hitDelays = new ArrayDeque<>(20);
        this.lastHit = 0;
        this.lastSafeLocation = bukkitPlayer.getLocation();
        this.checks = Psycho.get().getCheckService().createChecksForPlayer(this);
        this.hitCancelTicks = 0;
    }

    public void registerRotation(float yaw, float pitch) {
        this.lastDeltaYaw = this.deltaYaw;
        this.lastDeltaPitch = this.deltaPitch;
        this.deltaYaw = yaw - this.lastYaw;
        this.deltaPitch = pitch - this.lastPitch;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lastAccelYaw = this.accelYaw;
        this.lastAccelPitch = this.accelPitch;
        this.accelYaw = deltaYaw - lastDeltaYaw;
        this.accelPitch = deltaPitch - lastDeltaPitch;
        this.jerkYaw = accelYaw - lastAccelYaw;
        this.jerkPitch = accelPitch - lastAccelPitch;

        this.sendAlerts = true;

        DataCollector.collect(this);
    }

    public void registerPosition(double x, double y, double z) {
        this.deltaX = x - this.lastX;
        this.deltaY = y - this.lastY;
        this.deltaZ = z - this.lastZ;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void registerHit() {
        long now = System.currentTimeMillis();

        if (lastHit > 0) {
            long delay = now - lastHit;

            if (hitDelays.size() >= 10) {
                hitDelays.removeFirst();
            }
            hitDelays.addLast(delay);
        }

        hitTimestamps.addLast(now);
        lastHit = now;

        purgeOldHits();
    }

    public void registerInventoryClick() {
        long now = System.currentTimeMillis();

        clickDelay = now - lastInventoryClick;

        lastInventoryClick = now;
    }

    public void registerStartSprint() {
        startSprint = true;
        stopSprint = false;
    }

    public void registerStopSprint() {
        stopSprint = true;
        startSprint = false;
    }

    public boolean isSprinting() {
        return startSprint && !stopSprint;
    }

    private void purgeOldHits() {
        long now = System.currentTimeMillis();
        while (!hitTimestamps.isEmpty() && now - hitTimestamps.peekFirst() > 1000) {
            hitTimestamps.removeFirst();
        }
    }

    public void updateSprintPacketTime() {
        lastSprintPacket = System.nanoTime();
    }

    public long getSprintDelay() {
        if (lastSprintPacket == -1L) {
            return Long.MAX_VALUE;
        }
        return System.nanoTime() - lastSprintPacket;
    }

    public int getCps() {
        purgeOldHits();
        return hitTimestamps.size();
    }

    public long getTimeSinceLastHit() {
        long now = System.currentTimeMillis();
        return now - lastHit;
    }

    public int getViolation(String checkName) {
        return violations.getOrDefault(checkName, 0);
    }

    public void addViolation(String checkName, int amount) {
        violations.put(checkName, getViolation(checkName) + amount);
    }

    public void setViolation(String checkName, int value) {
        violations.put(checkName, Math.max(0, value));
    }

    public void resetViolationsForCombatChecks() {
        violations.entrySet().removeIf(entry ->
                entry.getKey().startsWith("KillAura") ||
                        entry.getKey().startsWith("Aim"));
    }

    public void updateSafeLocation() {
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            this.lastSafeLocation = bukkitPlayer.getLocation().clone();
        }
    }

    public void setSafeLocation(Location location) {
        this.lastSafeLocation = location != null ? location.clone() : null;
    }

    public long getLastDecayTime(String check) {
        return lastDecayTime.getOrDefault(check, 0L);
    }

    public void setLastDecayTime(String check, long time) {
        lastDecayTime.put(check, time);
    }

    @SuppressWarnings("unchecked")
    public <T extends Check> T getCheck(Class<T> checkClass) {
        for (Check check : checks) {
            if (checkClass.isInstance(check)) {
                return (T) check;
            }
        }
        return null;
    }
}
