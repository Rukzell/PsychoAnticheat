package com.psycho.checks.impl.sprint;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class SprintB extends Check {
    public SprintB(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || player.getCps() > 3 || !getCfg().enabled() || player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (wrapper.getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING || wrapper.getAction() == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                if (player.getSprintDelay() < 15_000_000) {
                    flag(player);
                    setback(player);
                }
            }
        }
    }
}
