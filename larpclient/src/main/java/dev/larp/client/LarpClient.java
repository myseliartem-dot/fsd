package dev.larp.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * LarpClient - Minecraft 1.21.11 / Fabric (Yarn) / Java 21.
 * RIGHT SHIFT toggles the menu. All visuals are drawn in the HUD layer (no mixins).
 */
public class LarpClient implements ClientModInitializer {
    private static boolean lastKey;

    /** Polled from tick + every frame so quick taps are never missed. */
    public static void pollMenuKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !lastKey) {
            if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ClickGui());
            else if (mc.currentScreen instanceof ClickGui) mc.currentScreen.close();
        }
        lastKey = down;
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pollMenuKey();
            ModuleManager.tick();
        });
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("larpclient", "visuals"),
                (ctx, tickCounter) -> {
                    pollMenuKey();
                    ModuleManager.hud(ctx, tickCounter.getTickProgress(false));
                });
    }
}
