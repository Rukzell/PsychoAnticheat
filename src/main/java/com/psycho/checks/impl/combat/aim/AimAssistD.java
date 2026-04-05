package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.math.MathUtil;

public class AimAssistD extends Check {
    private final SampleBuffer yawBuffer = new SampleBuffer(60);
    private final SampleBuffer pitchBuffer = new SampleBuffer(60);

    public AimAssistD(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || !getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            yawBuffer.add(Math.abs(player.getJerkYaw()));
            pitchBuffer.add(Math.abs(player.getJerkPitch()));

            if (yawBuffer.isFull() && pitchBuffer.isFull()) {
                double stddevYaw = MathUtil.stddev(yawBuffer.getValues());
                double stddevPitch = MathUtil.stddev(pitchBuffer.getValues());
                double avgYaw = MathUtil.average(yawBuffer.getValues());
                double jerkAsymmetry = stddevYaw - stddevPitch;

                if (jerkAsymmetry > 4 && avgYaw < 2) {
                    flag("jerkasymmetry=" + jerkAsymmetry + ", avg=" + avgYaw);
                }

                yawBuffer.getValues().clear();
                pitchBuffer.getValues().clear();
            }
        }
    }
}
