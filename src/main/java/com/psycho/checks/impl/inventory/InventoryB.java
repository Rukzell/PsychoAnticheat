package com.psycho.checks.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;

public class InventoryB extends Check {
    public InventoryB(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (!getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (player.getClickDelay() < 2) {
                flag(player);
            }
        }
    }
}
