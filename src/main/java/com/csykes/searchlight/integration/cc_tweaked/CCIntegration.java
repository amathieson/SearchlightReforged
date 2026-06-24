package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.lighting.LightPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class CCIntegration {
    private static final BlockCapability<IPeripheral, Direction> CAPABILITY = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("computercraft", "peripheral"), IPeripheral.class);

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                CAPABILITY,
                Searchlight.SEARCHLIGHT_BE.get(),
                (be, side) -> new LightPeripheral(be)
        );
        event.registerBlockEntity(
                CAPABILITY,
                Searchlight.WALL_LIGHT_BE.get(),
                (be, side) -> new LightPeripheral(be)
        );
    }
}
