package net.rukzell.tac.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.Logger;
import net.rukzell.tac.utils.buffer.VlBuffer;
import net.rukzell.tac.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.List;

public class AimAssistConsistency extends Check {
    private final List<Float> deltaYawsStd = new ArrayList<>();
    private final List<Float> deltaYawsDistinct = new ArrayList<>();
    private final List<Float> deltaPitchesStd = new ArrayList<>();
    private final List<Float> deltaPitchesDistinct = new ArrayList<>();
    public AimAssistConsistency(CheckCfg cfg) {
        super(cfg);
    }

    private final VlBuffer bufferYawDistinct = new VlBuffer();
    private final VlBuffer bufferPitchDistinct = new VlBuffer();

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            deltaYawsStd.add(Math.abs(player.getDeltaYaw()));
            deltaPitchesStd.add((player.getDeltaPitch()));
            deltaYawsDistinct.add(Math.abs(player.getDeltaYaw()));
            deltaPitchesDistinct.add(Math.abs(player.getDeltaPitch()));

            if (deltaYawsStd.size() >= 5) {
                if (MathUtil.stddev(deltaYawsStd) == 0) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevXAxis)");
                }

                deltaYawsStd.clear();
            }

            if (deltaPitchesStd.size() >= 5) {
                if (MathUtil.stddev(deltaPitchesStd) == 0) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(StdDevYAxis)");
                }

                deltaPitchesStd.clear();
            }

            if (deltaYawsDistinct.size() >= 10) {
                int distinct = (int) deltaYawsDistinct.stream().distinct().count();
                if (distinct < 8) {
                    bufferYawDistinct.fail(1);
                } else {
                    bufferYawDistinct.decay(0.25);
                }

                if (bufferYawDistinct.getVl() > 3) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(DistinctXAxis)");
                }
                deltaYawsDistinct.clear();
            }

            if (deltaPitchesDistinct.size() >= 10) {
                int distinct = (int) deltaPitchesDistinct.stream().distinct().count();
                if (distinct < 8) {
                    bufferPitchDistinct.fail(1);
                } else {
                    bufferPitchDistinct.decay(0.25);
                }

                if (bufferPitchDistinct.getVl() > 3) {
                    flag(player);
                    Logger.log(player.getBukkitPlayer().getName() + " flagged for AimAssistConsistency(DistinctYAxis)");
                }
                deltaPitchesDistinct.clear();
            }
        }
    }
}
