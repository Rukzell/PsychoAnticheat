package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Logger;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

public class AimDistribution extends Check {
    public AimDistribution(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(PsychoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0) || !getCfg().enabled()) return;

        float deltaYaw = Math.abs(player.getDeltaYaw());
        float deltaPitch = Math.abs(player.getDeltaPitch());

        SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 30);
        SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 30);

        yawBuffer.add(deltaYaw);
        pitchBuffer.add(deltaPitch);

        if (!yawBuffer.isFull() || !pitchBuffer.isFull()) return;

        double yawIQR = MathUtil.robustRangeIQR(yawBuffer.getValues());
        double pitchIQR = MathUtil.robustRangeIQR(pitchBuffer.getValues());

        VlBuffer robustZeroVlBufferYaw = player.getBuffer(getName() + ":robust-zero-vl-buffer-yaw");
        VlBuffer robustZeroVlBufferPitch = player.getBuffer(getName() + ":robust-zero-vl-buffer-pitch");

        if (yawIQR == 0) {
            robustZeroVlBufferYaw.fail(1);
            if (robustZeroVlBufferYaw.getVl() > 4) {
                flag(player);
                robustZeroVlBufferYaw.setVl(0);
                Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistDistribution(robustZeroX)");
            }
            yawBuffer.getValues().clear();
        } else {
            robustZeroVlBufferYaw.decay(0.4);
        }

        if (pitchIQR == 0) {
            robustZeroVlBufferPitch.fail(1);
            if (robustZeroVlBufferPitch.getVl() > 4) {
                flag(player);
                robustZeroVlBufferPitch.setVl(0);
                Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistDistribution(robustZeroY)");
            }
            pitchBuffer.getValues().clear();
        } else {
            robustZeroVlBufferPitch.decay(0.4);
        }
    }
}
