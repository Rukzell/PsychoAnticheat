package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimSignal extends Check {
    private final SampleBuffer yawBuffer = new SampleBuffer(256);
    private final SampleBuffer pitchBuffer = new SampleBuffer(256);

    public AimSignal(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg, true);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            yawBuffer.add(player.getDeltaYaw());
            pitchBuffer.add(player.getDeltaPitch());

            if (yawBuffer.isFull()) {
                double[] data = MathUtil.zNormalize(yawBuffer.getValues());
                double[] psd = MathUtil.welchPSD(data, 128, 64);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);
                double error = MathUtil.bestSineFit(data);

                if (error < 0.52) {
                    flag("BestSineFitXAxis");
                }

                if (spectralFlatness > 0.9) {
                    flag("SpectralFlatnessXAxis");
                }

                if (highFreqRatio > 0.42) {
                    flag("HighFreqXAxis");
                }
            }

            if (pitchBuffer.isFull()) {
                double[] data = MathUtil.zNormalize(pitchBuffer.getValues());
                double[] psd = MathUtil.welchPSD(data, 128, 64);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);
                double error = MathUtil.bestSineFit(data);

                if (error < 0.52) {
                    flag("BestSineFitYAxis");
                }

                if (spectralFlatness > 0.9) {
                    flag("SpectralFlatnessYAxis");
                }

                if (highFreqRatio > 0.42) {
                    flag("HighFreqYAxis");
                }
            }
        }
    }
}
