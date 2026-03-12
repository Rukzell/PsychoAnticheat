package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.Logger;
import net.rukzell.tac.utils.SampleBuffer;
import net.rukzell.tac.utils.math.MathUtil;

public class AimFrequency extends Check {
    public AimFrequency(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION && (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 128);
            SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 128);

            yawBuffer.add(Math.abs(player.getDeltaYaw()));
            pitchBuffer.add(Math.abs(player.getDeltaPitch()));

            if (yawBuffer.isFull()) {
                double highFreqRatio = MathUtil.highFreqRatio(yawBuffer.getValues());
                double spectralFlatness = MathUtil.spectralFlatness(yawBuffer.getValues());

                if (spectralFlatness > 0.82) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(XAxisSF), SF: " + spectralFlatness);
                }

                if (highFreqRatio > 0.35) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(XAxisFFT), ratio: " + highFreqRatio);
                }
                yawBuffer.getValues().clear();
            }

            if (pitchBuffer.isFull()) {
                double highFreqRatio = MathUtil.highFreqRatio(pitchBuffer.getValues());
                double spectralFlatness = MathUtil.spectralFlatness(pitchBuffer.getValues());

                if (spectralFlatness > 0.82) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(YAxisSF), SF: " + spectralFlatness);
                }

                if (highFreqRatio > 0.35) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimFrequency(YAxisFFT), ratio: " + highFreqRatio);
                }
                pitchBuffer.getValues().clear();
            }
        }
    }
}
