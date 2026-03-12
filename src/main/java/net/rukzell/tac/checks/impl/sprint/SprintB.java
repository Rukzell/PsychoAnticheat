package net.rukzell.tac.checks.impl.sprint;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;

public class SprintB extends Check {
    public SprintB(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (player.getTimeSinceLastHit() > 2000 || player.getCps() > 3) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (wrapper.getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING || wrapper.getAction() == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                if (player.getSprintDelay() < 20_000_000) {
                    flag(player);
                    setback(player);
                }
            }
        }
    }
}
