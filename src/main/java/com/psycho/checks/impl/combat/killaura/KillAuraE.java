package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.KillAuraUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class KillAuraE extends Check {
    private boolean sent = false;

    public KillAuraE(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            long delay = System.currentTimeMillis() - player.getLastFlying();
            if (this.sent) {
                boolean exempt = this.player.getBukkitPlayer().getGameMode() == GameMode.SPECTATOR;
                if (delay > 40L && delay < 100L && !exempt) {
                    flag();
                    setback();
                    cancelHits();
                }

                this.sent = false;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapperPlayClientInteract = new WrapperPlayClientInteractEntity(event);
            if (wrapperPlayClientInteract.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }

            Entity target = KillAuraUtils.getTarget(wrapperPlayClientInteract.getEntityId(), player.getBukkitPlayer());

            if (target != null && target.getType() == EntityType.PLAYER) return;

            long delay2 = System.currentTimeMillis() - player.getLastFlying();
            if (delay2 < 10L) {
                this.sent = true;
            }
        }
    }
}
