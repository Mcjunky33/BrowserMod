package de.mcjunky33;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TabManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File FILE = new File(Minecraft.getInstance().gameDirectory, "config/mcef/browsermod/browsertabs.json");

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

        if (tabs.isEmpty()) {
            tabs.add(new BrowserTab("https://www.google.com"));
        }

        if (activeIndex >= tabs.size()) activeIndex = 0;

        for (BrowserTab tab : tabs) {
            if (tab.browser == null) tab.browser = MCEF.createBrowser(tab.url, false);
            tab.updateTitle();
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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