package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class AimSpike extends Check {
    private final List<Float> deltaYaws = new ArrayList<>();
    private final List<Float> deltaPitches = new ArrayList<>();

    public AimSpike(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            deltaYaws.add(Math.abs(player.getDeltaYaw()));
            deltaPitches.add(Math.abs(player.getDeltaPitch()));

            if (deltaYaws.size() >= 3) {
                if (deltaYaws.get(0) < 0.2 && deltaYaws.get(1) > 25 && deltaYaws.get(2) < 0.2) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimSpike(XAxis)");
                }
                deltaYaws.remove(0);
            }

            if (deltaPitches.size() >= 3) {
                if (deltaPitches.get(0) < 0.2 && deltaPitches.get(1) > 25 && deltaPitches.get(2) < 0.2) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimSpike(YAxis)");
                }
                deltaPitches.remove(0);
            }
        }
    }
}
