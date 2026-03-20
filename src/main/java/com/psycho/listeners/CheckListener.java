package com.psycho.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
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

        psychoPlayer.updateSafeLocation();

        if (psychoPlayer.getTimeSinceLastHit() > 120000) {
            psychoPlayer.resetAllViolations();
        }

        for (Check check : Psycho.get().getCheckService().getRegisteredChecks()) {
            check.handle(psychoPlayer, event);
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            psychoPlayer.registerRotation(wrapper.getYaw(), wrapper.getPitch());
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
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
                default -> { }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            psychoPlayer.registerInventoryClick();
        }
    }
}
