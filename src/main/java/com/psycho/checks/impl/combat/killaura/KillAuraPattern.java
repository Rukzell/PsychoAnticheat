package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.math.MathUtil;

import java.util.Deque;

public class KillAuraPattern extends Check {
    public KillAuraPattern(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                Deque<Long> hitDelays = player.getHitDelays();
                if (hitDelays.size() == 20 && MathUtil.stddev(hitDelays) <= 1.1) {
                    flag();
                    setback();
                }
            }
        }
    }
}
