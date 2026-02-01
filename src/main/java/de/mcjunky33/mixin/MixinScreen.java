package de.mcjunky33.mixin;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mcjunky33.BrowserScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Unique private static boolean isDragging = false;
    @Unique private static boolean isResizing = false;
    @Unique private static double dragOffsetX, dragOffsetY;
    @Unique private static final int BORDER_THICKNESS = 10;

    @Unique private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/mcef/browsermod/pip_config.json");
    @Unique private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @Unique private static boolean configLoaded = false;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderPipOnTop(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!configLoaded) { loadPipConfig(); configLoaded = true; }

        if (BrowserScreen.pipActive && BrowserScreen.currentInstance != null) {
            if ((Object) this instanceof BrowserScreen && !BrowserScreen.pipInteracting) return;

            long handle = GLFW.glfwGetCurrentContext();
            boolean leftMouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == 1;
            boolean isMouseOver = mouseX >= BrowserScreen.pipX && mouseX <= BrowserScreen.pipX + BrowserScreen.pipW &&
                    mouseY >= BrowserScreen.pipY && mouseY <= BrowserScreen.pipY + BrowserScreen.pipH;

            Component pipKey = getMapping("key.browsermodmcef.toggle_pip");
            Component browserKey = getMapping("key.browsermodmcef.toggle_browser");

            MutableComponent infoText = Component.literal("Press ")
                    .append(pipKey.copy().withStyle(ChatFormatting.YELLOW))
                    .append(" to show and hide PIP and press ")
                    .append(browserKey.copy().withStyle(ChatFormatting.YELLOW))
                    .append(" to open Browser in Fullscreen to interact");

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int textWidth = mc.font.width(infoText);


            graphics.drawString(mc.font, infoText, (screenWidth / 2) - (textWidth / 2), screenHeight - 60, 0xFFFFFFFF, true);


            if (leftMouseDown) {
                if (!isDragging && !isResizing && isMouseOver) {
                    if (mouseX > (BrowserScreen.pipX + BrowserScreen.pipW - BORDER_THICKNESS) ||
                            mouseY > (BrowserScreen.pipY + BrowserScreen.pipH - BORDER_THICKNESS)) {
                        isResizing = true;
                    } else {
                        isDragging = true;
                        dragOffsetX = mouseX - BrowserScreen.pipX;
                        dragOffsetY = mouseY - BrowserScreen.pipY;
                    }
                    BrowserScreen.pipInteracting = true;
                }
                if (isResizing) {
                    BrowserScreen.pipW = Math.max(80, mouseX - BrowserScreen.pipX);
                    BrowserScreen.pipH = Math.max(45, mouseY - BrowserScreen.pipY);
                } else if (isDragging) {
                    BrowserScreen.pipX = (int) (mouseX - dragOffsetX);
                    BrowserScreen.pipY = (int) (mouseY - dragOffsetY);
                }
            } else {
                if (isDragging || isResizing) savePipConfig();
                isDragging = false;
                isResizing = false;
                if (!isMouseOver) BrowserScreen.pipInteracting = false;
            }

            var tab = BrowserScreen.getTabManager().getCurrent();
            if (tab != null && tab.browser != null && tab.browser.isTextureReady()) {
                int color = BrowserScreen.pipInteracting ? 0xFF00FF00 : 0xFFFFFFFF;
                graphics.fill(BrowserScreen.pipX - 1, BrowserScreen.pipY - 1,
                        BrowserScreen.pipX + BrowserScreen.pipW + 1,
                        BrowserScreen.pipY + BrowserScreen.pipH + 1, color);

                graphics.blit(RenderPipelines.GUI_TEXTURED, tab.browser.getTextureLocation(),
                        BrowserScreen.pipX, BrowserScreen.pipY,
                        0.0f, 0.0f, BrowserScreen.pipW, BrowserScreen.pipH,
                        BrowserScreen.pipW, BrowserScreen.pipH);
            }
        }
    }

    @Unique
    private Component getMapping(String keyName) {
        return Arrays.stream(Minecraft.getInstance().options.keyMappings)
                .filter(m -> m.getName().equals(keyName))
                .findFirst()
                .map(KeyMapping::getTranslatedKeyMessage)
                .orElse(Component.literal("NONE"));
    }

    @Unique
    private void savePipConfig() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("x", BrowserScreen.pipX);
            json.addProperty("y", BrowserScreen.pipY);
            json.addProperty("w", BrowserScreen.pipW);
            json.addProperty("h", BrowserScreen.pipH);
            if (!CONFIG_FILE.getParentFile().exists()) CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) { GSON.toJson(json, writer); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Unique
    private void loadPipConfig() {
        if (!CONFIG_FILE.exists()) return;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                if (json.has("x")) BrowserScreen.pipX = json.get("x").getAsInt();
                if (json.has("y")) BrowserScreen.pipY = json.get("y").getAsInt();
                if (json.has("w")) BrowserScreen.pipW = json.get("w").getAsInt();
                if (json.has("h")) BrowserScreen.pipH = json.get("h").getAsInt();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}