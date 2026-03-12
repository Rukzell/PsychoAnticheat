package net.rukzell.tac.player;

import lombok.Data;
import net.rukzell.tac.utils.SampleBuffer;
import net.rukzell.tac.utils.buffer.VlBuffer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Data
public class TornadoPlayer {
    private final Player bukkitPlayer;
    private final Map<String, Integer> violations = new HashMap<>();
    private final Map<String, VlBuffer> buffers = new HashMap<>();
    private final Map<String, SampleBuffer> sampleBuffers = new HashMap<>();
    private long lastFlagTime;

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
    private final Deque<Long> hitDelays;
    private long lastHit;

    // entity action
    private boolean startSprint;
    private boolean stopSprint;
    private long lastSprintPacket = -1L;

    // inventory
    private long lastInventoryClick = -1L;
    private long clickDelay = -1L;

    // setback tracking
    private Location lastSafeLocation;

    public TornadoPlayer(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        this.hitTimestamps = new ArrayDeque<>();
        this.hitDelays = new ArrayDeque<>(10);
        this.lastHit = 0;
        this.lastSafeLocation = bukkitPlayer.getLocation();
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

    public SampleBuffer getSampleBuffer(String key, int size) {
        return sampleBuffers.computeIfAbsent(key, k -> new SampleBuffer(size));
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

    public VlBuffer getBuffer(String checkName) {
        return buffers.computeIfAbsent(checkName, k -> new VlBuffer());
    }

    public void resetViolation(String checkName) {
        violations.remove(checkName);
    }

    public void resetAllViolations() {
        violations.clear();
    }

    public void updateSafeLocation() {
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            this.lastSafeLocation = bukkitPlayer.getLocation().clone();
        }
    }

    public void setSafeLocation(Location location) {
        this.lastSafeLocation = location != null ? location.clone() : null;
    }
}
