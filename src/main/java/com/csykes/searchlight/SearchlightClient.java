package com.csykes.searchlight;

import com.csykes.searchlight.features.searchlight.SearchlightBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Searchlight.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Searchlight.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SearchlightClient {
    public SearchlightClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Searchlight.LOGGER.info("HELLO FROM CLIENT SETUP");
        Searchlight.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Searchlight.SEARCHLIGHT_BE.get(), SearchlightBlockRenderer::new);
    }

//    @SubscribeEvent
//    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
//        event.register((state, world, pos, tintIndex) -> {
//            if (tintIndex == 0 && world != null && pos != null) {
//                BlockPos targetPos = pos;
//                AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
//
//                if (face == AttachFace.CEILING) {
//                    targetPos = pos.above();
//                } else if (face == AttachFace.FLOOR) {
//                    targetPos = pos.below();
//                } else {
//                    Direction wallFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
//                    targetPos = pos.relative(wallFacing.getOpposite());
//                }
//
//                BlockState targetState = world.getBlockState(targetPos);
//                int color = event.getBlockColors().getColor(targetState, world, targetPos, 0);
//                if (color == -1) {
//                    return targetState.getMapColor(world, targetPos).col;
//                }
//                return color;
//            }
//            return -1;
//        }, Searchlight.CORNER_LIGHT.get());
//    }
}