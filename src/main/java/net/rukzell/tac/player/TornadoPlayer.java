package net.rukzell.tac.player;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.*;

@Data
public class TornadoPlayer {
    private final Player bukkitPlayer;
    private final Map<String, Integer> violations = new HashMap<>();

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

    // hits
    private final Deque<Long> hitTimestamps;
    private long lastHit;

    public TornadoPlayer(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        this.hitTimestamps = new ArrayDeque<>();
        this.lastHit = 0;
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
    }

    private void purgeOldHits() {
        long now = System.currentTimeMillis();
        while (!hitTimestamps.isEmpty() && now - hitTimestamps.peekFirst() > 1000) {
            hitTimestamps.removeFirst();
        }
    }

    public void registerHit() {
        hitTimestamps.addLast(System.currentTimeMillis());
        lastHit = System.currentTimeMillis();
        purgeOldHits();
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

    public void resetViolation(String checkName) {
        violations.remove(checkName);
    }

    public void resetAllViolations() {
        violations.clear();
    }
}
