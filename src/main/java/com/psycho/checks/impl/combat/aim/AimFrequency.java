package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimFrequency extends Check {
    private final SampleBuffer yawBuffer = new SampleBuffer(128);
    private final SampleBuffer pitchBuffer = new SampleBuffer(128);

    public AimFrequency(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            yawBuffer.add(Math.abs(player.getDeltaYaw()));
            pitchBuffer.add(Math.abs(player.getDeltaPitch()));

            if (yawBuffer.isFull()) {
                double[] psd = MathUtil.welchPSD(yawBuffer.getValues(), 64, 32);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);

                if (spectralFlatness > 0.9) {
                    flag("SpectralFlatnessXAxis");
                }

                if (highFreqRatio > 0.4) {
                    flag("HighFreqXAxis");
                }
                yawBuffer.getValues().clear();
            }

            if (pitchBuffer.isFull()) {
                double[] psd = MathUtil.welchPSD(pitchBuffer.getValues(), 64, 32);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);

                if (spectralFlatness > 0.9) {
                    flag("SpectralFlatnessYAxis");
                }

                if (highFreqRatio > 0.4) {
                    flag("HighFreqYAxis");
                }
                pitchBuffer.getValues().clear();
            }
        }
    }
}
