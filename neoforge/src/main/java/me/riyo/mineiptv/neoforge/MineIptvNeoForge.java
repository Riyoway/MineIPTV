package me.riyo.mineiptv.neoforge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.network.MineIptvNetwork;
import me.riyo.mineiptv.network.TvControlPayload;
import me.riyo.mineiptv.tv.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod(MineIptv.MOD_ID)
public final class MineIptvNeoForge {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MineIptv.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MineIptv.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MineIptv.MOD_ID);

    private static final DeferredHolder<Block, TelevisionBlock> TV = BLOCKS.register("television", () ->
            new TelevisionBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()));
    private static final DeferredHolder<Block, TelevisionPanelBlock> PANEL = BLOCKS.register("tv_panel", () ->
            new TelevisionPanelBlock(BlockBehaviour.Properties.of().strength(1.8f).noOcclusion()));
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TelevisionBlockEntity>> TV_BE =
            BLOCK_ENTITIES.register("television", () -> BlockEntityType.Builder.of(TelevisionBlockEntity::new, TV.get()).build(null));
    private static final Map<TvSize, DeferredHolder<Item, Item>> TV_ITEMS = new EnumMap<>(TvSize.class);

    static {
        for (TvSize size : TvSize.values()) {
            TV_ITEMS.put(size, ITEMS.register(size.id(), () ->
                    new TelevisionItem(size, new Item.Properties().stacksTo(16))));
        }
    }

    public MineIptvNeoForge(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(this::registerPayloads);

        Map<TvSize, Supplier<? extends Item>> items = new EnumMap<>(TvSize.class);
        TV_ITEMS.forEach(items::put);
        ModTelevisions.install(TV, PANEL, TV_BE, items);
        MineIptv.initialize();
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TvControlPayload.TYPE, TvControlPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        MineIptvNetwork.handleServer(payload, player);
                    }
                });
    }
}
