package com.schooltag;

import com.schooltag.commands.TagCommand;
import com.schooltag.commands.DanhHieuCommand;
import com.schooltag.listeners.PlayerListener;
import com.schooltag.listeners.MenuListener;
import com.schooltag.managers.TagManager;
import com.schooltag.managers.PlaceholderManager;
import com.schooltag.utils.ConfigManager;
import com.schooltag.placeholder.SchooltagExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Schooltag extends JavaPlugin {
    private static Schooltag instance;
    private TagManager tagManager;
    private ConfigManager configManager;
    private PlaceholderManager placeholderManager;

    @Override
    public void onEnable() {
        try {
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
            
            // Register PlaceholderAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new SchooltagExpansion().register();
                getLogger().info("Đã đăng ký với PlaceholderAPI!");
            } else {
                getLogger().warning("PlaceholderAPI không tìm thấy! Placeholder sẽ không hoạt động.");
            }
            
            getLogger().info("=========================================");
            getLogger().info("  Schooltag v1.0.0 đã được kích hoạt!");
            getLogger().info("  /tags hoặc /danhhieu để mở menu");
            getLogger().info("  Placeholder: %schooltag_name%");
            getLogger().info("=========================================");
        } catch (Exception e) {
            getLogger().severe("=========================================");
            getLogger().severe("  LỖI KHI KÍCH HOẠT SCHOOLTAG!");
            getLogger().severe("  Vui lòng kiểm tra config và thử lại");
            getLogger().severe("=========================================");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (tagManager != null) {
                tagManager.savePlayerTags();
            }
            getLogger().info("Schooltag đã được tắt!");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            reloadConfig();
            configManager = new ConfigManager();
            tagManager.loadPlayerTags();
            getLogger().info("Schooltag đã được reload!");
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Lỗi khi reload Schooltag!");
        }
    }
}