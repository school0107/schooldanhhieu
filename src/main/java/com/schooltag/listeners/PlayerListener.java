package com.schooltag.listeners;

import com.schooltag.Schooltag;
import com.schooltag.managers.TagManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TagManager tagManager = Schooltag.getInstance().getTagManager();
        
        // Load và áp dụng danh hiệu
        tagManager.loadPlayerTag(player);
        tagManager.applyTagEffects(player);
        
        // Áp dụng hiệu ứng tăng máu ngay khi join
        applyHealthBoost(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Schooltag.getInstance().getTagManager().savePlayerTag(player);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        
        if (tagId == null) return;
        
        double healthMultiplier = Schooltag.getInstance().getConfigManager()
            .getTagHealthMultiplier(tagId);
        
        if (healthMultiplier > 1.0) {
            // Giảm sát thương nhận vào dựa trên % tăng máu
            double damageReduction = 1.0 - (1.0 / healthMultiplier);
            event.setDamage(event.getDamage() * (1.0 - damageReduction));
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        
        if (tagId == null) return;
        
        double damageMultiplier = Schooltag.getInstance().getConfigManager()
            .getTagDamageMultiplier(tagId);
        
        if (damageMultiplier > 1.0) {
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }
    
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        
        if (tagId == null) return;
        
        double healthMultiplier = Schooltag.getInstance().getConfigManager()
            .getTagHealthMultiplier(tagId);
        
        if (healthMultiplier > 1.0) {
            // Tăng lượng máu hồi phục
            event.setAmount(event.getAmount() * healthMultiplier);
        }
    }
    
    /**
     * Áp dụng tăng máu tối đa cho người chơi
     */
    public void applyHealthBoost(Player player) {
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        if (tagId == null) return;
        
        double healthMultiplier = Schooltag.getInstance().getConfigManager()
            .getTagHealthMultiplier(tagId);
        
        if (healthMultiplier > 1.0) {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                double baseHealth = 20.0;
                double newMaxHealth = baseHealth * healthMultiplier;
                attribute.setBaseValue(newMaxHealth);
                
                // Hồi phục máu theo tỷ lệ
                double currentHealth = player.getHealth();
                double healthRatio = currentHealth / 20.0;
                player.setHealth(Math.min(newMaxHealth * healthRatio, newMaxHealth));
            }
        } else {
            resetHealth(player);
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
}