package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.HitboxUtils;
import com.psycho.utils.buffer.VlBuffer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

public class KillAuraWallHit extends Check {
    private final VlBuffer buffer = new VlBuffer();

    public KillAuraWallHit(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg, true);
    }

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        if (player.getCps() > 3) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int id = wrapper.getEntityId();
        Player attacker = player.getBukkitPlayer();
        Player target = null;

        for (Player e : attacker.getWorld().getPlayers()) {
            if (e.getEntityId() == id) {
                target = e;
                break;
            }
        }

        if (target == null) return;
        if (target.equals(attacker)) return;

        Location eyeLocation = attacker.getEyeLocation();
        Location feetLocation = attacker.getLocation().add(0, 0.1, 0);

        List<Vector> hitboxPoints = HitboxUtils.getHitboxPoints(target, 2, 0.3);

        int blockedRays = 0;
        int totalRays = hitboxPoints.size();

        for (Vector point : hitboxPoints) {

            boolean eyeBlocked = false;
            boolean feetBlocked = false;

            Vector eyeDirection = point.clone().subtract(eyeLocation.toVector());
            double eyeDistance = eyeDirection.length();

            if (eyeDistance > 0) {
                RayTraceResult eyeResult = attacker.getWorld().rayTraceBlocks(
                        eyeLocation,
                        eyeDirection.normalize(),
                        eyeDistance,
                        FluidCollisionMode.NEVER,
                        true
                );

                if (eyeResult != null && eyeResult.getHitBlock() != null) {
                    Material mat = eyeResult.getHitBlock().getType();
                    if (!mat.name().contains("SLAB")) {
                        eyeBlocked = true;
                    }
                }
            }

            Vector feetDirection = point.clone().subtract(feetLocation.toVector());
            double feetDistance = feetDirection.length();

            if (feetDistance > 0) {
                RayTraceResult feetResult = attacker.getWorld().rayTraceBlocks(
                        feetLocation,
                        feetDirection.normalize(),
                        feetDistance,
                        FluidCollisionMode.NEVER,
                        true
                );

                if (feetResult != null && feetResult.getHitBlock() != null) {
                    Material mat = feetResult.getHitBlock().getType();
                    if (!mat.name().contains("SLAB")) {
                        feetBlocked = true;
                    }
                }
            }

            if (eyeBlocked && feetBlocked) {
                blockedRays++;
            }
        }

        if (blockedRays == totalRays) {
            buffer.fail(1);
            if (buffer.getVl() > 2) {
                flag();
                setback();
            }
        } else {
            buffer.decay(0.5);
        }
    }
}