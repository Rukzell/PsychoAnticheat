package com.psycho.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.ml.DataCollector;
import com.psycho.utils.BoundingBox;
import com.psycho.utils.math.MathUtil;
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
    private final List<Integer> sensitivitySamples = new ArrayList<>(40);
    // hits
    private final Deque<Long> hitTimestamps;
    private final Deque<Long> hitDelays;
    private int hitCancelTicks;
    private double finalSensitivity;
    private double sensitivity;
    private double mcpSensitivity;
    // hitbox
    private BoundingBox boundingBox;
    private BoundingBox lastBoundingBox;
    private long lastHit;
    private long lastDamageTime;

    // rotation
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float deltaYaw;
    private float deltaPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private float lastLastDeltaYaw;
    private float lastLastDeltaPitch;
    private float accelYaw;
    private float accelPitch;
    private float lastAccelYaw;
    private float lastAccelPitch;
    private float lastLastAccelYaw;
    private float lastLastAccelPitch;
    private float jerkYaw;
    private float jerkPitch;
    private float lastJerkYaw;
    private float lastJerkPitch;
    private float lastLastJerkYaw;
    private float lastLastJerkPitch;
    private float lastDeltaXRot;
    private float lastDeltaYRot;

    // sensitivity
    private float sensitivityX;
    private float sensitivityY;
    private float lastSensitivityX;
    private float lastSensitivityY;
    private float deltaSensitivityX;
    private float deltaSensitivityY;

    // position
    private double x, y, z;
    private double lastX, lastY, lastZ;
    private double deltaX, deltaY, deltaZ;

    // flying
    private long lastFlying;

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
        this.sendAlerts = true;
        this.boundingBox = new BoundingBox(
                bukkitPlayer.getLocation().getX(),
                bukkitPlayer.getLocation().getY(),
                bukkitPlayer.getLocation().getZ(),
                0.6,
                1.8
        );
        this.lastBoundingBox = boundingBox;
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
        this.lastLastAccelYaw = this.lastAccelYaw;
        this.lastLastAccelPitch = this.lastAccelPitch;
        this.lastAccelYaw = this.accelYaw;
        this.lastAccelPitch = this.accelPitch;
        this.accelYaw = deltaYaw - lastDeltaYaw;
        this.accelPitch = deltaPitch - lastDeltaPitch;
        this.lastLastJerkYaw = this.lastJerkYaw;
        this.lastLastJerkPitch = this.lastJerkPitch;
        this.lastJerkYaw = this.jerkYaw;
        this.lastJerkPitch = this.jerkPitch;
        this.jerkYaw = accelYaw - lastAccelYaw;
        this.jerkPitch = accelPitch - lastAccelPitch;

        // sens
        if (Math.abs(deltaPitch) == 0 || Math.abs(lastDeltaPitch) == 0) return;

        double gcd = MathUtil.gcd(deltaPitch, lastDeltaPitch);
        double sensitivityModifier = Math.cbrt(0.8333 * gcd);
        double sensitivityStepTwo = 1.666 * sensitivityModifier - 0.3333;
        this.finalSensitivity = sensitivityStepTwo * 200.0;

        sensitivitySamples.add((int) finalSensitivity);

        if (sensitivitySamples.size() == 40) {
            this.sensitivity = MathUtil.getMode(sensitivitySamples);
            if (hasValidSensitivity()) {
                this.mcpSensitivity = sensitivity;
            }
            sensitivitySamples.clear();
        }

        DataCollector.collect(this);
    }

    public void registerPosition(double x, double y, double z) {
        this.lastBoundingBox = new BoundingBox(
                this.x,
                this.y,
                this.z,
                0.6,
                1.8
        );

        this.deltaX = x - this.lastX;
        this.deltaY = y - this.lastY;
        this.deltaZ = z - this.lastZ;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.x = x;
        this.y = y;
        this.z = z;

        if (this.boundingBox == null) {
            this.boundingBox = new BoundingBox(x, y, z, 0.6, 1.8);
        } else {
            this.boundingBox.update(x, y, z, 0.6, 1.8);
        }
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

    public void registerDamage() {
        lastDamageTime = System.currentTimeMillis();
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

    public BoundingBox getMovementBox() {
        double minX = Math.min(boundingBox.getMinX(), lastBoundingBox.getMinX());
        double minY = Math.min(boundingBox.getMinY(), lastBoundingBox.getMinY());
        double minZ = Math.min(boundingBox.getMinZ(), lastBoundingBox.getMinZ());

        double maxX = Math.max(boundingBox.getMaxX(), lastBoundingBox.getMaxX());
        double maxY = Math.max(boundingBox.getMaxY(), lastBoundingBox.getMaxY());
        double maxZ = Math.max(boundingBox.getMaxZ(), lastBoundingBox.getMaxZ());

        BoundingBox box = new BoundingBox(0, 0, 0, 0, 0);
        box.setMinX(minX);
        box.setMinY(minY);
        box.setMinZ(minZ);
        box.setMaxX(maxX);
        box.setMaxY(maxY);
        box.setMaxZ(maxZ);

        return box;
    }

    public BoundingBox getInterpolatedBox(double t) {
        double ix = lastX + (x - lastX) * t;
        double iy = lastY + (y - lastY) * t;
        double iz = lastZ + (z - lastZ) * t;

        return new BoundingBox(ix, iy, iz, 0.6, 1.8);
    }

    public boolean hasValidSensitivity() {
        return mcpSensitivity > 0 && mcpSensitivity <= 200;
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
