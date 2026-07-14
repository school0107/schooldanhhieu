package com.schooltag;

import com.schooltag.commands.TagCommand;
import com.schooltag.commands.DanhHieuCommand;
import com.schooltag.listeners.PlayerListener;
import com.schooltag.listeners.MenuListener;
import com.schooltag.managers.TagManager;
import com.schooltag.managers.PlaceholderManager;
import com.schooltag.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Schooltag extends JavaPlugin {
    private static Schooltag instance;
    private TagManager tagManager;
    private ConfigManager configManager;
    private PlaceholderManager placeholderManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load config
        saveDefaultConfig();
        reloadConfig();
        
        // Initialize managers
        configManager = new ConfigManager();
        tagManager = new TagManager();
        placeholderManager = new PlaceholderManager();
        
        // Register commands
        getCommand("tags").setExecutor(new TagCommand());
        getCommand("danhhieu").setExecutor(new DanhHieuCommand());
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        
        // Load data
        tagManager.loadPlayerTags();
        
        getLogger().info("Schooltag đã được kích hoạt!");
    }

    @Override
    public void onDisable() {
        if (tagManager != null) {
            tagManager.savePlayerTags();
        }
        getLogger().info("Schooltag đã được tắt!");
    }

    public static Schooltag getInstance() {
        return instance;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public void reload() {
        reloadConfig();
        configManager = new ConfigManager();
        tagManager.loadPlayerTags();
    }
}