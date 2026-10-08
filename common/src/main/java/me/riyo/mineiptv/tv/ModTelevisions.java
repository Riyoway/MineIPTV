package me.riyo.mineiptv.tv;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Loader-neutral registry handles installed before each loader starts registration. */
public final class ModTelevisions {
    private ModTelevisions() {}
    private static Supplier<? extends Block> television;
    private static Supplier<? extends Block> panel;
    private static Supplier<? extends BlockEntityType<TelevisionBlockEntity>> blockEntityType;
    private static final Map<TvSize, Supplier<? extends Item>> items = new EnumMap<>(TvSize.class);

    public static void install(Supplier<? extends Block> tv, Supplier<? extends Block> tvPanel,
                               Supplier<? extends BlockEntityType<TelevisionBlockEntity>> be,
                               Map<TvSize, ? extends Supplier<? extends Item>> tvItems) {
        television = tv; panel = tvPanel; blockEntityType = be;
        items.clear(); items.putAll(tvItems);
    }

    public static Block television() { return television.get(); }
    public static Block panel() { return panel.get(); }
    public static BlockEntityType<TelevisionBlockEntity> televisionBlockEntity() { return blockEntityType.get(); }
    public static Item itemFor(TvSize size) { return items.get(size).get(); }
}
