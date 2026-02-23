package net.rukzell.tac.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.player.TornadoPlayer;
import org.bukkit.entity.Player;

public class CheckListener implements PacketListener {
    private final TornadoAC plugin;

    public CheckListener() {
        this.plugin = TornadoAC.get();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();

        if (player == null) return;

        TornadoPlayer tornadoPlayer = plugin.getConnectionListener().getPlayer(player.getUniqueId());

        if (tornadoPlayer == null) return;

        for (Check check : TornadoAC.get().getCheckService().getRegisteredChecks()) {
            check.handle(tornadoPlayer, event);
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            tornadoPlayer.registerRotation(wrapper.getYaw(), wrapper.getPitch());
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                tornadoPlayer.registerHit();
            }
        }
    }
}
