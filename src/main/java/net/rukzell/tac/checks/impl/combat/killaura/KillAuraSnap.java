package net.rukzell.tac.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.utils.buffer.VlBuffer;

public class KillAuraSnap extends Check {
    private final VlBuffer angleLockingBuffer = new VlBuffer();

    public KillAuraSnap(CheckCfg cfg) {
        super(cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                float deltaYawAbs = Math.abs(player.getDeltaYaw());
                float lastDeltaYawAbs = Math.abs(player.getLastDeltaYaw());

                if (lastDeltaYawAbs < 2 && deltaYawAbs > 80) {
                    flag(player);
                }
            }
        }
    }
}
