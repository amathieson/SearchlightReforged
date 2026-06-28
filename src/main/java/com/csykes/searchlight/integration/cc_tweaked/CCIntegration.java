package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CCIntegration {
    private static final BlockCapability<IPeripheral, Direction> CAPABILITY = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("computercraft", "peripheral"), IPeripheral.class);

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                CAPABILITY,
                Searchlight.SEARCHLIGHT_BE.get(),
                (be, side) -> new LightPeripheral(be, "search_light")
        );
        event.registerBlockEntity(
                CAPABILITY,
                Searchlight.WALL_LIGHT_BE.get(),
                (be, side) -> new LightPeripheral(be, "wall_light")
        );
        event.registerBlockEntity(
                CAPABILITY,
                Searchlight.CORNER_LIGHT_BE.get(),
                (be, side) -> new LightPeripheral(be, "corner_light")
        );
    }
}
