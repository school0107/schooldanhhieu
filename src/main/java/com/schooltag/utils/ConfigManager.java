package com.schooltag.utils;

import com.schooltag.Schooltag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {
    private final FileConfiguration config;
    
    public ConfigManager() {
        this.config = Schooltag.getInstance().getConfig();
    }
    
    public String getPrefix() {
        return config.getString("settings.prefix", "&6&lSchoolTag &8» &r");
    }
    
    public int getAutoSave() {
        return config.getInt("settings.auto-save", 5);
    }
    
    public Set<String> getAllTags() {
        ConfigurationSection tags = config.getConfigurationSection("tags");
        if (tags == null) return new HashSet<>();
        return tags.getKeys(false);
    }
    
    public boolean tagExists(String tagId) {
        return config.contains("tags." + tagId);
    }
    
    public String getTagName(String tagId) {
        return config.getString("tags." + tagId + ".name", tagId);
    }
    
    public String getTagMaterial(String tagId) {
        return config.getString("tags." + tagId + ".material", "NAME_TAG");
    }
    
    public int getTagSlot(String tagId) {
        return config.getInt("tags." + tagId + ".slot", -1);
    }
    
    public List<String> getTagLore(String tagId) {
        return config.getStringList("tags." + tagId + ".lore");
    }
    
    public String getTagPermission(String tagId) {
        return config.getString("tags." + tagId + ".permission", "");
    }
    
    public List<String> getTagGrantPermissions(String tagId) {
        return config.getStringList("tags." + tagId + ".grant-permissions");
    }
    
    public double getTagDamageMultiplier(String tagId) {
        return config.getDouble("tags." + tagId + ".effects.damage-multiplier", 1.0);
    }
    
    public double getTagHealthMultiplier(String tagId) {
        return config.getDouble("tags." + tagId + ".effects.health-multiplier", 1.0);
    }
    
    public int getTagPriority(String tagId) {
        return config.getInt("tags." + tagId + ".priority", 0);
    }
    
    public String getGuiTitle() {
        return config.getString("gui.title", "&6&l🎯 Danh Hiệu");
    }
    
    public int getGuiSize() {
        return config.getInt("gui.size", 54);
    }
    
    public String getFillerMaterial() {
        return config.getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE");
    }
    
    public String getFillerName() {
        return config.getString("gui.filler.name", " ");
    }
    
    public String getPrevMaterial() {
        return config.getString("gui.control.previous.material", "ARROW");
    }
    
    public String getPrevName() {
        return config.getString("gui.control.previous.name", "&6◀ Trang Trước");
    }
    
    public String getNextMaterial() {
        return config.getString("gui.control.next.material", "ARROW");
    }
    
    public String getNextName() {
        return config.getString("gui.control.next.name", "&6Trang Sau ▶");
    }
    
    public String getCloseMaterial() {
        return config.getString("gui.control.close.material", "BARRIER");
    }
    
    public String getCloseName() {
        return config.getString("gui.control.close.name", "&c✖ Đóng");
    }
    
    public List<String> getSelectedLore() {
        return config.getStringList("gui.selected.lore");
    }
    
    public List<String> getUnselectedLore() {
        return config.getStringList("gui.unselected.lore");
    }
    
    public String getLockMaterial() {
        return config.getString("gui.lock.material", "BARRIER");
    }
    
    public String getLockName() {
        return config.getString("gui.lock.name", "&c&l🔒 Khóa");
    }
    
    public List<String> getLockLore() {
        return config.getStringList("gui.lock.lore");
    }
    
    public String getMessage(String key) {
        return config.getString("messages." + key, "&cKhông tìm thấy message: " + key);
    }
}