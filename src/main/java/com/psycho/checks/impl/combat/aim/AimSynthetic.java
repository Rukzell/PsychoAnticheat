package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimSynthetic extends Check {
    public AimSynthetic(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            SampleBuffer yawBufferJerk = player.getSampleBuffer(getName() + ":yawJerk", 60);
            SampleBuffer pitchBufferJerk = player.getSampleBuffer(getName() + ":pitchJerk", 60);
            SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 60);
            SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 60);
            yawBufferJerk.add(player.getJerkYaw());
            pitchBufferJerk.add(player.getJerkPitch());
            yawBuffer.add(player.getDeltaYaw());
            pitchBuffer.add(player.getDeltaPitch());

            if (yawBufferJerk.isFull() && yawBuffer.isFull()) {
                double kurtosis = MathUtil.kurtosis(yawBufferJerk.getValues());
                double autocorrelation = MathUtil.autocorrelation(yawBufferJerk.getValues(), 1);

                if (autocorrelation < -0.3 && kurtosis < -1.25) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimSyntheticNoise(XAxis), kurtosis: " + kurtosis + ", autocorrelation: " + autocorrelation);
                }
                yawBufferJerk.getValues().clear();
                yawBuffer.getValues().clear();
            }

            if (pitchBufferJerk.isFull() && pitchBuffer.isFull()) {
                double kurtosis = MathUtil.kurtosis(pitchBufferJerk.getValues());
                double autocorrelation = MathUtil.autocorrelation(pitchBufferJerk.getValues(), 1);

                if (autocorrelation < -0.3 && kurtosis < -1.25) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimSyntheticNoise(YAxis), kurtosis: " + kurtosis + ", autocorrelation: " + autocorrelation);
                }
                pitchBufferJerk.getValues().clear();
            }
        }
    }
}
