package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimFrequency extends Check {
    public AimFrequency(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION && !(player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 128);
            SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 128);

            yawBuffer.add(Math.abs(player.getDeltaYaw()));
            pitchBuffer.add(Math.abs(player.getDeltaPitch()));

            if (yawBuffer.isFull()) {
                double[] psd = MathUtil.welchPSD(yawBuffer.getValues(), 64, 32);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);

                if (spectralFlatness > 0.835) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(XAxisSF), SF: " + spectralFlatness);
                }

                if (highFreqRatio > 0.35) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(XAxisHF), ratio: " + highFreqRatio);
                }
                yawBuffer.getValues().clear();
            }

            if (pitchBuffer.isFull()) {
                double[] psd = MathUtil.welchPSD(yawBuffer.getValues(), 64, 32);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);

                if (spectralFlatness > 0.82) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(YAxisSF), SF: " + spectralFlatness);
                }

                if (highFreqRatio > 0.35) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(YAxisHF), ratio: " + highFreqRatio);
                }
                pitchBuffer.getValues().clear();
            }
        }
    }
}
