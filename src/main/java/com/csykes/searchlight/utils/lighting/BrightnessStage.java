package com.csykes.searchlight.utils.lighting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@Getter
@AllArgsConstructor
public enum BrightnessStage implements StringRepresentable {
    OFF(0, "off"),
    LOW(1, "low"),
    MEDIUM(2, "medium"),
    HIGH(3, "high"),
    ULTRA(4, "ultra");

    private final int id;
    private final String name;

    @Override
    public String getSerializedName() {
        return name;
    }

    public static BrightnessStage fromId(int id) {
        for (BrightnessStage stage : values()) {
            if (stage.id == id) {
                return stage;
            }
        }
        return MEDIUM;
    }

    public BrightnessStage next() {
        return fromId(Math.min(4, id + 1));
    }

    public BrightnessStage previous() {
        return fromId(Math.max(0, id - 1));
    }
}