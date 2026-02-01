package de.mcjunky33;

import de.mcjunky33.TabManager.BrowserTab;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.MutableComponent;

public class BrowserScreen extends Screen {
    private static final int UI_HEIGHT = 45;
    private static TabManager tabManager;
    private final KeyboardLayoutHandler layoutHandler = new KeyboardLayoutHandler();

    private EditBox urlBar;
    private boolean isFullscreen = false;
    private int fullscreenMsgTimer = 0;
    private boolean isAltGrActive = false;
    private int tabScrollOffset = 0;
    public static boolean pipActive = false;
    public static boolean pipInteracting = false;
    public static int pipX = 10, pipY = 10;
    public static int pipW = 320, pipH = 180;
    public static boolean isDragging = false;
    public static boolean isResizing = false;
    public static double dragStartX, dragStartY;
    public static BrowserScreen currentInstance;


    public BrowserScreen() {
        super(Component.literal("Browser"));
        currentInstance = this;
        if (tabManager == null) {
            tabManager = new TabManager();
            tabManager.load();
        }
    }


    public static TabManager getTabManager() {
        if (tabManager == null) {
            tabManager = new TabManager();
            tabManager.load();
        }
        return tabManager;
    }

    @Override
    protected void init() {
        super.init();
        pipActive = false;
        validateIndices();

        TabManager.BrowserTab currentTab = tabManager.getCurrent();

        for (TabManager.BrowserTab tab : tabManager.tabs) {
            tab.updateTitle();
        }

        if (currentTab != null && currentTab.browser != null) {

            String screenFixJS =
                    "Element.prototype.requestFullscreen = function() {" +
                            "  this.classList.add('forced-fs');" +
                            "  this.style.position = 'fixed';" +
                            "  this.style.top = '0'; this.style.left = '0';" +
                            "  this.style.width = '100vw'; this.style.height = '100vh';" +
                            "  this.style.zIndex = '2147483647';" +
                            "  document.documentElement.style.overflow = 'hidden';" +
                            "  return Promise.resolve();" +
                            "};" +
                            "document.exitFullscreen = function() {" +
                            "  var v = document.querySelector('.forced-fs');" +
                            "  if(v) { v.style.position = ''; v.style.width = ''; v.style.height = ''; v.classList.remove('forced-fs'); }" +
                            "  document.documentElement.style.overflow = 'auto';" +
                            "  return Promise.resolve();" +
                            "};";
            currentTab.browser.executeJavaScript(screenFixJS, currentTab.browser.getURL(), 0);
        }

        if (!isFullscreen) {

            addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
                    .bounds(this.width - 25, 2, 20, 18)
                    .tooltip(Tooltip.create(Component.literal("Close browser")))
                    .build());


            int urlBarWidth = width - 108 - 75;
            this.urlBar = new EditBox(font, 88, 22, urlBarWidth, 20, Component.literal("URL"));
            this.urlBar.setMaxLength(10000);
            if (currentTab != null && currentTab.browser != null) {
                this.urlBar.setValue(currentTab.browser.getURL());
                this.urlBar.setCursorPosition(0);
                this.urlBar.setHighlightPos(0);
            }
            this.addWidget(this.urlBar);

            addRenderableWidget(Button.builder(Component.literal("⟳"), b -> { if(currentTab != null) currentTab.browser.reload(); }).bounds(5, 2, 20, 18).tooltip(Tooltip.create(Component.literal("Reload Website"))).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                tabManager.addTab("https://www.google.com");
                refreshGui();
                this.setFocused(null);
            }).bounds(27, 2, 20, 18).tooltip(Tooltip.create(Component.literal("Open new Tab"))).build());

            addRenderableWidget(Button.builder(Component.literal("<<"), b -> {
                if (tabScrollOffset > 0) { tabScrollOffset--; refreshGui(); }
            }).bounds(52, 2, 25, 18).tooltip(Tooltip.create(Component.literal("scroll Tabs to left"))).build());

            addRenderableWidget(Button.builder(Component.literal(">>"), b -> {
                if (tabScrollOffset < tabManager.tabs.size() - 1) { tabScrollOffset++; refreshGui(); }
            }).bounds(79, 2, 25, 18).tooltip(Tooltip.create(Component.literal("scroll Tabs to right"))).build());


            addRenderableWidget(Button.builder(Component.literal("📺"), b -> {
                pipActive = true;
                this.onClose();
            }).bounds(width - 65, 22, 20, 20).tooltip(Tooltip.create(Component.literal("Picture-in-Picture Mode"))).build());


            addRenderableWidget(Button.builder(Component.literal("<"), b -> { if(currentTab != null) currentTab.browser.goBack(); }).bounds(5, 22, 20, 20).tooltip(Tooltip.create(Component.literal("back"))).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { if(currentTab != null) currentTab.browser.goForward(); }).bounds(30, 22, 20, 20).tooltip(Tooltip.create(Component.literal("before"))).build());
            addRenderableWidget(Button.builder(Component.literal("⌂"), b -> {
                if(currentTab != null) {
                    currentTab.browser.loadURL("https://www.google.com");
                }
            }).bounds(52, 22, 20, 20).tooltip(Tooltip.create(Component.literal("Home (Google.com)"))).build());
            addRenderableWidget(Button.builder(Component.literal("🌐"), b -> { if(currentTab != null) Util.getPlatform().openUri(currentTab.browser.getURL()); }).bounds(width - 45, 22, 20, 20).tooltip(Tooltip.create(Component.literal("Open URL in extern Browser"))).build());
            addRenderableWidget(Button.builder(Component.literal("⛶"), b -> toggleFullscreen()).bounds(width - 25, 22, 20, 20).tooltip(Tooltip.create(Component.literal("Open Fullscreen (Press F12 to exit Fullscreen)"))).build());

            renderTabWidgets();
        }

        resizeAllBrowsers();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_ALT) {
            isAltGrActive = true;
        }


        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.isFullscreen) { toggleFullscreen(); return true; }
            if (urlBar != null && urlBar.isFocused()) { urlBar.setFocused(false); return true; }
            this.onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_F12) {
            toggleFullscreen();
            return true;
        }


        if (!isFullscreen && urlBar != null && urlBar.isFocused()) {

            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                handleUrlInput(urlBar.getValue());
                return true;
            }
            return urlBar.keyPressed(event);
        }

        if (isAltGrActive && !tabManager.tabs.isEmpty()) {
            char special = layoutHandler.getSpecialChar(event.key());
            if (special != 0) {
                tabManager.getCurrent().browser.sendKeyTyped(special, 0);
                return true;
            }
        }

        validateIndices();
        if (!tabManager.tabs.isEmpty() && tabManager.getCurrent().browser != null) {
            tabManager.getCurrent().browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {

        if (event.key() == GLFW.GLFW_KEY_RIGHT_ALT) {
            isAltGrActive = false;
        }


        if (pipActive && pipInteracting) {
            validateIndices();
            if (!tabManager.tabs.isEmpty() && tabManager.getCurrent().browser != null) {
                tabManager.getCurrent().browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
            }
            return true;
        }

        validateIndices();
        if (!tabManager.tabs.isEmpty() && tabManager.getCurrent().browser != null) {
            tabManager.getCurrent().browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
        }

        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        super.tick();

        boolean needsRefresh = false;
        boolean needsSave = false;

        TabManager.BrowserTab currentTab = tabManager.getCurrent();
        if (currentTab != null && currentTab.browser != null) {

            currentTab.browser.executeJavaScript(
                    "(function() {" +
                            "  var v = document.querySelector('video');" +
                            "  var forced = document.querySelector('.forced-fs');" +
                            "  if(forced && v) {" +
                            "    v.style.position = 'fixed';" +
                            "    v.style.top = '0'; v.style.left = '0';" +
                            "    v.style.width = '100vw'; v.style.height = '100vh';" +
                            "    v.style.zIndex = '999999';" +
                            "  }" +
                            "})();",
                    currentTab.browser.getURL(), 0
            );
        }

        for (TabManager.BrowserTab tab : tabManager.tabs) {
            String oldTitle = tab.title;
            String oldUrl = tab.url;

            tab.updateTitle();

            if (tab.url != null && !tab.url.equals(oldUrl)) {
                needsSave = true;
            }

            if (tab.title != null && !tab.title.equals(oldTitle)) {
                needsRefresh = true;
            }
        }

        if (needsSave) {
            tabManager.save();
        }

        if (needsRefresh) {
            refreshGui();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        validateIndices();
        TabManager.BrowserTab currentTab = tabManager.getCurrent();
        if (currentTab == null || currentTab.browser == null) {
            super.render(guiGraphics, mouseX, mouseY, partial);
            return;
        }

        MCEFBrowser currentBrowser = currentTab.browser;

        if (isFullscreen) {
            guiGraphics.fill(0, 0, width, height, 0xFF000000);
        } else {
            try {
                this.renderBackground(guiGraphics, mouseX, mouseY, partial);
            } catch (Exception e) {
                guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
            }
        }

        if (currentBrowser.isTextureReady()) {
            ResourceLocation tex = currentBrowser.getTextureLocation();
            if (isFullscreen) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0, 0, width, height, width, height);
            } else {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, 20, UI_HEIGHT + 10, 0, 0, width - 40, height - UI_HEIGHT - 35, width - 40, height - UI_HEIGHT - 35);
            }
        }

        if (this.isFullscreen && this.fullscreenMsgTimer > 0) {
            MutableComponent fsHint = Component.literal("Press ")
                    .append(Component.literal("F12").withStyle(net.minecraft.ChatFormatting.YELLOW))
                    .append(" to exit Fullscreen");

            int textWidth = font.width(fsHint);
            guiGraphics.drawString(font, fsHint, (width / 2) - (textWidth / 2), 20, 0xFFFFFFFF, true);

            this.fullscreenMsgTimer--;
        }

        if (!isFullscreen) {
            if (urlBar != null) {
                if (!urlBar.isFocused() && !urlBar.getValue().equals(currentBrowser.getURL())) {
                    urlBar.setValue(currentBrowser.getURL());
                    urlBar.setCursorPosition(0);
                }
                urlBar.render(guiGraphics, mouseX, mouseY, partial);
            }
            super.render(guiGraphics, mouseX, mouseY, partial);
        }
    }

    private void handleUrlInput(String input) {
        String finalUrl = input.trim();
        if (finalUrl.isEmpty()) return;

        TabManager.BrowserTab currentTab = tabManager.getCurrent();

        if (finalUrl.contains(".") && !finalUrl.contains(" ")) {
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "https://" + finalUrl;
            }
        }
        else {
            String query = finalUrl.replace(" ", "+");
            finalUrl = "https://www.google.com/search?q=" + query;
        }

        if (currentTab != null && currentTab.browser != null) {
            currentTab.browser.loadURL(finalUrl);
            urlBar.setFocused(false);
        }
    }

    private void syncBrowserSizeToPip() {
        if (tabManager.getCurrent() != null && tabManager.getCurrent().browser != null) {

            int internalW = pipW;
            int internalH = pipH;

            tabManager.getCurrent().browser.resize(internalW, internalH);
        }
    }

    private void sendScaledInputToBrowser(double mx, double my, int btn, boolean press) {
        if (tabManager.getCurrent() == null || tabManager.getCurrent().browser == null) return;


        double relX = (mx - pipX) / (double) pipW;
        double relY = (my - pipY) / (double) pipH;

        int scale = (int) minecraft.getWindow().getGuiScale();
        int bx = (int)(relX * pipW * scale);
        int by = (int)(relY * pipH * scale);


        if (press) {
            tabManager.getCurrent().browser.sendMousePress(bx, by, btn);
        } else {
            tabManager.getCurrent().browser.sendMouseRelease(bx, by, btn);
        }
    }

    private void renderTabWidgets() {
        int startX = 108;
        for (int i = tabScrollOffset; i < tabManager.tabs.size(); i++) {
            if (startX + 85 > width - 50) break;
            final int index = i;
            TabManager.BrowserTab tab = tabManager.tabs.get(index);
            Component titleComp = (index == tabManager.activeIndex) ? Component.literal("• " + tab.title) : Component.literal(tab.title);
            addRenderableWidget(Button.builder(titleComp, b -> { tabManager.activeIndex = index; refreshGui(); }).bounds(startX, 2, 70, 18).build());
            addRenderableWidget(Button.builder(Component.literal("x"), b -> { tabManager.removeTab(index); validateIndices(); refreshGui(); this.setFocused(null); }).bounds(startX + 70, 2, 15, 18).build());
            startX += 90;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {

        if (pipActive && pipInteracting) {
            validateIndices();
            if (!tabManager.tabs.isEmpty()) {
                tabManager.getCurrent().browser.sendKeyTyped((char) event.codepoint(), event.modifiers());
                return true;
            }
        }

        if (urlBar != null && urlBar.isFocused()) {
            return urlBar.charTyped(event);
        }


        validateIndices();
        if (!tabManager.tabs.isEmpty()) {
            tabManager.getCurrent().browser.sendKeyTyped((char) event.codepoint(), event.modifiers());
            return true;
        }
        return false;
    }

    private void validateIndices() {
        if (tabManager.tabs.isEmpty()) {
            tabManager.addTab("https://www.google.com");
        }
        if (tabManager.activeIndex >= tabManager.tabs.size()) {
            tabManager.activeIndex = Math.max(0, tabManager.tabs.size() - 1);
        }
        if (tabScrollOffset >= tabManager.tabs.size()) {
            tabScrollOffset = Math.max(0, tabManager.tabs.size() - 1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean primary) {
        validateIndices();
        if (tabManager.tabs.isEmpty()) return super.mouseClicked(event, primary);

        double mx = event.x();
        double my = event.y();
        int btn = event.button();

        if (pipActive) {
            if (mx >= pipX && mx <= pipX + pipW && my >= pipY && my <= pipY + pipH) {

                tabManager.getCurrent().browser.setFocus(true);

                if (btn == 0) {

                    if (mx >= pipX + pipW - 20 && my >= pipY + pipH - 20) {
                        isResizing = true;
                        return true;
                    }

                    if (!pipInteracting || my <= pipY + 15) {
                        isDragging = true;
                        dragStartX = mx - pipX;
                        dragStartY = my - pipY;
                        return true;
                    }
                }

                if (pipInteracting) {
                    sendScaledInputToBrowser(mx, my, btn, true);
                    return true;
                } else {
                    pipInteracting = true;
                    return true;
                }
            } else {
                pipInteracting = false;
            }
        }

        tabManager.getCurrent().browser.setFocus(true);
        tabManager.getCurrent().browser.sendMousePress(getBX(mx), getBY(my), btn);

        return super.mouseClicked(event, primary);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isResizing) {
            pipW = (int) Math.max(100, event.x() - pipX);
            pipH = (int) Math.max(60, event.y() - pipY);
            syncBrowserSizeToPip();
            return true;
        }
        if (isDragging) {
            pipX = (int) (event.x() - dragStartX);
            pipY = (int) (event.y() - dragStartY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mx = event.x();
        double my = event.y();
        int btn = event.button();

        isDragging = false;
        isResizing = false;

        if (pipActive) {
            if (mx >= pipX && mx <= pipX + pipW && my >= pipY && my <= pipY + pipH) {
                if (pipInteracting) {
                    sendScaledInputToBrowser(mx, my, btn, false);
                    return true;
                }
            }
        }

        // --- NORMALER MODUS LOGIK ---
        validateIndices();
        if (!tabManager.tabs.isEmpty() && tabManager.getCurrent().browser != null) {

            tabManager.getCurrent().browser.sendMouseRelease(getBX(mx), getBY(my), btn);
        }

        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        validateIndices();
        if (tabManager.tabs.isEmpty()) {
            super.mouseMoved(mx, my);
            return;
        }

        if (pipActive && pipInteracting) {

            double relX = (mx - pipX) / (double) pipW;
            double relY = (my - pipY) / (double) pipH;
            int scale = (int) minecraft.getWindow().getGuiScale();

            tabManager.getCurrent().browser.sendMouseMove((int)(relX * pipW * scale), (int)(relY * pipH * scale));
        } else {

            tabManager.getCurrent().browser.sendMouseMove(getBX(mx), getBY(my));
        }

        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        validateIndices();
        if (tabManager.tabs.isEmpty()) return super.mouseScrolled(mx, my, h, v);


        if (pipActive && mx >= pipX && mx <= pipX + pipW && my >= pipY && my <= pipY + pipH) {
            if (pipInteracting) {
                double relX = (mx - pipX) / (double) pipW;
                double relY = (my - pipY) / (double) pipH;
                int scale = (int) minecraft.getWindow().getGuiScale();
                tabManager.getCurrent().browser.sendMouseWheel((int)(relX * pipW * scale), (int)(relY * pipH * scale), v, 0);
                return true;
            }
        }


        tabManager.getCurrent().browser.sendMouseWheel(getBX(mx), getBY(my), v, 0);
        return super.mouseScrolled(mx, my, h, v);
    }

    private int getBX(double x) { return (int) ((isFullscreen ? x : x - 20) * minecraft.getWindow().getGuiScale()); }
    private int getBY(double y) { return (int) ((isFullscreen ? y : y - (UI_HEIGHT + 10)) * minecraft.getWindow().getGuiScale()); }

    private void toggleFullscreen() {
        this.isFullscreen = !this.isFullscreen;
        this.fullscreenMsgTimer = 80;

        if (pipActive) {
            syncBrowserSizeToPip();
        } else {
            resizeAllBrowsers();
        }

    }


    private void resizeAllBrowsers() {
        int scale = (int) minecraft.getWindow().getGuiScale();
        int bW = isFullscreen ? width : width - 40;
        int bH = isFullscreen ? height : height - UI_HEIGHT - 35;

        for (TabManager.BrowserTab t : tabManager.tabs) {
            if (t.browser != null) {
                t.browser.resize(bW * scale, bH * scale);
            }
        }
    }

    protected void refreshGui() {
        this.clearWidgets();
        this.init(this.minecraft, this.width, this.height);
    }

    @Override
    public void removed() {

        validateIndices();
        if (!tabManager.tabs.isEmpty()) {
            tabManager.getCurrent().browser.setFocus(false);
        }
        super.removed();
    }

    @Override
    public void onClose() {
        tabManager.save();
        super.onClose();
    }
}