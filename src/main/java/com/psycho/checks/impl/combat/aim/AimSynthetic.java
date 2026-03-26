package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimSynthetic extends Check {
    private final SampleBuffer yawBufferJerk = new SampleBuffer(60);
    private final SampleBuffer pitchBufferJerk = new SampleBuffer(60);
    private final SampleBuffer yawBuffer = new SampleBuffer(60);
    private final SampleBuffer pitchBuffer = new SampleBuffer(60);

    public AimSynthetic(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            yawBufferJerk.add(player.getJerkYaw());
            pitchBufferJerk.add(player.getJerkPitch());
            yawBuffer.add(player.getDeltaYaw());
            pitchBuffer.add(player.getDeltaPitch());

            if (yawBufferJerk.isFull() && yawBuffer.isFull()) {
                double kurtosis = MathUtil.kurtosis(yawBufferJerk.getValues());
                double autocorrelation = MathUtil.autocorrelation(yawBufferJerk.getValues(), 1);

                if (autocorrelation < -0.3 && kurtosis < -1.25) {
                    flag("SyntheticXAxis");
                }
                yawBufferJerk.getValues().clear();
                yawBuffer.getValues().clear();
            }

            if (pitchBufferJerk.isFull() && pitchBuffer.isFull()) {
                double kurtosis = MathUtil.kurtosis(pitchBufferJerk.getValues());
                double autocorrelation = MathUtil.autocorrelation(pitchBufferJerk.getValues(), 1);

                if (autocorrelation < -0.3 && kurtosis < -1.25) {
                    flag("SyntheticYAxis");
                }
                pitchBufferJerk.getValues().clear();
            }
        }
    }
}
