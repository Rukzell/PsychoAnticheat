package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

public class AimDistribution extends Check {
    private final SampleBuffer yawBuffer = new SampleBuffer(30);
    private final SampleBuffer pitchBuffer = new SampleBuffer(30);
    private final VlBuffer robustZeroVlBufferYaw = new VlBuffer();
    private final VlBuffer robustZeroVlBufferPitch = new VlBuffer();

    public AimDistribution(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0) || !getCfg().enabled())
            return;

        float deltaYaw = Math.abs(player.getDeltaYaw());
        float deltaPitch = Math.abs(player.getDeltaPitch());

        yawBuffer.add(deltaYaw);
        pitchBuffer.add(deltaPitch);

        if (!yawBuffer.isFull() || !pitchBuffer.isFull()) return;

        double yawIQR = MathUtil.robustRangeIQR(yawBuffer.getValues());
        double pitchIQR = MathUtil.robustRangeIQR(pitchBuffer.getValues());

        if (yawIQR == 0) {
            robustZeroVlBufferYaw.fail(1);
            if (robustZeroVlBufferYaw.getVl() > 4) {
                flag("RobustXAxis");
                robustZeroVlBufferYaw.setVl(0);
            }
            yawBuffer.getValues().clear();
        } else {
            robustZeroVlBufferYaw.decay(0.4);
        }

        if (pitchIQR == 0) {
            robustZeroVlBufferPitch.fail(1);
            if (robustZeroVlBufferPitch.getVl() > 4) {
                flag("RobustYAxis");
                robustZeroVlBufferPitch.setVl(0);
            }
            pitchBuffer.getValues().clear();
        } else {
            robustZeroVlBufferPitch.decay(0.4);
        }
    }
}
