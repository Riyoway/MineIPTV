package me.riyo.mineiptv.neoforge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.MineIptvClientCore;
import me.riyo.mineiptv.TelevisionRenderer;
import me.riyo.mineiptv.network.MineIptvNetwork;
import me.riyo.mineiptv.tv.ModTelevisions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MineIptv.MOD_ID, value = Dist.CLIENT)
public final class MineIptvNeoForgeClient {
    private MineIptvNeoForgeClient() {}

    public static void registerModEvents(IEventBus modBus) {
        modBus.addListener(MineIptvNeoForgeClient::setup);
        modBus.addListener(MineIptvNeoForgeClient::renderers);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        MineIptvClientCore.tick();
    }

    private static void setup(FMLClientSetupEvent event) {
        MineIptvNetwork.installClientSender(PacketDistributor::sendToServer);
        MineIptvClientCore.initialize();
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTelevisions.televisionBlockEntity(), TelevisionRenderer::new);
    }
}
