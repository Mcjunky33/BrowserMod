package de.mcjunky33.mixin;

import de.mcjunky33.BrowsermodMCEFClient;
import de.mcjunky33.BrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.EditBox;
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