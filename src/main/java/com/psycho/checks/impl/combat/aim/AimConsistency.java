package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

public class AimConsistency extends Check {
    public AimConsistency(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            VlBuffer bufferYawDistinct = player.getBuffer("AimAssistConsistency:yawDistinct");
            VlBuffer bufferPitchDistinct = player.getBuffer("AimAssistConsistency:pitchDistinct");

            SampleBuffer yawBufferStd = player.getSampleBuffer(getName() + ":yawStd", 20);
            SampleBuffer pitchBufferStd = player.getSampleBuffer(getName() + ":pitchStd", 20);
            SampleBuffer yawBufferDistinct = player.getSampleBuffer(getName() + ":yawDistinct", 10);
            SampleBuffer pitchBufferDistinct = player.getSampleBuffer(getName() + ":pitchDistinct", 10);
            VlBuffer bufferYawMonotonic = player.getBuffer(getName() + ":yawMonotonic");
            VlBuffer bufferPitchMonotonic = player.getBuffer(getName() + ":pitchMonotonic");

            yawBufferStd.add(Math.abs(player.getDeltaYaw()));
            pitchBufferStd.add(Math.abs(player.getDeltaPitch()));
            yawBufferDistinct.add(Math.abs(player.getDeltaYaw()));
            pitchBufferDistinct.add(Math.abs(player.getDeltaPitch()));

            if (Math.abs(player.getDeltaYaw()) < Math.abs(player.getLastDeltaYaw())) {
                bufferYawMonotonic.fail(1);
                if (bufferYawMonotonic.getVl() > 10) {
                    flag(player);
                    bufferYawMonotonic.setVl(0);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(MonotonicXAxis)");
                }
            } else {
                bufferYawMonotonic.decay(2);
            }

            if (Math.abs(player.getDeltaPitch()) < Math.abs(player.getLastDeltaPitch())) {
                bufferPitchMonotonic.fail(1);
                if (bufferPitchMonotonic.getVl() > 10) {
                    flag(player);
                    bufferPitchMonotonic.setVl(0);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(MonotonicYAxis)");
                }
            } else {
                bufferPitchMonotonic.decay(2);
            }

            if (yawBufferStd.isFull()) {
                if (MathUtil.stddev(yawBufferStd.getValues()) < 0.05) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevXAxis)");
                }

                yawBufferStd.getValues().clear();
            }

            if (pitchBufferStd.isFull()) {
                if (MathUtil.stddev(pitchBufferStd.getValues()) < 0.05) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevYAxis)");
                }

                pitchBufferStd.getValues().clear();
            }

            if (yawBufferDistinct.isFull()) {
                int distinct = MathUtil.distinct(yawBufferDistinct.getValues());
                double avg = MathUtil.average(yawBufferDistinct.getValues());

                if (distinct < 8 && avg > 0.5) {
                    bufferYawDistinct.fail(1);
                } else {
                    bufferYawDistinct.decay(0.3);
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
                double avg = MathUtil.average(pitchBufferDistinct.getValues());

                if (distinct < 8 && avg > 0.5) {
                    bufferPitchDistinct.fail(1);
                } else {
                    bufferPitchDistinct.decay(0.3);
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
