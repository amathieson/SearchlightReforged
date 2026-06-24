package com.csykes.searchlight;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MutableVector3d {
    public double x, y, z;

    public void add(net.minecraft.world.phys.Vec3 vector) {
        this.x += vector.x;
        this.y += vector.y;
        this.z += vector.z;
    }
}
