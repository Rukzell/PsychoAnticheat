package com.psycho.checks.impl.sprint;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class SprintC extends Check {
    public SprintC(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || player.getCps() > 3 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (wrapper.getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                if (player.getBukkitPlayer().isHandRaised()) {
                    flag();
                    setback();
                }
            }
        }
    }
}
