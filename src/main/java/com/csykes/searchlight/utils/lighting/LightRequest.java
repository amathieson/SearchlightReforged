package com.csykes.searchlight.utils.lighting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@Getter
@AllArgsConstructor
public enum LightRequest implements StringRepresentable {
    RELEASE(0, "release"),
    OFF(1, "off"),
    ON(2, "on");

    private final int id;
    private final String name;

    @Override
    public String getSerializedName() {
        return name;
    }

}
