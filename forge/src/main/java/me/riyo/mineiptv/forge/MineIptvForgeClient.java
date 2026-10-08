package me.riyo.mineiptv.forge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.MineIptvClientCore;
import me.riyo.mineiptv.TelevisionRenderer;
import me.riyo.mineiptv.tv.ModTelevisions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class MineIptvForgeClient {
    private MineIptvForgeClient() {}

    @Mod.EventBusSubscriber(modid = MineIptv.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) { MineIptvClientCore.initialize(); }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModTelevisions.televisionBlockEntity(), TelevisionRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MineIptv.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class GameEvents {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent.Post event) { MineIptvClientCore.tick(); }
    }
}
