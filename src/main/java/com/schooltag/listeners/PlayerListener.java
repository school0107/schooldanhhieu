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

public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TagManager tagManager = Schooltag.getInstance().getTagManager();
        
        // Load và áp dụng danh hiệu
        tagManager.loadPlayerTag(player);
        tagManager.applyTagEffects(player);
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
            // Giảm sát thương nhận vào
            event.setDamage(event.getDamage() * (1 / healthMultiplier));
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
}