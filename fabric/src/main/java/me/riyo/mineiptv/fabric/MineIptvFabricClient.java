package me.riyo.mineiptv.fabric;

import me.riyo.mineiptv.MineIptvClientCore;
import me.riyo.mineiptv.TelevisionRenderer;
import me.riyo.mineiptv.tv.ModTelevisions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public final class MineIptvFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MineIptvClientCore.initialize();
        BlockEntityRendererRegistry.register(ModTelevisions.televisionBlockEntity(), TelevisionRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> MineIptvClientCore.tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MineIptvClientCore.shutdown());
    }
}
