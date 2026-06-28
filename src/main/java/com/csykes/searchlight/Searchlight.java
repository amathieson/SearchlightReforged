package com.csykes.searchlight;

import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.features.searchlight.SearchlightLightSourceBlock;
import com.csykes.searchlight.features.searchlight.SearchlightLightSourceBlockEntity;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.integration.cc_tweaked.CCIntegration;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(Searchlight.MODID)
public class Searchlight {
    public static final String MODID = "searchlight";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_DISTANCE = 256;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // --- Registries ---

    public static final DeferredBlock<Block> SEARCHLIGHT_BLOCK = BLOCKS.register("searchlight", () -> new SearchlightBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .pushReaction(PushReaction.DESTROY)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(4.0f)
            .noOcclusion()
            .lightLevel((state) -> state.getValue(AbstractLightBlock.LIT) ? (state.getValue(AbstractLightBlock.BRIGHTNESS).getId() + 1) * 3 : 0)));

    public static final DeferredItem<BlockItem> SEARCHLIGHT_ITEM = ITEMS.registerSimpleBlockItem("searchlight", SEARCHLIGHT_BLOCK);

    public static final DeferredBlock<Block> LIGHT_SOURCE_BLOCK = BLOCKS.register("searchlight_lightsource", () -> new SearchlightLightSourceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .replaceable()
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.DESTROY)
            .lightLevel((state) -> 15)));

    // Wall Lights
    public static final Map<String, DeferredBlock<Block>> WALL_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> CORNER_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> WALL_LIGHT_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> CORNER_LIGHTS_ITEMS = new LinkedHashMap<>();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SearchlightBlockEntity>> SEARCHLIGHT_BE = BLOCK_ENTITY_TYPES.register("searchlight_entity", () -> BlockEntityType.Builder.of(SearchlightBlockEntity::new, SEARCHLIGHT_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallLightBlockEntity>> WALL_LIGHT_BE = BLOCK_ENTITY_TYPES.register("wall_light_entity", () -> {
        Block[] blocks = WALL_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallLightBlockEntity>> CORNER_LIGHT_BE = BLOCK_ENTITY_TYPES.register("corner_light_entity", () -> {
        Block[] blocks = CORNER_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SearchlightLightSourceBlockEntity>> LIGHT_SOURCE_BE = BLOCK_ENTITY_TYPES.register("searchlight_lightsource_entity", () -> BlockEntityType.Builder.of(SearchlightLightSourceBlockEntity::new, LIGHT_SOURCE_BLOCK.get()).build(null));

    static {
        registerWallLight("iron");
        registerWallLight("copper");
        registerWallLight("prismarine");
        for (DyeColor color : DyeColor.values()) {
            registerWallLight(color.getName());
            registerCornerLight(color.getName());
        }
    }

    private static void registerWallLight(String postfix) {
        String wl_name = "wall_light_" + postfix;
        DeferredBlock<Block> block = BLOCKS.register(wl_name, () -> new WallLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> {
                    if (!state.getValue(BlockStateProperties.LIT)) return 0;
                    return state.hasProperty(AbstractLightBlock.BRIGHTNESS)
                            ? (state.getValue(AbstractLightBlock.BRIGHTNESS).getId() + 1) * 3
                            : 15; // Safe default light value
                })
                .sound(SoundType.STONE)
                .noOcclusion()));


        WALL_LIGHTS.put(postfix, block);
        WALL_LIGHT_ITEMS.put(postfix, ITEMS.registerSimpleBlockItem(wl_name, block));
    }

    private static void registerCornerLight(String postfix) {

        String cl_name = "corner_light_" + postfix;

        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> corner_light = BLOCKS.register(cl_name, () -> new CornerLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> {
                    if (!state.getValue(BlockStateProperties.LIT)) return 0;
                    return state.hasProperty(AbstractLightBlock.BRIGHTNESS)
                            ? (state.getValue(AbstractLightBlock.BRIGHTNESS).getId() + 1) * 3
                            : 15; // Safe default light value
                })
                .sound(SoundType.GLASS)
                .noOcclusion(), blockColor));
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, corner_light);
        CORNER_LIGHTS.put(postfix, corner_light);
        CORNER_LIGHTS_ITEMS.put(postfix, item);
    }


    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("searchlight_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.searchlight"))
            .icon(() -> new ItemStack(SEARCHLIGHT_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(SEARCHLIGHT_ITEM.get());
                WALL_LIGHT_ITEMS.values().forEach(item -> output.accept(item.get()));
                CORNER_LIGHTS_ITEMS.values().forEach(item -> output.accept(item.get()));
            }).build());

    public Searchlight(IEventBus modEventBus) {
        modEventBus.addListener(this::registerCapabilities);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            try {
                CCIntegration.register(event);
            } catch (Throwable e) {
                LOGGER.error("Failed to register ComputerCraft integration", e);
            }
        }
    }
}