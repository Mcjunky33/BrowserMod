package de.mcjunky33;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StreamScreen extends Screen {

    public static boolean isSelecting = false;
    public static boolean isStreaming = false;
    public static Direction face = null;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    private EditBox nameField;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mcef/browsermod/streampresets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Preset {
        public String name;
        public int x1, y1, z1, x2, y2, z2;
        public String direction;

        public Preset(String name, BlockPos pos1, BlockPos pos2, Direction face) {
            this.name = name;
            this.x1 = pos1.getX(); this.y1 = pos1.getY(); this.z1 = pos1.getZ();
            this.x2 = pos2.getX(); this.y2 = pos2.getY(); this.z2 = pos2.getZ();
            this.direction = face.getName();
        }
    }

    public static void reset() {
        isSelecting = false;
        isStreaming = false;
        pos1 = null;
        pos2 = null;
        face = null;
    }

    public StreamScreen() {
        super(Component.literal("Stream Configuration"));
    }

    @Override
    protected void init() {
        super.init();
        boolean inWorld = Minecraft.getInstance().level != null;

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> Minecraft.getInstance().setScreen(new BrowserScreen()))
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 55, 100, 20)
                .build());

        String btnText = isSelecting ? "Stop Selection" : (isStreaming ? "Stop Screen" : "Create Screen");
        Button.Builder createBtn = Button.builder(Component.literal(btnText), b -> {
            if (inWorld) {
                if (isSelecting || isStreaming) {
                    isSelecting = false; isStreaming = false;
                    pos1 = null; pos2 = null; face = null;
                } else {
                    isSelecting = true;
                    pos1 = null; pos2 = null; face = null;
                    this.onClose();
                }
                this.refreshGui();
            }
        }).bounds(this.width / 2 - 120, this.height / 2 + 50, 240, 20);

        if (!inWorld) createBtn.tooltip(Tooltip.create(Component.literal("You need to join a world or server")));
        this.addRenderableWidget(createBtn.build());

        if (pos1 != null && pos2 != null && face != null) {
            nameField = new EditBox(font, this.width / 2 - 120, 230, 120, 20, Component.literal("Name"));
            this.addRenderableWidget(nameField);

            this.addRenderableWidget(Button.builder(Component.literal("Save current Screen as Preset"), b -> saveCurrentPreset())
                    .bounds(this.width / 2 + 5, 230, 115, 20).build());
        }


        List<Preset> presets = loadPresets();
        int yOffset = 60;
        int rightMargin = this.width - 250;

        for (int i = 0; i < presets.size(); i++) {
            Preset p = presets.get(i);
            int index = i;

            String label = String.format("%s (%d,%d,%d -> %d,%d,%d)", p.name, p.x1, p.y1, p.z1, p.x2, p.y2, p.z2);

            Button.Builder presetBtn = Button.builder(Component.literal(label), b -> applyPreset(p))
                    .bounds(rightMargin, yOffset, 190, 20);
            if (!inWorld) presetBtn.tooltip(Tooltip.create(Component.literal("You need to join a world or server")));
            this.addRenderableWidget(presetBtn.build());

            this.addRenderableWidget(Button.builder(Component.literal("X"), b -> deletePreset(index))
                    .bounds(rightMargin + 195, yOffset, 45, 20).build());

            yOffset += 25;
        }
    }

    private void saveCurrentPreset() {
        if (nameField == null || nameField.getValue().isEmpty()) return;
        List<Preset> presets = loadPresets();
        presets.add(new Preset(nameField.getValue(), pos1, pos2, face));
        saveToFile(presets);
        this.refreshGui();
    }

    private void deletePreset(int index) {
        List<Preset> presets = loadPresets();
        if (index >= 0 && index < presets.size()) {
            presets.remove(index);
            saveToFile(presets);
            this.refreshGui();
        }
    }

    private void saveToFile(List<Preset> presets) {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) { GSON.toJson(presets, writer); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void applyPreset(Preset p) {
        pos1 = new BlockPos(p.x1, p.y1, p.z1);
        pos2 = new BlockPos(p.x2, p.y2, p.z2);
        face = Direction.byName(p.direction);
        isSelecting = false;
        isStreaming = true;

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§aScreen created: " + pos1.toShortString() + " to " + pos2.toShortString() + " [" + face.getName() + "]"));
        }
        this.onClose();
    }

    private List<Preset> loadPresets() {
        if (!CONFIG_PATH.toFile().exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            return GSON.fromJson(reader, new TypeToken<List<Preset>>(){}.getType());
        } catch (IOException e) { return new ArrayList<>(); }
    }

    private void renderStreamPreview(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h) {
        TabManager.BrowserTab currentTab = BrowserScreen.getTabManager().getCurrent();
        if (currentTab != null && currentTab.browser != null && currentTab.browser.isTextureReady()) {
            Identifier tex = currentTab.browser.getTextureIdentifier();
            guiGraphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF888888);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, w, h, w, h);
            String label = "Stream Preview";
            int textWidth = font.width(label);
            guiGraphics.textWithBackdrop(this.font, Component.literal(label), x + (w / 2) - (textWidth / 2), y - 12, textWidth, 0xFFFFFFFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partial);
        renderStreamPreview(guiGraphics, (this.width / 2) - 140, 60, 280, 158);
        guiGraphics.centeredText(this.font, "Stream Mod Settings", this.width / 2, 20, 0xFFFFFF);

    }

    protected void refreshGui() {
        this.clearWidgets();
        this.init();
    }
}