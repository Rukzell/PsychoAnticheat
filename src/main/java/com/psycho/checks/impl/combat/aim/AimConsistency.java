package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

public class AimConsistency extends Check {
    private final VlBuffer bufferYawDistinct = new VlBuffer();
    private final VlBuffer bufferPitchDistinct = new VlBuffer();
    private final SampleBuffer yawBufferStd = new SampleBuffer(20);
    private final SampleBuffer pitchBufferStd = new SampleBuffer(20);
    private final SampleBuffer yawBufferDistinct = new SampleBuffer(10);
    private final SampleBuffer pitchBufferDistinct = new SampleBuffer(10);
    private final VlBuffer bufferYawMonotonic = new VlBuffer();
    private final VlBuffer bufferPitchMonotonic = new VlBuffer();

    public AimConsistency(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            yawBufferStd.add(Math.abs(player.getDeltaYaw()));
            pitchBufferStd.add(Math.abs(player.getDeltaPitch()));
            yawBufferDistinct.add(Math.abs(player.getDeltaYaw()));
            pitchBufferDistinct.add(Math.abs(player.getDeltaPitch()));

            if (Math.abs(player.getDeltaYaw()) < Math.abs(player.getLastDeltaYaw())) {
                bufferYawMonotonic.fail(1);
                if (bufferYawMonotonic.getVl() > 10) {
                    flag("MonotonicXAxis");
                    bufferYawMonotonic.setVl(0);
                }
            } else {
                bufferYawMonotonic.decay(3);
            }

            if (Math.abs(player.getDeltaPitch()) < Math.abs(player.getLastDeltaPitch())) {
                bufferPitchMonotonic.fail(1);
                if (bufferPitchMonotonic.getVl() > 10) {
                    flag("MonotonicYAxis");
                    bufferPitchMonotonic.setVl(0);
                }
            } else {
                bufferPitchMonotonic.decay(3);
            }

            if (yawBufferStd.isFull()) {
                if (MathUtil.stddev(yawBufferStd.getValues()) < 0.05) {
                    flag("StdDevXAxis");
                }
                yawBufferStd.getValues().clear();
            }

            if (pitchBufferStd.isFull()) {
                if (MathUtil.stddev(pitchBufferStd.getValues()) < 0.05) {
                    flag("StdDevYAxis");
                }
                pitchBufferStd.getValues().clear();
            }

            if (yawBufferDistinct.isFull()) {
                int distinct = MathUtil.distinct(yawBufferDistinct.getValues());
                double avg = MathUtil.average(yawBufferDistinct.getValues());

                if (distinct < 8 && avg > 1) {
                    bufferYawDistinct.fail(1);
                } else {
                    bufferYawDistinct.decay(0.3);
                }

                if (bufferYawDistinct.getVl() > 4) {
                    flag("DistinctXAxis");
                    bufferYawDistinct.setVl(0);
                }
                yawBufferDistinct.getValues().clear();
            }

            if (pitchBufferDistinct.isFull()) {
                int distinct = MathUtil.distinct(pitchBufferDistinct.getValues());
                double avg = MathUtil.average(pitchBufferDistinct.getValues());

                if (distinct < 8 && avg > 1) {
                    bufferPitchDistinct.fail(1);
                } else {
                    bufferPitchDistinct.decay(0.3);
                }

                if (bufferPitchDistinct.getVl() > 4) {
                    flag("DistinctYAxis");
                    bufferPitchDistinct.setVl(0);
                }
                pitchBufferDistinct.getValues().clear();
            }
        }
    }
}
