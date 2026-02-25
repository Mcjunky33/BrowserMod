package de.mcjunky33;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File BASE_DIR = new File(Minecraft.getInstance().gameDirectory, "config/mcef/browsermod");
    public static final File FILE = new File(BASE_DIR, "browsertabs.json");
    public static final File MISC_FILE = new File(BASE_DIR, "miscellaneous.json");
    public static final File WELCOME_HTML = new File(BASE_DIR, "HelloUser.html");

    public List<BrowserTab> tabs = new ArrayList<>();
    public int activeIndex = 0;

    public static class BrowserTab {
        public String url;
        public String title;
        public transient MCEFBrowser browser;

        public BrowserTab(String url) {
            this.url = url;
            this.title = extractDomain(url);
        }

        public void updateTitle() {
            if (browser != null && browser.getURL() != null && !browser.getURL().isEmpty() && !browser.getURL().equals("about:blank")) {
                this.url = browser.getURL();
            }
            this.title = extractDomain(this.url);
        }

        private String extractDomain(String rawUrl) {
            if (rawUrl == null || rawUrl.isEmpty() || rawUrl.equals("about:blank")) return "Loading...";
            if (rawUrl.contains("HelloUser.html")) return "Welcome";
            String domain = rawUrl.replaceFirst("^(https?://)?(www\\.)?", "");
            int slashIndex = domain.indexOf('/');
            if (slashIndex > 0) domain = domain.substring(0, slashIndex);
            if (domain.isEmpty()) return "New Tab";
            return domain.length() > 15 ? domain.substring(0, 13) + ".." : domain;
        }
    }

    public static class SaveContainer {
        int activeIndex;
        List<BrowserTab> tabs;
    }

    public void load() {
        if (!BASE_DIR.exists()) BASE_DIR.mkdirs();

        if (FILE.exists()) {
            try (Reader r = new FileReader(FILE)) {
                SaveContainer data = GSON.fromJson(r, SaveContainer.class);
                if (data != null && data.tabs != null) {
                    this.tabs = new ArrayList<>();
                    for (BrowserTab t : data.tabs) {
                        if (t.url != null && !t.url.isEmpty()) {
                            this.tabs.add(t);
                        }
                    }
                    this.activeIndex = data.activeIndex;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (checkIsNewUser()) {
            reloadWelcomePage();
            setNewUserFalse();
        } else if (tabs.isEmpty()) {
            tabs.add(new BrowserTab("https://www.google.com"));
        }

        if (activeIndex >= tabs.size()) activeIndex = 0;

        for (BrowserTab tab : tabs) {
            if (tab.browser == null) tab.browser = MCEF.createBrowser(tab.url, false);
            tab.updateTitle();
        }
    }


    public void reloadWelcomePage() {
        prepareAndLoadWelcomePage();
        save();
    }


    public void prepareAndLoadWelcomePage() {
        try {
            String browserKey = getMappingName("key.browsermodmcef.toggle_browser");
            String pipKey = getMappingName("key.browsermodmcef.toggle_pip");
            String upKey = getMappingName("key.browsermodmcef.scroll_up");
            String downKey = getMappingName("key.browsermodmcef.scroll_down");

            InputStream is = getClass().getResourceAsStream("/assets/browsermodmcef/HelloUser.html");
            if (is != null) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                content = content.replace("KEY_BROWSER", browserKey)
                        .replace("KEY_PIP", pipKey)
                        .replace("KEY_SCROLL_UP", upKey)
                        .replace("KEY_SCROLL_DOWN", downKey);

                if (!WELCOME_HTML.getParentFile().exists()) {
                    WELCOME_HTML.getParentFile().mkdirs();
                }

                try (FileWriter fw = new FileWriter(WELCOME_HTML, false)) {
                    fw.write(content);
                    fw.flush();
                }

                String welcomeUrl = WELCOME_HTML.toURI().toString();

                int existingTabIndex = -1;
                for (int i = 0; i < tabs.size(); i++) {
                    if (tabs.get(i).url.equals(welcomeUrl)) {
                        existingTabIndex = i;
                        break;
                    }
                }

                if (existingTabIndex != -1) {
                    activeIndex = existingTabIndex;
                    if (tabs.get(existingTabIndex).browser != null) {
                        tabs.get(existingTabIndex).browser.loadURL(welcomeUrl);
                    }
                } else {
                    addTab(welcomeUrl);
                }

                save();

            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMappingName(String key) {
        return Arrays.stream(Minecraft.getInstance().options.keyMappings)
                .filter(m -> m.getName().equals(key))
                .findFirst()
                .map(KeyMapping::getTranslatedKeyMessage)
                .map(Component::getString)
                .orElse("NONE");
    }

    private boolean checkIsNewUser() {
        if (!MISC_FILE.exists()) return true;
        try (FileReader reader = new FileReader(MISC_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return json == null || !json.has("newUser") || json.get("newUser").getAsBoolean();
        } catch (Exception e) { return true; }
    }

    private void setNewUserFalse() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("newUser", false);
            try (FileWriter writer = new FileWriter(MISC_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void save() {
        for (BrowserTab tab : tabs) {
            if (tab.browser != null) {
                String currentUrl = tab.browser.getURL();
                if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals("about:blank")) {
                    tab.url = currentUrl;
                }
            }
        }
        tabs.removeIf(t -> t.url == null || t.url.isEmpty());
        try (Writer w = new FileWriter(FILE)) {
            SaveContainer data = new SaveContainer();
            data.activeIndex = this.activeIndex;
            data.tabs = this.tabs;
            GSON.toJson(data, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void addTab(String url) {
        BrowserTab newTab = new BrowserTab(url);
        newTab.browser = MCEF.createBrowser(url, false);
        newTab.updateTitle();
        tabs.add(newTab);
        activeIndex = tabs.size() - 1;
        save();
    }

    public void removeTab(int index) {
        if (tabs.size() > 1) {
            if (tabs.get(index).browser != null) tabs.get(index).browser.close();
            tabs.remove(index);
            if (activeIndex >= tabs.size()) activeIndex = tabs.size() - 1;
            save();
        }
    }

    public BrowserTab getCurrent() {
        if (activeIndex >= tabs.size() || activeIndex < 0) activeIndex = 0;
        return tabs.get(activeIndex);
    }
}