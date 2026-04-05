package com.psycho.checks.impl.badpackets;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import org.bukkit.GameMode;

public class BadPacketsC extends Check {
    private final boolean is1_9 = player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
    private boolean sentAnimationSinceLastAttack = player.getUser().getClientVersion().isNewerThan(ClientVersion.V_1_8);
    private boolean sentAttack, sentAnimation, sentSlotSwitch;

    public BadPacketsC(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION
                && new WrapperPlayClientAnimation(event).getHand() == InteractionHand.MAIN_HAND) {
            sentAnimationSinceLastAttack = sentAnimation = true;
            sentAttack = sentSlotSwitch = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.STAB) {
                onAttack();
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE && !is1_9 && !sentSlotSwitch) {
            sentSlotSwitch = true;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onAttack();
                return;
            }
        }

        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) {
            if (sentAttack && is1_9) {
                flag("post-attack");
            }

            sentAttack = sentAnimation = sentSlotSwitch = false;
        }
    }

    private void onAttack() {
        if (player.getBukkitPlayer().getGameMode() == GameMode.SPECTATOR && player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11))
            return;

        sentAttack = true;

        if (is1_9 ? !sentAnimationSinceLastAttack : !sentAnimation) {
            sentAttack = false;
            flag("pre-attack");
        }

        sentAnimationSinceLastAttack = sentAnimation = sentSlotSwitch = false;
    }
}