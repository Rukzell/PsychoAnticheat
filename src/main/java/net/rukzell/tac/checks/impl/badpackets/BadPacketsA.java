package net.rukzell.tac.checks.impl.badpackets;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.rukzell.tac.cfg.CheckCfg;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;

public class BadPacketsA extends Check {
    public BadPacketsA(String cfgPath, CheckCfg cfg) {
        super(cfgPath, cfg);
    }

    @Override
    public void handle(TornadoPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            if (Math.abs(player.getPitch()) > 90.2) {
                flag(player);
            }
        }
    }
}
