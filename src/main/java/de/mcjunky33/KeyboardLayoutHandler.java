package de.mcjunky33;

import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KeyboardLayoutHandler {
    private final Map<Integer, Character> altGrMap = new HashMap<>();
    private String layoutName;

    public KeyboardLayoutHandler() {
        detectAndLoadLayout();
    }

    private void detectAndLoadLayout() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry().toLowerCase();
        String lang = locale.getLanguage().toLowerCase();

        if (country.equals("tr") || lang.equals("tr")) {
            loadTurkishLayout();
        } else if (country.equals("se") || lang.equals("se") || country.equals("fi")) {
            loadSwedishLayout();
        } else if (country.equals("ch") && (lang.equals("de") || lang.equals("fr"))) {
            loadSwissLayout();
        } else if (lang.equals("zh")) {
            layoutName = "Chinese (IME Support)";
            loadUSLayout(); // Basis für Pinyin
        } else if (lang.equals("ja")) {
            layoutName = "Japanese (Romaji/Kana)";
            loadJapaneseAltGr();
        } else {
            loadGermanLayout();
        }
    }

    private void loadTurkishLayout() {
        layoutName = "Turkish (Q/F)";
        altGrMap.put(GLFW.GLFW_KEY_Q, '@');
        altGrMap.put(GLFW.GLFW_KEY_E, '€');
        altGrMap.put(GLFW.GLFW_KEY_L, '₺'); // Türkische Lira
        altGrMap.put(GLFW.GLFW_KEY_G, 'ğ');
        altGrMap.put(GLFW.GLFW_KEY_S, 'ş');
    }

    private void loadSwedishLayout() {
        layoutName = "Swedish/Finnish";
        altGrMap.put(GLFW.GLFW_KEY_2, '@');
        altGrMap.put(GLFW.GLFW_KEY_3, '£');
        altGrMap.put(GLFW.GLFW_KEY_4, '$');
        altGrMap.put(GLFW.GLFW_KEY_E, '€');
        altGrMap.put(GLFW.GLFW_KEY_M, 'µ');
    }

    private void loadSwissLayout() {
        layoutName = "Swiss (QWERTZ)";
        altGrMap.put(GLFW.GLFW_KEY_G, '@');
        altGrMap.put(GLFW.GLFW_KEY_E, '€');
        altGrMap.put(GLFW.GLFW_KEY_1, '¦');
        altGrMap.put(GLFW.GLFW_KEY_7, '{');
        altGrMap.put(GLFW.GLFW_KEY_8, '[');
        altGrMap.put(GLFW.GLFW_KEY_9, ']');
        altGrMap.put(GLFW.GLFW_KEY_0, '}');
    }

    private void loadJapaneseAltGr() {
        altGrMap.put(GLFW.GLFW_KEY_Y, '¥');
        altGrMap.put(GLFW.GLFW_KEY_BACKSLASH, '|');
    }

    private void loadGermanLayout() {
        layoutName = "German (QWERTZ)";
        altGrMap.put(GLFW.GLFW_KEY_Q, '@');
        altGrMap.put(GLFW.GLFW_KEY_E, '€');
        altGrMap.put(GLFW.GLFW_KEY_7, '{');
        altGrMap.put(GLFW.GLFW_KEY_8, '[');
        altGrMap.put(GLFW.GLFW_KEY_9, ']');
        altGrMap .put(GLFW.GLFW_KEY_0, '}');
        altGrMap.put(GLFW.GLFW_KEY_MINUS, '\\');
        altGrMap .put(GLFW.GLFW_KEY_RIGHT_BRACKET, '~');
        altGrMap.put(GLFW.GLFW_KEY_BACKSLASH, '|');
        altGrMap.put(GLFW.GLFW_KEY_2, '²');
        altGrMap.put(GLFW.GLFW_KEY_3, '³');
    }

    private void loadUSLayout() {
        layoutName = "US English";
        altGrMap.put(GLFW.GLFW_KEY_2, '@');
    }

    public char getSpecialChar(int keyCode) {
        return altGrMap.getOrDefault(keyCode, (char) 0);
    }

    public String getLayoutName() { return layoutName; }
}