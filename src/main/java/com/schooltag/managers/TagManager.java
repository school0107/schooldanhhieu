package com.schooltag.managers;

import com.schooltag.Schooltag;
import com.schooltag.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.util.*;

public class TagManager {
    private final Map<UUID, String> playerTags = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private File playerDataFile;
    private FileConfiguration playerData;
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 45;

    public TagManager() {
        try {
            playerDataFile = new File(Schooltag.getInstance().getDataFolder(), "playerdata.yml");
            if (!playerDataFile.exists()) {
                Schooltag.getInstance().saveResource("playerdata.yml", false);
            }
            playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        } catch (Exception e) {
            e.printStackTrace();
            Schooltag.getInstance().getLogger().severe("Không thể tải file playerdata.yml!");
        }
    }

    public void loadPlayerTags() {
        if (playerData == null) return;
        
        try {
            for (String uuidStr : playerData.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String tag = playerData.getString(uuidStr + ".current");
                if (tag != null && !tag.isEmpty()) {
                    playerTags.put(uuid, tag);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerTags() {
        if (playerData == null) return;
        
        try {
            for (Map.Entry<UUID, String> entry : playerTags.entrySet()) {
                String uuid = entry.getKey().toString();
                playerData.set(uuid + ".current", entry.getValue());
            }
            playerData.save(playerDataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerTag(Player player) {
        UUID uuid = player.getUniqueId();
        
        try {
            if (playerData != null && playerData.contains(uuid.toString())) {
                String tag = playerData.getString(uuid.toString() + ".current");
                if (tag != null && !tag.isEmpty()) {
                    playerTags.put(uuid, tag);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String currentTag = playerTags.get(uuid);
        if (currentTag == null || !Schooltag.getInstance().getConfigManager().tagExists(currentTag)) {
            if (hasTagPermission(player, "default")) {
                playerTags.put(uuid, "default");
            } else {
                playerTags.remove(uuid);
            }
        }
        
        // Apply tag permissions và effects
        applyTagPermissions(player);
        applyTagEffects(player);
    }

    public boolean hasTagPermission(Player player, String tagId) {
        try {
            String permission = Schooltag.getInstance().getConfigManager().getTagPermission(tagId);
            if (permission == null || permission.isEmpty()) return false;
            return player.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    public void savePlayerTag(Player player) {
        if (playerData == null) return;
        
        try {
            UUID uuid = player.getUniqueId();
            String uuidStr = uuid.toString();
            
            if (playerTags.containsKey(uuid)) {
                playerData.set(uuidStr + ".current", playerTags.get(uuid));
                playerData.save(playerDataFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyTagPermissions(Player player) {
        // Remove old attachments
        if (attachments.containsKey(player.getUniqueId())) {
            try {
                attachments.get(player.getUniqueId()).remove();
            } catch (Exception e) {}
            attachments.remove(player.getUniqueId());
        }
        
        String tagId = getPlayerTag(player);
        if (tagId == null) return;
        
        List<String> grantPermissions = Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
        if (grantPermissions.isEmpty()) return;
        
        try {
            PermissionAttachment attachment = player.addAttachment(Schooltag.getInstance());
            attachments.put(player.getUniqueId(), attachment);
            
            if (grantPermissions.contains("*")) {
                attachment.setPermission("*", true);
            } else {
                for (String perm : grantPermissions) {
                    attachment.setPermission(perm, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeTagPermissions(Player player) {
        if (attachments.containsKey(player.getUniqueId())) {
            try {
                attachments.get(player.getUniqueId()).remove();
            } catch (Exception e) {}
            attachments.remove(player.getUniqueId());
        }
    }

    public void applyTagEffects(Player player) {
        String tagId = getPlayerTag(player);
        if (tagId == null) {
            resetPlayerStats(player);
            return;
        }
        
        // Áp dụng tăng máu
        double healthMultiplier = Schooltag.getInstance().getConfigManager()
            .getTagHealthMultiplier(tagId);
        
        if (healthMultiplier > 1.0) {
            applyHealthBoost(player, healthMultiplier);
        } else {
            resetHealth(player);
        }
    }

    private void applyHealthBoost(Player player, double multiplier) {
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                double baseHealth = 20.0;
                double newMaxHealth = baseHealth * multiplier;
                
                // Lưu tỷ lệ máu hiện tại
                double currentHealth = player.getHealth();
                double currentMax = attribute.getValue();
                double healthRatio = currentHealth / currentMax;
                
                // Cập nhật máu tối đa mới
                attribute.setBaseValue(newMaxHealth);
                
                // Cập nhật máu hiện tại theo tỷ lệ
                double newHealth = newMaxHealth * healthRatio;
                player.setHealth(Math.min(newHealth, newMaxHealth));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetHealth(Player player) {
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                attribute.setBaseValue(20.0);
                if (player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetPlayerStats(Player player) {
        resetHealth(player);
    }

    public void openTagMenu(Player player) {
        openTagMenu(player, 0);
    }

    public void openTagMenu(Player player, int page) {
        try {
            this.currentPage = page;
            Inventory inv = Bukkit.createInventory(null, 54, 
                ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getGuiTitle()));
            
            // Fill background
            ItemStack filler = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getFillerMaterial()));
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getFillerName()));
            filler.setItemMeta(fillerMeta);
            
            for (int i = 0; i < 54; i++) {
                inv.setItem(i, filler);
            }

            // Get all tags and sort by slot
            List<String> allTags = new ArrayList<>(Schooltag.getInstance().getConfigManager().getAllTags());
            Map<Integer, String> slotMap = new HashMap<>();
            
            for (String tagId : allTags) {
                int slot = Schooltag.getInstance().getConfigManager().getTagSlot(tagId);
                if (slot >= 0 && slot < 54) {
                    slotMap.put(slot, tagId);
                }
            }
            
            // Add tags to their specific slots
            for (Map.Entry<Integer, String> entry : slotMap.entrySet()) {
                int slot = entry.getKey();
                String tagId = entry.getValue();
                
                boolean hasPerm = hasTagPermission(player, tagId);
                boolean isSelected = tagId.equals(getPlayerTag(player));
                
                ItemStack item = createTagItem(tagId, hasPerm, isSelected, player);
                inv.setItem(slot, item);
            }

            // Add control buttons
            ItemStack prev = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getPrevMaterial()));
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getPrevName()));
            prev.setItemMeta(prevMeta);
            inv.setItem(48, prev);

            ItemStack next = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getNextMaterial()));
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getNextName()));
            next.setItemMeta(nextMeta);
            inv.setItem(50, next);

            ItemStack close = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getCloseMaterial()));
            ItemMeta closeMeta = close.getItemMeta();
            closeMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getCloseName()));
            close.setItemMeta(closeMeta);
            inv.setItem(49, close);

            player.openInventory(inv);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra khi mở menu!"));
        }
    }

    private ItemStack createTagItem(String tagId, boolean hasPerm, boolean selected, Player player) {
        try {
            ItemStack item = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getTagMaterial(tagId)));
            ItemMeta meta = item.getItemMeta();
            
            String name = Schooltag.getInstance().getConfigManager().getTagName(tagId);
            meta.setDisplayName(ColorUtils.colorize(name));
            
            List<String> lore = new ArrayList<>();
            lore.addAll(Schooltag.getInstance().getConfigManager().getTagLore(tagId));
            
            // Thêm thông tin hiệu ứng
            double damageMult = Schooltag.getInstance().getConfigManager().getTagDamageMultiplier(tagId);
            double healthMult = Schooltag.getInstance().getConfigManager().getTagHealthMultiplier(tagId);
            
            if (damageMult > 1.0) {
                lore.add(ColorUtils.colorize("&c⚔️ Tăng " + (int)((damageMult - 1) * 100) + "% sát thương"));
            }
            if (healthMult > 1.0) {
                lore.add(ColorUtils.colorize("&a❤️ Tăng " + (int)((healthMult - 1) * 100) + "% máu"));
            }
            
            // Thêm thông tin permission được cấp
            List<String> grantPerms = Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
            String grantPermsStr = grantPerms.isEmpty() ? "Không có" : String.join(", ", grantPerms);
            
            if (selected) {
                lore.add("");
                List<String> selectedLore = Schooltag.getInstance().getConfigManager().getSelectedLore();
                for (String line : selectedLore) {
                    line = line.replace("{grant-permissions}", grantPermsStr);
                    lore.add(ColorUtils.colorize(line));
                }
            } else if (hasPerm) {
                lore.add("");
                List<String> unselectedLore = Schooltag.getInstance().getConfigManager().getUnselectedLore();
                for (String line : unselectedLore) {
                    line = line.replace("{grant-permissions}", grantPermsStr);
                    lore.add(ColorUtils.colorize(line));
                }
            } else {
                lore.add("");
                List<String> lockLore = Schooltag.getInstance().getConfigManager().getLockLore();
                String permission = Schooltag.getInstance().getConfigManager().getTagPermission(tagId);
                for (String line : lockLore) {
                    line = line.replace("{permission}", permission);
                    lore.add(ColorUtils.colorize(line));
                }
            }
            
            meta.setLore(ColorUtils.colorizeList(lore));
            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack(Material.BARRIER);
        }
    }

    public void selectTag(Player player, String tagId) {
        try {
            // Check if player has permission for this tag
            if (!hasTagPermission(player, tagId)) {
                player.sendMessage(ColorUtils.colorize(
                    Schooltag.getInstance().getConfigManager().getMessage("tag-locked")));
                return;
            }
            
            if (!Schooltag.getInstance().getConfigManager().tagExists(tagId)) {
                player.sendMessage(ColorUtils.colorize(
                    Schooltag.getInstance().getConfigManager().getMessage("tag-not-exist")));
                return;
            }
            
            // Remove old tag permissions và effects
            String oldTag = getPlayerTag(player);
            if (oldTag != null) {
                removeTagPermissions(player);
                resetPlayerStats(player);
            }
            
            // Set new tag
            playerTags.put(player.getUniqueId(), tagId);
            
            // Apply new tag permissions và effects
            applyTagPermissions(player);
            applyTagEffects(player);
            
            // Save
            savePlayerTag(player);
            
            // Send message
            String msg = Schooltag.getInstance().getConfigManager().getMessage("tag-selected")
                .replace("{tag}", Schooltag.getInstance().getConfigManager().getTagName(tagId));
            player.sendMessage(ColorUtils.colorize(msg));
            
            // Show effects info
            double damageMult = Schooltag.getInstance().getConfigManager().getTagDamageMultiplier(tagId);
            double healthMult = Schooltag.getInstance().getConfigManager().getTagHealthMultiplier(tagId);
            
            List<String> effects = new ArrayList<>();
            if (damageMult > 1.0) {
                effects.add(ColorUtils.colorize("&c⚔️ +" + (int)((damageMult - 1) * 100) + "% sát thương"));
            }
            if (healthMult > 1.0) {
                effects.add(ColorUtils.colorize("&a❤️ +" + (int)((healthMult - 1) * 100) + "% máu"));
            }
            
            if (!effects.isEmpty()) {
                player.sendMessage(ColorUtils.colorize("&7Hiệu ứng: " + String.join(", ", effects)));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra khi chọn danh hiệu!"));
        }
    }

    public void removeTag(Player player) {
        try {
            UUID uuid = player.getUniqueId();
            
            // Remove permissions và effects
            removeTagPermissions(player);
            resetPlayerStats(player);
            
            // Remove tag
            playerTags.remove(uuid);
            savePlayerTag(player);
            
            // Auto select default if has permission
            if (hasTagPermission(player, "default")) {
                playerTags.put(uuid, "default");
                applyTagPermissions(player);
                applyTagEffects(player);
                savePlayerTag(player);
            }
            
            // Send message
            player.sendMessage(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getMessage("tag-removed")));
            
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra khi gỡ danh hiệu!"));
        }
    }

    public String getPlayerTag(Player player) {
        return playerTags.get(player.getUniqueId());
    }

    public boolean isTagUnlocked(Player player, String tagId) {
        return hasTagPermission(player, tagId);
    }

    public void handleMenuClick(Player player, int slot) {
        try {
            // Check if clicked slot has a tag
            String clickedTag = null;
            for (String tagId : Schooltag.getInstance().getConfigManager().getAllTags()) {
                int tagSlot = Schooltag.getInstance().getConfigManager().getTagSlot(tagId);
                if (tagSlot == slot) {
                    clickedTag = tagId;
                    break;
                }
            }
            
            if (clickedTag != null) {
                selectTag(player, clickedTag);
                openTagMenu(player, currentPage);
            } else if (slot == 48) {
                // Previous page - not used in slot mode
            } else if (slot == 50) {
                // Next page - not used in slot mode
            } else if (slot == 49) {
                player.closeInventory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Reset player stats khi plugin disable
    public void resetAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetHealth(player);
            removeTagPermissions(player);
        }
    }
}