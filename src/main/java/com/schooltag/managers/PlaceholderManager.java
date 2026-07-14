package com.schooltag.managers;

import com.schooltag.Schooltag;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager {
    
    public String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        if (tagId == null) {
            return text.replace("%schooltag%", "")
                      .replace("%tag%", "")
                      .replace("%schooltag_permissions%", "");
        }
        
        String tagName = Schooltag.getInstance().getConfigManager().getTagName(tagId);
        if (tagName == null) tagName = "";
        
        List<String> grantPerms = Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
        String permsStr = grantPerms.isEmpty() ? "Không có" : String.join(", ", grantPerms);
        
        return text.replace("%schooltag%", tagName)
                   .replace("%tag%", tagName)
                   .replace("%schooltag_permissions%", permsStr);
    }
    
    public String getPlayerTagDisplay(Player player) {
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        if (tagId == null) return "";
        return Schooltag.getInstance().getConfigManager().getTagName(tagId);
    }
    
    public List<String> getPlayerTagPermissions(Player player) {
        String tagId = Schooltag.getInstance().getTagManager().getPlayerTag(player);
        if (tagId == null) return new ArrayList<>();
        return Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tagId);
    }
}