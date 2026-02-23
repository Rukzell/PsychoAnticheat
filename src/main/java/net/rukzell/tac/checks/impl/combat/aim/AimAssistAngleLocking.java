package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.buffer.VlBuffer;

public class AimAssistAngleLocking extends Check {
    public AimAssistAngleLocking(CheckCfg cfg) {
        super(cfg);
    }

    private final VlBuffer bufferYaw = new VlBuffer();
    private final VlBuffer bufferPitch = new VlBuffer();

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            if (Math.abs(player.getDeltaYaw()) > 1 && Math.abs(player.getLastDeltaYaw()) < 0.01) {
                bufferYaw.fail(1);
            } else {
                bufferYaw.decay(0.05);
            }

            if (Math.abs(player.getDeltaPitch()) > 1 && Math.abs(player.getLastDeltaPitch()) < 0.01) {
                bufferPitch.fail(1);
            } else {
                bufferPitch.decay(0.05);
            }

            if (bufferYaw.getVl() > 5) {
                flag(player);
                bufferYaw.setVl(0);
            }

            if (bufferPitch.getVl() > 5) {
                flag(player);
                bufferPitch.setVl(0);
            }
        }
    }
}
