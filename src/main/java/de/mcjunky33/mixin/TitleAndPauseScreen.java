package de.mcjunky33.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import de.mcjunky33.BrowserScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Screen.class, priority = 2000)
public abstract class TitleAndPauseScreen {

    @Shadow public int width;
    @Shadow public int height;

    @Unique
    private Button cachedBrowserButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.cachedBrowserButton = null;
        updateBrowserButton();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onReposition(CallbackInfo ci) {
        updateBrowserButton();
    }

    @Unique
    private void updateBrowserButton() {
        Object current = (Object) this;
        if (!(current instanceof PauseScreen) && !(current instanceof TitleScreen)) return;

        ScreenAccessor accessor = (ScreenAccessor) this;

        int x = 0, y = 0;
        if (current instanceof PauseScreen) {
            x = this.width - 25;
            y = this.height - 25;
        } else {
            String optionsText = Component.translatable("menu.options").getString();
            for (Object child : accessor.getChildren()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.getMessage().getString().equals(optionsText)) {
                        x = widget.getX() - 24;
                        y = widget.getY() - 24;
                        break;
                    }
                }
            }
        }

        if (x == 0 && y == 0) return;

        if (this.cachedBrowserButton != null &&
                accessor.getRenderables().contains(this.cachedBrowserButton)) {
            if (this.cachedBrowserButton.getX() == x && this.cachedBrowserButton.getY() == y) {
                return;
            }
            accessor.getRenderables().remove(this.cachedBrowserButton);
            accessor.getChildren().remove(this.cachedBrowserButton);
        }

        this.cachedBrowserButton = Button.builder(Component.literal("🌐"), b -> {
                    Minecraft.getInstance().setScreen(new BrowserScreen());
                })
                .bounds(x, y, 20, 20)
                .build();

        accessor.invokeAddRenderableWidget(this.cachedBrowserButton);
    }
}