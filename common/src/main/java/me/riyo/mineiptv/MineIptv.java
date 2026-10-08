package me.riyo.mineiptv;

import me.riyo.mineiptv.network.MineIptvNetwork;
import net.minecraft.resources.Identifier;

public final class MineIptv {
    public static final String MOD_ID = "mineiptv";
    private static boolean initialized;

    private MineIptv() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MineIptvNetwork.initialize();
    }
}
