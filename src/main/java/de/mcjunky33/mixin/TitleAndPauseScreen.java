package de.mcjunky33.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import de.mcjunky33.BrowserScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class TitleAndPauseScreen {

    @Shadow public int width;
    @Shadow public int height;
    @Shadow @Final private List<Renderable> renderables;
    @Shadow @Final private List<net.minecraft.client.gui.components.events.GuiEventListener> children;
    @Shadow @Final private List<Object> narratables;

    @Unique
    private Button myBrowserButton;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Object current = (Object) this;

        if (current instanceof PauseScreen || current instanceof TitleScreen) {
            if (!this.renderables.contains(myBrowserButton)) {
                ensureButtonExists((Screen) current);
            }
        }
    }

    @Unique
    private void ensureButtonExists(Screen screen) {
        int x, y;

        if (screen instanceof PauseScreen) {
            x = this.width - 25;
            y = this.height - 25;
        } else {
            x = this.width / 2 - 124;
            y = this.height / 4 + 118;
        }

        this.myBrowserButton = Button.builder(Component.literal("🌐"), b -> {
                    Minecraft.getInstance().setScreen(new BrowserScreen());
                })
                .bounds(x, y, 20, 20)
                .build();

        this.renderables.add(0, myBrowserButton);
        this.children.add(0, myBrowserButton);
        ((List<Object>)(Object)this.narratables).add(0, myBrowserButton);
    }
}