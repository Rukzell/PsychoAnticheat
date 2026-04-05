package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.buffer.VlBuffer;

import java.util.ArrayList;
import java.util.List;

public class AimAssistF extends Check {
    private final List<Float> deltaYaws = new ArrayList<>();
    private final List<Float> deltaPitches = new ArrayList<>();
    private final VlBuffer buffer = new VlBuffer();

    public AimAssistF(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            deltaYaws.add(Math.abs(player.getDeltaYaw()));
            deltaPitches.add(Math.abs(player.getDeltaPitch()));

            if (deltaYaws.size() >= 3) {
                if (deltaYaws.get(0) < 0.15 && deltaYaws.get(1) > 20 && deltaYaws.get(2) < 0.15) {
                    buffer.fail(1);
                    if (buffer.getVl() > 1) {
                        flag("spike(x)=" + deltaYaws.get(1));
                    }
                } else {
                    buffer.decay(0.05);
                }
                deltaYaws.remove(0);
            }

            if (deltaPitches.size() >= 3) {
                if (deltaPitches.get(0) < 0.15 && deltaPitches.get(1) > 20 && deltaPitches.get(2) < 0.15) {
                    buffer.fail(1);
                    if (buffer.getVl() > 1) {
                        flag("spike(y)=" + deltaPitches.get(1));
                    }
                } else {
                    buffer.decay(0.05);
                }
                deltaPitches.remove(0);
            }
        }
    }
}
