package com.csykes.searchlight;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class SearchLightLanguageProvider extends LanguageProvider {

    public SearchLightLanguageProvider(PackOutput output) {
        super(output, Searchlight.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {

        for (String key : Searchlight.CORNER_LIGHTS.keySet()) {
            String blockId = "corner_light_" + key;

            add("block.searchlight." + blockId,
                    formatName(key) + " Corner Light");
        }

        for (String key : Searchlight.WALL_LIGHTS.keySet()) {
            String blockId = "wall_light_" + key;

            add("block.searchlight." + blockId,
                    formatName(key) + " Wall Light");
        }
    }

    private String formatName(String key) {
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }
}