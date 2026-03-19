package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;

public class KillAuraSnap extends Check {
    public KillAuraSnap(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (!getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                float deltaYaw = player.getDeltaYaw();
                float deltaYawAbs = Math.abs(player.getDeltaYaw());
                float deltaPitchAbs = Math.abs(player.getDeltaPitch());
                float lastDeltaYaw = player.getLastDeltaYaw();
                float lastDeltaYawAbs = Math.abs(lastDeltaYaw);
                float lastDeltaPitchAbs = Math.abs(player.getLastDeltaPitch());

                boolean snap1 = lastDeltaYawAbs < 1.5 && deltaYawAbs > 60;
                boolean snap2 = lastDeltaPitchAbs < 1.5 && deltaPitchAbs > 80;
                boolean snap3 = lastDeltaYaw < 0 && deltaYaw > 50 || lastDeltaYaw > 0 && deltaYaw < -50;

                if (snap1) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for KillAuraSnap(1)");
                }

                if (snap2) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for KillAuraSnap(2)");
                }

                if (snap3) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for KillAuraSnap(3)");
                }
            }
        }
    }
}
