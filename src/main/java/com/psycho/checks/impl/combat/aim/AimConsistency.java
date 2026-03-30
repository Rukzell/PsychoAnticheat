package com.psycho.checks.impl.combat.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.SampleBuffer;
import com.psycho.utils.buffer.VlBuffer;
import com.psycho.utils.math.MathUtil;

public class AimConsistency extends Check {
    public AimConsistency(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg, false);
    }

    @Override
    public void handle(PacketReceiveEvent event) {
    }
}
