package com.psycho.utils;

import org.bukkit.entity.Player;

public class KillAuraUtils {
    public static Player getTarget(int entityId, Player attacker) {
        for (Player p : attacker.getWorld().getPlayers()) {
            if (p.getEntityId() == entityId) {
                return p;
            }
        }
        return null;
    }
}
