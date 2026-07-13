package com.schooltag.listeners;

import com.schooltag.Schooltag;
import com.schooltag.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getGuiTitle());
        if (!event.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        if (slot >= 0 && slot < 54) {
            Schooltag.getInstance().getTagManager().handleMenuClick(player, slot);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getGuiTitle());
        if (!event.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
    }
}
