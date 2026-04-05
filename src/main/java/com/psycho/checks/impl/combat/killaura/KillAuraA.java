package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.BoundingBox;
import com.psycho.utils.KillAuraUtils;
import com.psycho.utils.buffer.VlBuffer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class KillAuraA extends Check {

    private final VlBuffer buffer = new VlBuffer();

    public KillAuraA(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        if (player.getCps() > 3) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        Player attacker = player.getBukkitPlayer();
        Player target = KillAuraUtils.getTarget(wrapper.getEntityId(), attacker);

        if (target == null || target.equals(attacker)) return;

        PsychoPlayer targetData = Psycho.get().getConnectionListener().getPlayer(target.getUniqueId());
        if (targetData == null) return;

        BoundingBox box = targetData.getInterpolatedBox(0.5).expand(0, 0.5, 0);

        Location eyeLocation = attacker.getEyeLocation();
        Location feetLocation = attacker.getLocation().add(0, 0.1, 0);

        List<Vector> points = getBoxPoints(box);

        int blockedRays = 0;
        int totalRays = points.size();

        for (Vector point : points) {

            boolean eyeBlocked = isRayBlocked(attacker, eyeLocation, point);
            boolean feetBlocked = isRayBlocked(attacker, feetLocation, point);

            if (eyeBlocked && feetBlocked) {
                blockedRays++;
            }
        }

        boolean anyVisible = blockedRays < totalRays;

        if (!anyVisible) {
            buffer.fail(1);

            if (buffer.getVl() > 2) {
                flag();
                setback();
                cancelHits();
            }

        } else {
            buffer.decay(0.5);
        }
    }

    private boolean isRayBlocked(Player player, Location from, Vector to) {
        Vector direction = to.clone().subtract(from.toVector());
        double distance = direction.length();

        if (distance <= 0) return false;

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                from,
                direction.normalize(),
                distance,
                FluidCollisionMode.NEVER,
                true
        );

        if (result != null && result.getHitBlock() != null) {
            Material mat = result.getHitBlock().getType();

            return !mat.name().contains("SLAB");
        }

        return false;
    }

    private List<Vector> getBoxPoints(BoundingBox box) {
        List<Vector> points = new ArrayList<>();

        double minX = box.getMinX();
        double minY = box.getMinY();
        double minZ = box.getMinZ();
        double maxX = box.getMaxX();
        double maxY = box.getMaxY();
        double maxZ = box.getMaxZ();

        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        double midZ = (minZ + maxZ) / 2;

        // head
        points.add(new Vector((box.getMinX() + box.getMaxX()) / 2, box.getMaxY() - 0.1, (box.getMinZ() + box.getMaxZ()) / 2));

        // center
        points.add(new Vector(midX, midY, midZ));

        // bottom
        points.add(new Vector(minX, minY, minZ));
        points.add(new Vector(minX, minY, maxZ));
        points.add(new Vector(maxX, minY, minZ));
        points.add(new Vector(maxX, minY, maxZ));

        // top
        points.add(new Vector(minX, maxY, minZ));
        points.add(new Vector(minX, maxY, maxZ));
        points.add(new Vector(maxX, maxY, minZ));
        points.add(new Vector(maxX, maxY, maxZ));

        return points;
    }
}