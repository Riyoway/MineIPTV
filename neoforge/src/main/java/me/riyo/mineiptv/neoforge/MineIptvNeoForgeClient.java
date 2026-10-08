package me.riyo.mineiptv.neoforge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.MineIptvClientCore;
import me.riyo.mineiptv.TelevisionRenderer;
import me.riyo.mineiptv.tv.ModTelevisions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only hooks. The outer subscriber is the NeoForge/game bus. */
@EventBusSubscriber(modid = MineIptv.MOD_ID, value = Dist.CLIENT)
public final class MineIptvNeoForgeClient {
    private MineIptvNeoForgeClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        MineIptvClientCore.tick();
    }

    /** Mod-bus-only lifecycle and renderer registration. */
    @EventBusSubscriber(modid = MineIptv.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            MineIptvClientCore.initialize();
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModTelevisions.televisionBlockEntity(), TelevisionRenderer::new);
        }
    }
}
