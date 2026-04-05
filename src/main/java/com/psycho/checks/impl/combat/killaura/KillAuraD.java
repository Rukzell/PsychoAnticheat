package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class KillAuraD extends Check {
    public KillAuraD(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
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
                boolean snap3 = lastDeltaYaw < -2 && deltaYaw > 50 || lastDeltaYaw > 2 && deltaYaw < -50;

//                player.getBukkitPlayer().sendMessage("1(" + player.getDeltaYaw() + ")" + "\n2(" + player.getAccelYaw() + ")" + "\n3(" + player.getJerkYaw() + ")");

                if (snap1) {
                    flag("1");
                }

                if (snap2) {
                    flag("2");
                }

                if (snap3) {
                    flag("3");
                }
            }
        }
    }
}
