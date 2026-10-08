package me.riyo.mineiptv.forge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.tv.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod(MineIptv.MOD_ID)
public final class MineIptvForge {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineIptv.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineIptv.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineIptv.MOD_ID);

    private static final RegistryObject<Block> TV = BLOCKS.register("television", () ->
            new TelevisionBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion().setId(BLOCKS.key("television"))));
    private static final RegistryObject<Block> PANEL = BLOCKS.register("tv_panel", () ->
            new TelevisionPanelBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion().setId(BLOCKS.key("tv_panel"))));
    private static final RegistryObject<BlockEntityType<?>> TV_BE = BLOCK_ENTITIES.register("television",
            () -> BlockEntityType.Builder.of(TelevisionBlockEntity::new, TV.get()).build(null));
    private static final Map<TvSize, RegistryObject<Item>> TV_ITEMS = new EnumMap<>(TvSize.class);

    static {
        for (TvSize size : TvSize.values()) {
            TV_ITEMS.put(size, ITEMS.register(size.id(), () -> new TelevisionItem(size,
                    new Item.Properties().stacksTo(16).setId(ITEMS.key(size.id())))));
        }
    }

    public MineIptvForge(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);
        Map<TvSize, Supplier<? extends Item>> items = new EnumMap<>(TvSize.class);
        TV_ITEMS.forEach(items::put);
        @SuppressWarnings("unchecked")
        Supplier<? extends BlockEntityType<TelevisionBlockEntity>> be = () ->
                (BlockEntityType<TelevisionBlockEntity>) TV_BE.get();
        ModTelevisions.install(TV, PANEL, be, items);
        MineIptv.initialize();
    }
}
