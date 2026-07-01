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
                // Zugriff via Getter-Methode screen()
                if (BrowserScreen.pipActive && Minecraft.getInstance().gui.screen() == null) {
                    renderPiP(guiGraphics);
                }
            }
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (keyToggleBrowser.consumeClick()) {
                if (client.gui.screen() instanceof BrowserScreen) {
                    client.setScreenAndShow(null);
                } else {
                    BrowserScreen.pipActive = false;
                    client.setScreenAndShow(new BrowserScreen());
                }
            }

            // 2. PiP Toggle Logik
            while (keyTogglePiP.consumeClick()) {
                BrowserScreen.pipActive = !BrowserScreen.pipActive;
                if (BrowserScreen.pipActive && client.gui.screen() instanceof BrowserScreen) {
                    client.setScreenAndShow(null);
                }
            }

            if (BrowserScreen.pipActive) {
                while (keyScrollUp.consumeClick()) sendScroll(1.0);
                while (keyScrollDown.consumeClick()) sendScroll(-1.0);
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