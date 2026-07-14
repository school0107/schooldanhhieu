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
        
        applyTagPermissions(player);
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
            
            removeTagPermissions(player);
            playerTags.put(player.getUniqueId(), tagId);
            applyTagPermissions(player);
            savePlayerTag(player);
            
            String msg = Schooltag.getInstance().getConfigManager().getMessage("tag-selected")
                .replace("{tag}", Schooltag.getInstance().getConfigManager().getTagName(tagId));
            player.sendMessage(ColorUtils.colorize(msg));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra khi chọn danh hiệu!"));
        }
    }

    public void removeTag(Player player) {
        try {
            UUID uuid = player.getUniqueId();
            removeTagPermissions(player);
            playerTags.remove(uuid);
            savePlayerTag(player);
            
            if (hasTagPermission(player, "default")) {
                playerTags.put(uuid, "default");
                applyTagPermissions(player);
                savePlayerTag(player);
            }
            
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

    public void applyTagEffects(Player player) {
        // Effects handled in PlayerListener
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
}