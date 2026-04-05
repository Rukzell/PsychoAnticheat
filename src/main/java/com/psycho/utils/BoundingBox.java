package com.psycho.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoundingBox {

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    public BoundingBox(double x, double y, double z, double width, double height) {
        double halfWidth = width / 2.0;

        this.minX = x - halfWidth;
        this.maxX = x + halfWidth;
        this.minY = y;
        this.maxY = y + height;
        this.minZ = z - halfWidth;
        this.maxZ = z + halfWidth;
    }

    public void update(double x, double y, double z, double width, double height) {
        double halfWidth = width / 2.0;

        this.minX = x - halfWidth;
        this.maxX = x + halfWidth;
        this.minY = y;
        this.maxY = y + height;
        this.minZ = z - halfWidth;
        this.maxZ = z + halfWidth;
    }

    public boolean intersects(BoundingBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX &&
                this.maxY >= other.minY && this.minY <= other.maxY &&
                this.maxZ >= other.minZ && this.minZ <= other.maxZ;
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(
                (minX + maxX) / 2,
                minY,
                (minZ + maxZ) / 2,
                (maxX - minX) + x,
                (maxY - minY) + y
        );
    }
}