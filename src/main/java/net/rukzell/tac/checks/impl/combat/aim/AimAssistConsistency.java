package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.Logger;
import net.rukzell.tac.utils.SampleBuffer;
import net.rukzell.tac.utils.buffer.VlBuffer;
import net.rukzell.tac.utils.math.MathUtil;

public class AimAssistConsistency extends Check {
    public AimAssistConsistency(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            VlBuffer bufferYawDistinct = player.getBuffer("AimAssistConsistency:yawDistinct");
            VlBuffer bufferPitchDistinct = player.getBuffer("AimAssistConsistency:pitchDistinct");

            SampleBuffer yawBufferStd = player.getSampleBuffer(getName() + ":yawStd", 20);
            SampleBuffer pitchBufferStd = player.getSampleBuffer(getName() + ":yawStd", 20);
            SampleBuffer yawBufferDistinct = player.getSampleBuffer(getName() + ":yawDistinct", 10);
            SampleBuffer pitchBufferDistinct = player.getSampleBuffer(getName() + ":pitchDistinct", 10);

            yawBufferStd.add(Math.abs(player.getDeltaYaw()));
            pitchBufferStd.add(Math.abs(player.getDeltaPitch()));
            yawBufferDistinct.add(Math.abs(player.getDeltaYaw()));
            pitchBufferDistinct.add(Math.abs(player.getDeltaPitch()));

            if (yawBufferStd.isFull()) {
                if (MathUtil.stddev(yawBufferStd.getValues()) == 0) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevXAxis)");
                }

                yawBufferStd.getValues().clear();
            }

            if (pitchBufferStd.isFull()) {
                if (MathUtil.stddev(pitchBufferStd.getValues()) == 0) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevYAxis)");
                }

                pitchBufferStd.getValues().clear();
            }

            if (yawBufferDistinct.isFull()) {
                int distinct = MathUtil.distinct(yawBufferDistinct.getValues());
                if (distinct < 8) {
                    bufferYawDistinct.fail(1);
                } else {
                    bufferYawDistinct.decay(0.25);
                }

                if (bufferYawDistinct.getVl() > 3) {
                    flag(player);
                    bufferYawDistinct.setVl(0);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(DistinctXAxis)");
                }
                yawBufferDistinct.getValues().clear();
            }

            if (pitchBufferDistinct.isFull()) {
                int distinct = MathUtil.distinct(pitchBufferDistinct.getValues());
                if (distinct < 8) {
                    bufferPitchDistinct.fail(1);
                } else {
                    bufferPitchDistinct.decay(0.25);
                }

                if (bufferPitchDistinct.getVl() > 3) {
                    flag(player);
                    bufferPitchDistinct.setVl(0);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(DistinctYAxis)");
                }
                pitchBufferDistinct.getValues().clear();
            }
        }
    }
}
