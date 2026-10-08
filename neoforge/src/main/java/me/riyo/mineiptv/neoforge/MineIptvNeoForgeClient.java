package me.riyo.mineiptv.neoforge;

import me.riyo.mineiptv.MineIptv;
import me.riyo.mineiptv.MineIptvClientCore;
import me.riyo.mineiptv.TelevisionRenderer;
import me.riyo.mineiptv.network.MineIptvNetwork;
import me.riyo.mineiptv.tv.ModTelevisions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = MineIptv.MOD_ID, dist = Dist.CLIENT)
public final class MineIptvNeoForgeClient {
    public MineIptvNeoForgeClient(IEventBus modBus) {
        modBus.addListener(MineIptvNeoForgeClient::setup);
        modBus.addListener(MineIptvNeoForgeClient::renderers);
        NeoForge.EVENT_BUS.addListener(MineIptvNeoForgeClient::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        MineIptvClientCore.tick();
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MineIptvNetwork.installClientSender(PacketDistributor::sendToServer);
            MineIptvClientCore.initialize();
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTelevisions.televisionBlockEntity(), TelevisionRenderer::new);
    }
}
