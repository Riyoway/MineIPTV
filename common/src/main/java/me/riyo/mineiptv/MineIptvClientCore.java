package me.riyo.mineiptv;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class MineIptvClientCore {
    private MineIptvClientCore() {}

    public static void initialize() {
        ClientBridge.install(MineIptvClientCore::openTelevision);
    }

    public static void openTelevision(BlockPos master) {
        Minecraft client = Minecraft.getInstance();
        TvPlaybackManager.activate(master);
        //? if >=26.1 {
        client.gui.setScreen(new IptvScreen(Component.literal("MineIPTV TV")));
        //?} else {
        /*client.setScreen(new IptvScreen(Component.literal("MineIPTV TV")));
        *///?}
    }

    public static void openStandalone() {
        Minecraft client = Minecraft.getInstance();
        TvPlaybackManager.activate(null);
        //? if >=26.1 {
        client.gui.setScreen(new IptvScreen(Component.literal("MineIPTV")));
        //?} else {
        /*client.setScreen(new IptvScreen(Component.literal("MineIPTV")));
        *///?}
    }

    public static void tick() {
        TvPlaybackManager.tick(Minecraft.getInstance());
    }

    public static void shutdown() {
        TvPlaybackManager.close();
    }
}
