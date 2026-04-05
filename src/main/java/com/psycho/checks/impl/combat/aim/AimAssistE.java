package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimAssistE extends Check {
    private final SampleBuffer yawBuffer = new SampleBuffer(256);
    private final SampleBuffer pitchBuffer = new SampleBuffer(256);

    public AimAssistE(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
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

                if (spectralFlatness > 0.97) {
                    flag("spectralflatness(x)=" + spectralFlatness);
                    cancelHits();
                }

                if (highFreqRatio > 0.47) {
                    flag("highfrequency(x)=" + highFreqRatio);
                    cancelHits();
                }
            }

            if (pitchBuffer.isFull()) {
                double[] data = MathUtil.zNormalize(pitchBuffer.getValues());
                double[] psd = MathUtil.welchPSD(data, 128, 64);

                double spectralFlatness = MathUtil.spectralFlatness(psd);
                double highFreqRatio = MathUtil.highFreqRatio(psd);

                if (spectralFlatness > 0.97) {
                    flag("spectralflatness(y)=" + spectralFlatness);
                }

                if (highFreqRatio > 0.47) {
                    flag("highfrequency(y)=" + highFreqRatio);
                    cancelHits();
                }
            }
        }
    }
}
