package com.schooltag.managers;

import com.schooltag.Schooltag;
import com.schooltag.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private final Map<UUID, List<String>> unlockedTags = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private File playerDataFile;
    private FileConfiguration playerData;
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 45;

    public TagManager() {
        playerDataFile = new File(Schooltag.getInstance().getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            Schooltag.getInstance().saveResource("playerdata.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void loadPlayerTags() {
        if (playerData == null) return;
        
        for (String uuidStr : playerData.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String tag = playerData.getString(uuidStr + ".current");
            List<String> unlocked = playerData.getStringList(uuidStr + ".unlocked");
            
            if (tag != null) {
                playerTags.put(uuid, tag);
            }
            if (!unlocked.isEmpty()) {
                unlockedTags.put(uuid, unlocked);
            }
        }
    }

    public void savePlayerTags() {
        if (playerData == null) return;
        
        for (Map.Entry<UUID, String> entry : playerTags.entrySet()) {
            String uuid = entry.getKey().toString();
            playerData.set(uuid + ".current", entry.getValue());
        }
        
        for (Map.Entry<UUID, List<String>> entry : unlockedTags.entrySet()) {
            String uuid = entry.getKey().toString();
            playerData.set(uuid + ".unlocked", entry.getValue());
        }
        
        try {
            playerData.save(playerDataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerTag(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerData.contains(uuid.toString())) {
            String tag = playerData.getString(uuid.toString() + ".current");
            if (tag != null && !tag.isEmpty()) {
                playerTags.put(uuid, tag);
            }
            
            List<String> unlocked = playerData.getStringList(uuid.toString() + ".unlocked");
            if (!unlocked.isEmpty()) {
                unlockedTags.put(uuid, unlocked);
            }
        }
        
        // Check and unlock tags based on permissions
        checkAndUpdateUnlockedTags(player);
        
        // Apply default tag if no tag selected or current tag not unlocked
        String currentTag = playerTags.get(uuid);
        if (currentTag == null || !hasTagPermission(player, currentTag)) {
            if (hasTagPermission(player, "default")) {
                playerTags.put(uuid, "default");
            } else {
                playerTags.remove(uuid);
            }
        }
        
        // Apply tag permissions
        applyTagPermissions(player);
    }

    public void checkAndUpdateUnlockedTags(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> unlocked = unlockedTags.getOrDefault(uuid, new ArrayList<>());
        boolean updated = false;
        
        // Check all tags
        for (String tagId : Schooltag.getInstance().getConfigManager().getAllTags()) {
            if (hasTagPermission(player, tagId) && !unlocked.contains(tagId)) {
                unlocked.add(tagId);
                updated = true;
            }
        }
        
        if (updated) {
            unlockedTags.put(uuid, unlocked);
            savePlayerTag(player);
        }
    }

    public boolean hasTagPermission(Player player, String tagId) {
        String permission = Schooltag.getInstance().getConfigManager().getTagPermission(tagId);
        if (permission == null || permission.isEmpty()) return false;
        return player.hasPermission(permission);
    }

    public void savePlayerTag(Player player) {
        UUID uuid = player.getUniqueId();
        String uuidStr = uuid.toString();
        
        if (playerTags.containsKey(uuid)) {
            playerData.set(uuidStr + ".current", playerTags.get(uuid));
        }
        
        if (unlockedTags.containsKey(uuid)) {
            playerData.set(uuidStr + ".unlocked", unlockedTags.get(uuid));
        }
        
        try {
            playerData.save(playerDataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyTagPermissions(Player player) {
        // Remove old attachments
        if (attachments.containsKey(player.getUniqueId())) {
            attachments.get(player.getUniqueId()).remove();
            attachments.remove(player.getUniqueId());
        }
        
        String tagId = getPlayerTag(player);
        if (tagId == null) return;
        
        List<String> grantPermissions = Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
        if (grantPermissions.isEmpty()) return;
        
        // Create new attachment
        PermissionAttachment attachment = player.addAttachment(Schooltag.getInstance());
        attachments.put(player.getUniqueId(), attachment);
        
        // Grant permissions
        if (grantPermissions.contains("*")) {
            // Grant all permissions - we can't actually grant all, so we'll grant specific ones
            // Or we can use a wildcard permission if supported
            attachment.setPermission("*", true);
        } else {
            for (String perm : grantPermissions) {
                attachment.setPermission(perm, true);
            }
        }
        
        // Send message
        String msg = Schooltag.getInstance().getConfigManager().getMessage("permissions-granted")
            .replace("{permissions}", String.join(", ", grantPermissions));
        player.sendMessage(ColorUtils.colorize(msg));
    }

    public void removeTagPermissions(Player player) {
        if (attachments.containsKey(player.getUniqueId())) {
            attachments.get(player.getUniqueId()).remove();
            attachments.remove(player.getUniqueId());
            
            String msg = Schooltag.getInstance().getConfigManager().getMessage("permissions-removed");
            player.sendMessage(ColorUtils.colorize(msg));
        }
    }

    public void openTagMenu(Player player) {
        openTagMenu(player, 0);
    }

    public void openTagMenu(Player player, int page) {
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

        // Get all tags
        List<String> allTags = new ArrayList<>(Schooltag.getInstance().getConfigManager().getAllTags());
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allTags.size());

        // Add tag items
        for (int i = start; i < end; i++) {
            String tagId = allTags.get(i);
            boolean hasPerm = hasTagPermission(player, tagId);
            boolean isSelected = tagId.equals(getPlayerTag(player));
            
            ItemStack item = createTagItem(tagId, hasPerm, isSelected, player);
            inv.setItem(i - start, item);
        }

        // Add control buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getPrevMaterial()));
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getPrevName()));
            prev.setItemMeta(prevMeta);
            inv.setItem(48, prev);
        }

        if (end < allTags.size()) {
            ItemStack next = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getNextMaterial()));
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getNextName()));
            next.setItemMeta(nextMeta);
            inv.setItem(50, next);
        }

        // Close button
        ItemStack close = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getCloseMaterial()));
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getCloseName()));
        close.setItemMeta(closeMeta);
        inv.setItem(49, close);

        player.openInventory(inv);
    }

    private ItemStack createTagItem(String tagId, boolean hasPerm, boolean selected, Player player) {
        ItemStack item = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getTagMaterial(tagId)));
        ItemMeta meta = item.getItemMeta();
        
        String name = Schooltag.getInstance().getConfigManager().getTagName(tagId);
        meta.setDisplayName(ColorUtils.colorize(name));
        
        List<String> lore = new ArrayList<>();
        lore.addAll(Schooltag.getInstance().getConfigManager().getTagLore(tagId));
        
        // Add grant permissions to lore
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
    }

    public void selectTag(Player player, String tagId) {
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
        
        // Remove old tag permissions
        removeTagPermissions(player);
        
        // Set new tag
        playerTags.put(player.getUniqueId(), tagId);
        
        // Apply new tag permissions
        applyTagPermissions(player);
        
        // Apply effects
        applyTagEffects(player);
        
        // Save
        savePlayerTag(player);
        
        // Send message
        String msg = Schooltag.getInstance().getConfigManager().getMessage("tag-selected")
            .replace("{tag}", Schooltag.getInstance().getConfigManager().getTagName(tagId));
        player.sendMessage(ColorUtils.colorize(msg));
    }

    public void removeTag(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Remove permissions
        removeTagPermissions(player);
        
        // Remove tag
        playerTags.remove(uuid);
        applyTagEffects(player);
        savePlayerTag(player);
        
        // Auto select default if has permission
        if (hasTagPermission(player, "default")) {
            playerTags.put(uuid, "default");
            applyTagPermissions(player);
            savePlayerTag(player);
        }
        
        // Send message
        player.sendMessage(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getMessage("tag-removed")));
    }

    public String getPlayerTag(Player player) {
        return playerTags.get(player.getUniqueId());
    }

    public boolean isTagUnlocked(Player player, String tagId) {
        return hasTagPermission(player, tagId);
    }

    public void applyTagEffects(Player player) {
        String tagId = getPlayerTag(player);
        if (tagId == null) return;
        
        // Apply effects through listeners
        // The actual effects are handled in PlayerListener
    }

    public void handleMenuClick(Player player, int slot) {
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            List<String> allTags = new ArrayList<>(Schooltag.getInstance().getConfigManager().getAllTags());
            int index = currentPage * ITEMS_PER_PAGE + slot;
            if (index < allTags.size()) {
                String tagId = allTags.get(index);
                selectTag(player, tagId);
                openTagMenu(player, currentPage);
            }
        } else if (slot == 48) {
            openTagMenu(player, currentPage - 1);
        } else if (slot == 50) {
            openTagMenu(player, currentPage + 1);
        } else if (slot == 49) {
            player.closeInventory();
        }
    }
}
