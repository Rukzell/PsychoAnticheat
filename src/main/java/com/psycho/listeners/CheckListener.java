package com.psycho.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import org.bukkit.entity.Player;

public class CheckListener implements PacketListener {
    private final Psycho plugin;

    public CheckListener() {
        this.plugin = Psycho.get();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();

        if (player == null) return;

        PsychoPlayer psychoPlayer = plugin.getConnectionListener().getPlayer(player.getUniqueId());

        if (psychoPlayer == null) return;

        if (psychoPlayer.getHitCancelTicks() >= 0) {
            psychoPlayer.setHitCancelTicks(psychoPlayer.getHitCancelTicks() - 1);
        }

        psychoPlayer.updateSafeLocation();

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation wrapper =
                    new WrapperPlayClientPlayerPositionAndRotation(event);
            psychoPlayer.registerPosition(
                    wrapper.getPosition().getX(),
                    wrapper.getPosition().getY(),
                    wrapper.getPosition().getZ()
            );
            psychoPlayer.registerRotation(
                    wrapper.getYaw(),
                    wrapper.getPitch()
            );

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation wrapper =
                    new WrapperPlayClientPlayerRotation(event);
            psychoPlayer.registerRotation(
                    wrapper.getYaw(),
                    wrapper.getPitch()
            );

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition wrapper =
                    new WrapperPlayClientPlayerPosition(event);
            psychoPlayer.registerPosition(
                    wrapper.getPosition().getX(),
                    wrapper.getPosition().getY(),
                    wrapper.getPosition().getZ()
            );
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            if (psychoPlayer.getHitCancelTicks() > 0) {
                event.setCancelled(true);
            }

            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                psychoPlayer.registerHit();
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            switch (wrapper.getAction()) {
                case START_SPRINTING -> {
                    psychoPlayer.registerStartSprint();
                    psychoPlayer.updateSprintPacketTime();
                }
                case STOP_SPRINTING -> {
                    psychoPlayer.registerStopSprint();
                    psychoPlayer.updateSprintPacketTime();
                }
                default -> {
                }
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            psychoPlayer.setLastFlying(System.currentTimeMillis());
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            psychoPlayer.registerInventoryClick();
        }

        for (Check check : psychoPlayer.getChecks()) {
            check.process(event);
        }

        plugin.getPlayerTrackerService().trackSnapshot(psychoPlayer);
    }
}
