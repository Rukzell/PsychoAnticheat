package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class KillAuraF extends Check {
    public KillAuraF(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                double diffJerk = Math.abs(player.getDeltaYaw()) - Math.abs(player.getJerkYaw());
                double diffAccel = Math.abs(player.getDeltaYaw()) - Math.abs(player.getAccelYaw());
                boolean snap1 = Math.abs(player.getDeltaYaw()) > 60 && Math.abs(player.getAccelYaw()) > 60 && Math.abs(player.getJerkYaw()) > 60 && diffJerk < -5 && Math.abs(player.getAccelYaw()) < 80 && Math.abs(player.getJerkYaw()) < 80;
                boolean snap2 = Math.abs(player.getDeltaYaw()) > 30 && Math.abs(player.getAccelYaw()) > 30 && Math.abs(player.getJerkYaw()) > 60 && diffJerk < -10 && diffAccel < -10;

                if (snap1 || snap2) {
                    flag("delta=" + player.getDeltaYaw() + ", accel=" + player.getAccelYaw() + ", jerk=" + player.getJerkYaw());
                }
            }
        }
    }
}
