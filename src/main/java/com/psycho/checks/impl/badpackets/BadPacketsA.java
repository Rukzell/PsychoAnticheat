package com.psycho.checks.impl.badpackets;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class BadPacketsA extends Check {
    public BadPacketsA(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            if (Math.abs(player.getPitch()) > 90.2) {
                flag(player);
            }
        }
    }
}
