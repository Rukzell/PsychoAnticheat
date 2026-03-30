package com.psycho.utils;

import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HitboxUtils {
    public static List<Vector> getHitboxPoints(Entity entity, int pointsPerEdge, double expand) {
        BoundingBox box = entity.getBoundingBox().expand(expand);

        double minX = box.getMinX(), maxX = box.getMaxX();
        double minY = box.getMinY(), maxY = box.getMaxY();
        double minZ = box.getMinZ(), maxZ = box.getMaxZ();

        Set<Vector> points = new LinkedHashSet<>();

        for (int i = 0; i <= pointsPerEdge; i++) {
            double tx = (double) i / pointsPerEdge;
            for (int j = 0; j <= pointsPerEdge; j++) {
                double ty = (double) j / pointsPerEdge;
                for (int k = 0; k <= pointsPerEdge; k++) {
                    double tz = (double) k / pointsPerEdge;

                    boolean onX = (i == 0 || i == pointsPerEdge);
                    boolean onY = (j == 0 || j == pointsPerEdge);
                    boolean onZ = (k == 0 || k == pointsPerEdge);

                    if (!onX && !onY && !onZ) continue;

                    points.add(new Vector(
                            minX + tx * (maxX - minX),
                            minY + ty * (maxY - minY),
                            minZ + tz * (maxZ - minZ)
                    ));
                }
            }
        }

        return new ArrayList<>(points);
    }
}
