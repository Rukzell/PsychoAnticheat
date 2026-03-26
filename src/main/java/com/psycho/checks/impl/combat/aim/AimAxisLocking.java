package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.buffer.VlBuffer;

public class AimAxisLocking extends Check {
    private final VlBuffer bufferYaw = new VlBuffer();
    private final VlBuffer bufferPitch = new VlBuffer();

    public AimAxisLocking(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            if (Math.abs(player.getDeltaYaw()) > 1 && Math.abs(player.getLastDeltaYaw()) < 0.005) {
                bufferYaw.fail(1);
            } else {
                bufferYaw.decay(0.2);
            }

            if (Math.abs(player.getDeltaPitch()) > 1 && Math.abs(player.getLastDeltaPitch()) < 0.005) {
                bufferPitch.fail(1);
            } else {
                bufferPitch.decay(0.2);
            }

            if (bufferYaw.getVl() > 5) {
                flag();
                bufferYaw.setVl(0);
            }

            if (bufferPitch.getVl() > 5) {
                flag();
                bufferPitch.setVl(0);
            }
        }
    }
}
