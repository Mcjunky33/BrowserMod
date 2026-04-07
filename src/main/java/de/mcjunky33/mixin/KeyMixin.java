package de.mcjunky33.mixin;

import de.mcjunky33.BrowsermodMCEFClient;
import de.mcjunky33.BrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class KeyMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onGlobalKeyPress(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = (Screen)(Object)this;

        if (currentScreen.getFocused() instanceof EditBox) {
            return;
        }

        if (currentScreen instanceof BrowserScreen) {
            return;
        }

        boolean allowed = currentScreen instanceof TitleScreen ||
                currentScreen instanceof PauseScreen ||
                currentScreen instanceof InventoryScreen ||
                currentScreen instanceof SafetyScreen ||
                currentScreen instanceof MouseSettingsScreen ||
                currentScreen instanceof VideoSettingsScreen ||
                currentScreen instanceof OptionsScreen ||
                currentScreen instanceof DeathScreen ||
                currentScreen instanceof LanguageSelectScreen ||
                currentScreen instanceof ControlsScreen ||
                currentScreen instanceof AccessibilityOnboardingScreen ||
                currentScreen instanceof LevelLoadingScreen ||
                currentScreen instanceof AbstractContainerScreen;

        if (!allowed) {
            return;
        }

        if (BrowsermodMCEFClient.keyToggleBrowser.matches(event)) {
            client.setScreen(new BrowserScreen());
            cir.setReturnValue(true);
            return;
        }

        if (BrowserScreen.currentInstance != null) {
            if (BrowsermodMCEFClient.keyTogglePiP.matches(event)) {
                BrowserScreen.pipActive = !BrowserScreen.pipActive;
                cir.setReturnValue(true);
                return;
            }

            if (BrowserScreen.pipActive) {
                if (BrowsermodMCEFClient.keyScrollUp.matches(event) ||
                        BrowsermodMCEFClient.keyScrollDown.matches(event)) {

                    if (BrowserScreen.currentInstance.keyPressed(event)) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }
}