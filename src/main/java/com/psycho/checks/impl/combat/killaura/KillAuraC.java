package com.psycho.checks.impl.combat.killaura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.math.MathUtil;

import java.util.Deque;

public class KillAuraC extends Check {
    public KillAuraC(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled()) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        Deque<Long> hitDelays = player.getHitDelays();
        if (hitDelays.size() < 20) return;

        double stddev = MathUtil.stddev(hitDelays);
        double mean = hitDelays.stream().mapToLong(Long::longValue).average().orElse(1.0);
        double cv = (mean > 0) ? stddev / mean : Double.MAX_VALUE;

        if (stddev < 2 && cv < 0.01) {
            flag(String.format("stddev=%.2f ms, cv=%.4f, mean=%.1f ms", stddev, cv, mean));
        }
    }
}