package de.mcjunky33;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.glfw.GLFW;

public class BrowsermodMCEFClient implements ClientModInitializer {
    public static final String MOD_ID = "browsermodmcef";

    public static final Identifier PIP_ID = Identifier.fromNamespaceAndPath("browsermodmcef", "pip_overlay");
    public static KeyMapping keyToggleBrowser;
    public static KeyMapping keyTogglePiP;
    public static KeyMapping keyScrollUp;
    public static KeyMapping keyScrollDown;

    private static boolean hasSentSelectionMessage = false;

    @Override
    public void onInitializeClient() {
        LinkScreenListener.register();
        BlockSelectionHandler.init();
        new StreamScreenRenderer().init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("browsermod", "browsermod")
        );

        keyToggleBrowser = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".toggle_browser",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

        keyTogglePiP = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".toggle_pip",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, category));

        keyScrollUp = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".scroll_up",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, category));

        keyScrollDown = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".scroll_down",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, category));

        HudElementRegistry.addLast(PIP_ID, (extractor, tickCounter) -> {
            if (extractor instanceof net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics) {
                if (BrowserScreen.pipActive && Minecraft.getInstance().screen == null) {
                    renderPiP(guiGraphics);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyToggleBrowser.consumeClick()) {
                if (client.screen instanceof BrowserScreen) {
                    client.setScreen(null);
                } else {
                    BrowserScreen.pipActive = false;
                    client.setScreen(new BrowserScreen());
                }
            }

            while (keyTogglePiP.consumeClick()) {
                BrowserScreen.pipActive = !BrowserScreen.pipActive;
                if (BrowserScreen.pipActive && client.screen instanceof BrowserScreen) {
                    client.setScreen(null);
                }
            }

            if (BrowserScreen.pipActive) {
                while (keyScrollUp.consumeClick()) sendScroll(1.0);
                while (keyScrollDown.consumeClick()) sendScroll(-1.0);
            }

            if (client.level == null && (StreamScreen.isStreaming || StreamScreen.isSelecting)) {
                StreamScreen.reset();
            }

            if (client.player != null) {
                if (StreamScreen.isSelecting) {
                    if (!hasSentSelectionMessage) {
                        client.player.sendSystemMessage(Component.literal("§eRightclick: set position | Shift: cancel"));
                        hasSentSelectionMessage = true;
                    }
                } else {
                    hasSentSelectionMessage = false;
                }

                if (StreamScreen.isSelecting && client.player.isShiftKeyDown()) {
                    StreamScreen.reset();
                    client.player.sendSystemMessage(Component.literal("§cCanceled"));
                }
            }
        });
    }

    private void sendScroll(double amount) {
        var tm = BrowserScreen.getTabManager();
        if (tm != null && tm.getCurrent() != null && tm.getCurrent().browser != null) {
            tm.getCurrent().browser.sendMouseWheel(0, 0, 0.0, (int) amount);
        }
    }

    public static void renderPiP(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        var tm = BrowserScreen.getTabManager();
        if (tm != null && tm.getCurrent() != null && tm.getCurrent().browser != null) {
            var browser = tm.getCurrent().browser;
            if (browser.isTextureReady()) {
                int color = BrowserScreen.pipInteracting ? 0xFF00FF00 : 0xFFFFFFFF;
                graphics.fill(BrowserScreen.pipX - 1, BrowserScreen.pipY - 1,
                        BrowserScreen.pipX + BrowserScreen.pipW + 1,
                        BrowserScreen.pipY + BrowserScreen.pipH + 1, color);

                graphics.blit(RenderPipelines.GUI_TEXTURED, browser.getTextureIdentifier(),
                        BrowserScreen.pipX, BrowserScreen.pipY, 0, 0,
                        BrowserScreen.pipW, BrowserScreen.pipH,
                        BrowserScreen.pipW, BrowserScreen.pipH);
            }
        }
    }
}