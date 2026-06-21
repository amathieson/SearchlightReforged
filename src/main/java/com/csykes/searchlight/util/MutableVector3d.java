package com.csykes.searchlight.util;

public class MutableVector3d {
    public double x, y, z;

    public MutableVector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(net.minecraft.world.phys.Vec3 vector) {
        this.x += vector.x;
        this.y += vector.y;
        this.z += vector.z;
    }
}
