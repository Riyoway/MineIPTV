package me.riyo.mineiptv;

import net.minecraft.core.BlockPos;

import java.util.function.Consumer;

/** Loader-neutral handoff. Dedicated servers never install the client callback. */
public final class ClientBridge {
    private static Consumer<BlockPos> openTv = pos -> {};

    private ClientBridge() {}

    public static void install(Consumer<BlockPos> opener) {
        openTv = opener == null ? pos -> {} : opener;
    }

    public static void openTv(BlockPos masterPos) {
        openTv.accept(masterPos.immutable());
    }
}
