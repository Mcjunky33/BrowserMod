package de.mcjunky33;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.CommonComponents;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;

public class LinkScreenListener {

    public static void register() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ConfirmLinkScreen confirmLinkScreen) {
                try {
                    Field urlField = ConfirmLinkScreen.class.getDeclaredField("url");
                    urlField.setAccessible(true);
                    capturedUrl.set((String) urlField.get(confirmLinkScreen));
                } catch (Exception e) {
                    for (Field f : ConfirmLinkScreen.class.getDeclaredFields()) {
                        if (f.getType() == String.class) {
                            try {
                                f.setAccessible(true);
                                String s = (String) f.get(confirmLinkScreen);
                                if (s != null && s.startsWith("http")) {
                                    capturedUrl.set(s);
                                    break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ConfirmLinkScreen && capturedUrl.get() != null) {
                String url = capturedUrl.get();

                screen.children().stream()
                        .filter(w -> w instanceof Button)
                        .map(w -> (Button) w)
                        .filter(b -> {
                            String txt = b.getMessage().getString();
                            return txt.equals(CommonComponents.GUI_YES.getString()) ||
                                    txt.equalsIgnoreCase("yes") ||
                                    txt.toLowerCase().contains("open");
                        })
                        .findFirst()
                        .ifPresent(yesButton -> {
                            if (patchButtonAction(yesButton, url)) {
                                capturedUrl.set(null);
                            }
                        });
            }
        });
    }

    private static boolean patchButtonAction(Button button, String url) {
        try {
            for (Field f : Button.class.getDeclaredFields()) {
                if (f.getType().isInterface() && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);

                    Button.OnPress newAction = (btn) -> {
                        Minecraft mc = Minecraft.getInstance();
                        TabManager tm = BrowserScreen.getTabManager();
                        if (tm != null) {
                            tm.addTab(url);
                            mc.setScreen(new BrowserScreen());
                        }
                    };

                    f.set(button, newAction);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}