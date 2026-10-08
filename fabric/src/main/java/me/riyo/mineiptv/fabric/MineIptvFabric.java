package me.riyo.mineiptv.fabric;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.tv.*;
import net.fabricmc.api.ModInitializer;
//? if >=26.1 {
//?} else {
/*import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
*///?}
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class MineIptvFabric implements ModInitializer {
    @Override public void onInitialize() {
        var tvId = MineIptv.id("television");
        var panelId = MineIptv.id("tv_panel");
        //? if >=1.21.5 {
        Block tv = Registry.register(BuiltInRegistries.BLOCK, tvId,
                new TelevisionBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()
                        .setId(ResourceKey.create(Registries.BLOCK, tvId))));
        Block panel = Registry.register(BuiltInRegistries.BLOCK, panelId,
                new TelevisionPanelBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()
                        .setId(ResourceKey.create(Registries.BLOCK, panelId))));
        //?} else {
        /*Block tv = Registry.register(BuiltInRegistries.BLOCK, tvId,
                new TelevisionBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()));
        Block panel = Registry.register(BuiltInRegistries.BLOCK, panelId,
                new TelevisionPanelBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()));
        *///?}

        //? if >=26.1 {
        BlockEntityType<TelevisionBlockEntity> be = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MineIptv.id("television"),
                new BlockEntityType<>(TelevisionBlockEntity::new, Set.of(tv)));
        //?} else {
        /*BlockEntityType<TelevisionBlockEntity> be = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MineIptv.id("television"),
                FabricBlockEntityTypeBuilder.create(TelevisionBlockEntity::new, tv).build());
        *///?}

        Map<TvSize, Item> items = new EnumMap<>(TvSize.class);
        for (TvSize size : TvSize.values()) {
            var id = MineIptv.id(size.id());
            //? if >=1.21.5 {
            Item item = Registry.register(BuiltInRegistries.ITEM, id,
                    new TelevisionItem(size, new Item.Properties().stacksTo(16)
                            .setId(ResourceKey.create(Registries.ITEM, id))));
            //?} else {
            /*Item item = Registry.register(BuiltInRegistries.ITEM, id,
                    new TelevisionItem(size, new Item.Properties().stacksTo(16)));
            *///?}
            items.put(size, item);
        }
        Map<TvSize, Supplier<? extends Item>> itemSuppliers = new EnumMap<>(TvSize.class);
        items.forEach((size, item) -> itemSuppliers.put(size, () -> item));
        ModTelevisions.install(() -> tv, () -> panel, () -> be, itemSuppliers);
        MineIptv.initialize();
    }
}
