package me.riyo.mineiptv;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MineIptvClient implements ClientModInitializer {
    public static final String MOD_ID = "mineiptv";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(id("main"));
        KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mineiptv.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_I,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) {
                if (client.player == null) {
                    continue;
                }
                if (client.screen instanceof IptvScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new IptvScreen(Component.literal("MineIPTV")));
                }
            }
        });
    }
}
