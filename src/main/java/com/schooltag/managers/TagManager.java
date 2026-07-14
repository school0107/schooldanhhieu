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
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TagManager {
    private final Map<UUID, String> playerTags = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private File playerDataFile;
    private FileConfiguration playerData;
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 45;
    private final int DEFAULT_COOLDOWN = 3;

    public TagManager() {
        try {
            // Tạo thư mục plugin nếu chưa tồn tại
            File dataFolder = Schooltag.getInstance().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            playerDataFile = new File(dataFolder, "playerdata.yml");
            
            // Nếu file chưa tồn tại, tạo mới
            if (!playerDataFile.exists()) {
                try {
                    playerDataFile.createNewFile();
                    Schooltag.getInstance().getLogger().info("Đã tạo file playerdata.yml mới!");
                } catch (IOException e) {
                    e.printStackTrace();
                    Schooltag.getInstance().getLogger().severe("Không thể tạo file playerdata.yml!");
                }
            }
            
            playerData = YamlConfiguration.loadConfiguration(playerDataFile);
            
            // Tạo cấu trúc mặc định nếu file rỗng
            if (playerData.getKeys(false).isEmpty()) {
                playerData.set("_version", "1.0");
                try {
                    playerData.save(playerDataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Schooltag.getInstance().getLogger().severe("Không thể tải file playerdata.yml!");
        }
    }

    public void loadPlayerTags() {
        if (playerData == null) return;
        
        try {
            for (String uuidStr : playerData.getKeys(false)) {
                if (uuidStr.equals("_version")) continue; // Bỏ qua key version
                
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String tag = playerData.getString(uuidStr + ".current");
                    if (tag != null && !tag.isEmpty()) {
                        playerTags.put(uuid, tag);
                    }
                } catch (IllegalArgumentException e) {
                    // Bỏ qua nếu UUID không hợp lệ
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
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                double baseHealth = 20.0;
                double newMaxHealth = baseHealth * multiplier;
                
                double currentHealth = player.getHealth();
                double currentMax = attribute.getValue();
                double healthRatio = currentHealth / currentMax;
                
                attribute.setBaseValue(newMaxHealth);
                
                double newHealth = newMaxHealth * healthRatio;
                player.setHealth(Math.min(newHealth, newMaxHealth));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetHealth(Player player) {
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
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

    private int getCooldownTime() {
        return Schooltag.getInstance().getConfigManager().getCooldown();
    }

    private boolean isOnCooldown(Player player) {
        int cooldownTime = getCooldownTime();
        if (cooldownTime <= 0) return false; // Tắt cooldown
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(uuid)) {
            long lastChange = cooldowns.get(uuid);
            long timePassed = (currentTime - lastChange) / 1000;
            
            if (timePassed < cooldownTime) {
                return true;
            }
        }
        
        return false;
    }

    private void updateCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getRemainingCooldown(Player player) {
        int cooldownTime = getCooldownTime();
        if (cooldownTime <= 0) return 0;
        
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return 0;
        
        long lastChange = cooldowns.get(uuid);
        long currentTime = System.currentTimeMillis();
        long timePassed = (currentTime - lastChange) / 1000;
        
        return Math.max(0, cooldownTime - timePassed);
    }

    public void openTagMenu(Player player) {
        openTagMenu(player, 0);
    }

    public void openTagMenu(Player player, int page) {
        try {
            this.currentPage = page;
            Inventory inv = Bukkit.createInventory(null, 54, 
                ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getGuiTitle()));
            
            ItemStack filler = new ItemStack(Material.valueOf(
                Schooltag.getInstance().getConfigManager().getFillerMaterial()));
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getFillerName()));
            filler.setItemMeta(fillerMeta);
            
            for (int i = 0; i < 54; i++) {
                inv.setItem(i, filler);
            }

            List<String> allTags = new ArrayList<>(Schooltag.getInstance().getConfigManager().getAllTags());
            Map<Integer, String> slotMap = new HashMap<>();
            
            for (String tagId : allTags) {
                int slot = Schooltag.getInstance().getConfigManager().getTagSlot(tagId);
                if (slot >= 0 && slot < 54) {
                    slotMap.put(slot, tagId);
                }
            }
            
            for (Map.Entry<Integer, String> entry : slotMap.entrySet()) {
                int slot = entry.getKey();
                String tagId = entry.getValue();
                
                boolean hasPerm = hasTagPermission(player, tagId);
                boolean isSelected = tagId.equals(getPlayerTag(player));
                
                ItemStack item = createTagItem(tagId, hasPerm, isSelected, player);
                inv.setItem(slot, item);
            }

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

            // Hiển thị cooldown
            if (isOnCooldown(player)) {
                long remaining = getRemainingCooldown(player);
                ItemStack cooldownItem = new ItemStack(Material.CLOCK);
                ItemMeta cooldownMeta = cooldownItem.getItemMeta();
                cooldownMeta.setDisplayName(ColorUtils.colorize("&c&l⏳ Đợi " + remaining + "s"));
                cooldownItem.setItemMeta(cooldownMeta);
                inv.setItem(4, cooldownItem);
            }

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
            
            double damageMult = Schooltag.getInstance().getConfigManager().getTagDamageMultiplier(tagId);
            double healthMult = Schooltag.getInstance().getConfigManager().getTagHealthMultiplier(tagId);
            
            if (damageMult > 1.0) {
                lore.add(ColorUtils.colorize("&c⚔️ +" + (int)((damageMult - 1) * 100) + "% sát thương"));
            }
            if (healthMult > 1.0) {
                lore.add(ColorUtils.colorize("&a❤️ +" + (int)((healthMult - 1) * 100) + "% máu"));
            }
            
            List<String> grantPerms = Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
            String grantPermsStr = grantPerms.isEmpty() ? "Không" : String.join(", ", grantPerms);
            
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
            
            if (isOnCooldown(player) && hasPerm && !selected) {
                lore.add("");
                lore.add(ColorUtils.colorize("&c⏳ Chờ " + getRemainingCooldown(player) + "s"));
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
            if (isOnCooldown(player)) {
                long remaining = getRemainingCooldown(player);
                String msg = Schooltag.getInstance().getConfigManager().getMessage("cooldown")
                    .replace("{seconds}", String.valueOf(remaining));
                player.sendMessage(ColorUtils.colorize(msg));
                openTagMenu(player, currentPage);
                return;
            }
            
            if (!hasTagPermission(player, tagId)) {
                player.sendMessage(ColorUtils.colorize(
                    Schooltag.getInstance().getConfigManager().getMessage("tag-locked")));
                return;
            }
            
            String currentTag = getPlayerTag(player);
            if (currentTag != null && currentTag.equals(tagId)) {
                player.sendMessage(ColorUtils.colorize("&eBạn đang dùng danh hiệu này!"));
                return;
            }
            
            if (!Schooltag.getInstance().getConfigManager().tagExists(tagId)) {
                player.sendMessage(ColorUtils.colorize(
                    Schooltag.getInstance().getConfigManager().getMessage("tag-not-exist")));
                return;
            }
            
            if (currentTag != null) {
                removeTagPermissions(player);
                resetPlayerStats(player);
            }
            
            playerTags.put(player.getUniqueId(), tagId);
            applyTagPermissions(player);
            applyTagEffects(player);
            updateCooldown(player);
            savePlayerTag(player);
            
            String msg = Schooltag.getInstance().getConfigManager().getMessage("tag-selected")
                .replace("{tag}", Schooltag.getInstance().getConfigManager().getTagName(tagId));
            player.sendMessage(ColorUtils.colorize(msg));
            
            double damageMult = Schooltag.getInstance().getConfigManager().getTagDamageMultiplier(tagId);
            double healthMult = Schooltag.getInstance().getConfigManager().getTagHealthMultiplier(tagId);
            
            List<String> effects = new ArrayList<>();
            if (damageMult > 1.0) {
                effects.add(ColorUtils.colorize("&c⚔️ +" + (int)((damageMult - 1) * 100) + "%"));
            }
            if (healthMult > 1.0) {
                effects.add(ColorUtils.colorize("&a❤️ +" + (int)((healthMult - 1) * 100) + "%"));
            }
            
            if (!effects.isEmpty()) {
                player.sendMessage(ColorUtils.colorize("&7Hiệu ứng: " + String.join(", ", effects)));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra!"));
        }
    }

    public void removeTag(Player player) {
        try {
            if (isOnCooldown(player)) {
                long remaining = getRemainingCooldown(player);
                String msg = Schooltag.getInstance().getConfigManager().getMessage("cooldown")
                    .replace("{seconds}", String.valueOf(remaining));
                player.sendMessage(ColorUtils.colorize(msg));
                return;
            }
            
            UUID uuid = player.getUniqueId();
            removeTagPermissions(player);
            resetPlayerStats(player);
            playerTags.remove(uuid);
            savePlayerTag(player);
            updateCooldown(player);
            
            if (hasTagPermission(player, "default")) {
                playerTags.put(uuid, "default");
                applyTagPermissions(player);
                applyTagEffects(player);
                savePlayerTag(player);
            }
            
            player.sendMessage(ColorUtils.colorize(
                Schooltag.getInstance().getConfigManager().getMessage("tag-removed")));
            
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra!"));
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
                Bukkit.getScheduler().runTaskLater(Schooltag.getInstance(), () -> {
                    openTagMenu(player, currentPage);
                }, 1L);
            } else if (slot == 48) {
                // Previous
            } else if (slot == 50) {
                // Next
            } else if (slot == 49) {
                player.closeInventory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetHealth(player);
            removeTagPermissions(player);
        }
    }

    public void resetCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void resetAllCooldowns() {
        cooldowns.clear();
    }
}