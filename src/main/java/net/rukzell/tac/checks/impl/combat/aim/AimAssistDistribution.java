package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.Logger;
import net.rukzell.tac.utils.SampleBuffer;
import net.rukzell.tac.utils.buffer.VlBuffer;
import net.rukzell.tac.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.List;

public class AimAssistDistribution extends Check {
    public AimAssistDistribution(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) return;

        float deltaYaw = Math.abs(player.getDeltaYaw());
        float deltaPitch = Math.abs(player.getDeltaPitch());

        SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 20);
        SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 20);

        yawBuffer.add(deltaYaw);
        pitchBuffer.add(deltaPitch);

        if (!yawBuffer.isFull() || !pitchBuffer.isFull()) return;

        double yawIQR = MathUtil.robustRangeIQR(yawBuffer.getValues());
        double pitchIQR = MathUtil.robustRangeIQR(pitchBuffer.getValues());

        SampleBuffer yawRobustBuffer = player.getSampleBuffer(getName() + ":robust-yaw", 200);
        SampleBuffer pitchRobustBuffer = player.getSampleBuffer(getName() + ":robust-pitch", 200);

        yawRobustBuffer.add((float) yawIQR);
        pitchRobustBuffer.add((float) pitchIQR);

        if (yawIQR == 0) {
            flag(player);
            yawBuffer.getValues().clear();
            Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistDistribution(robustX)");
        }

        if (pitchIQR == 0) {
            flag(player);
            pitchBuffer.getValues().clear();
            Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistDistribution(robustY)");
        }

        List<Float> allValuesList = new ArrayList<>();

        for (float f = 3.0f; f >= 0.91f; f -= 0.01f) {
            allValuesList.add(f);
        }

        for (float f = 0.9f; f >= 0.1f; f -= 0.1f) {
            allValuesList.add(f);
        }

        for (float f = 0.05f; f >= 0.01f; f -= 0.01f) {
            allValuesList.add(f);
        }

        allValuesList.add(0.005f);
        allValuesList.add(0.001f);

        float[] allValues = new float[allValuesList.size()];
        for (int i = 0; i < allValuesList.size(); i++) {
            allValues[i] = allValuesList.get(i);
        }

        VlBuffer robustRoundedVlBuffer = player.getBuffer(getName() + ":robust-rounded-vl-buffer");

        if (yawRobustBuffer.isFull() && pitchRobustBuffer.isFull()) {
            int roundedValuesYaw = MathUtil.roundedValues(yawRobustBuffer.getValues(), allValues).size();
            int roundedValuesPitch = MathUtil.roundedValues(pitchRobustBuffer.getValues(), allValues).size();

            if (roundedValuesYaw == 0 && roundedValuesPitch == 0) {
                robustRoundedVlBuffer.fail(1);
                if (robustRoundedVlBuffer.getVl() > 2) {
                    flag(player);
                    robustRoundedVlBuffer.setVl(0);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistDistribution(robustX rounded)");
                }
            } else {
                robustRoundedVlBuffer.setVl(0);
            }

            yawRobustBuffer.getValues().clear();
            pitchRobustBuffer.getValues().clear();
        }
    }
}
