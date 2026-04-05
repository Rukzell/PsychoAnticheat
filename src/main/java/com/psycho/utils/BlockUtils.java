package com.psycho.utils;

import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockUtils {
    public static Block getBlockAtPosition(World world, Vector3i pos) {
        if (world == null || pos == null) return null;
        return world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }
}
