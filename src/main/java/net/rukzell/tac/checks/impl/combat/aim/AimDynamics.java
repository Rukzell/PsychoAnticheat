package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.Logger;
import net.rukzell.tac.utils.SampleBuffer;
import net.rukzell.tac.utils.buffer.VlBuffer;
import net.rukzell.tac.utils.math.MathUtil;

public class AimDynamics extends Check {
    public AimDynamics(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && (player.getDeltaYaw() == 0 && player.getDeltaPitch() == 0)) {
            SampleBuffer yawBuffer = player.getSampleBuffer(getName() + ":yaw", 60);
            SampleBuffer pitchBuffer = player.getSampleBuffer(getName() + ":pitch", 60);

            yawBuffer.add(Math.abs(player.getJerkYaw()));
            pitchBuffer.add(Math.abs(player.getJerkPitch()));

            if (!yawBuffer.isFull() || !pitchBuffer.isFull()) return;

            double stddevYaw = MathUtil.stddev(yawBuffer.getValues());
            double stddevPitch = MathUtil.stddev(pitchBuffer.getValues());
            double avgYaw = MathUtil.average(yawBuffer.getValues());

            double jerkAsymmetry = stddevYaw - stddevPitch;

            if (jerkAsymmetry > 4 && avgYaw < 2) {
                flag(player);
                Logger.log(player.getBukkitPlayer().getName() + " flagged for AimDynamics(JerkAsymmetry)");
            }

            yawBuffer.getValues().clear();
            pitchBuffer.getValues().clear();
        }
    }
}
