package com.schooltag.placeholder;

import com.schooltag.Schooltag;
import org.bukkit.entity.Player;

public class SchooltagExpansion {
    
    public static String parsePlaceholders(Player player, String identifier) {
        if (player == null) return "";
        
        // %schooltag_name% hoặc %schooltag%
        if (identifier.equals("name") || identifier.equals("")) {
            String tag = Schooltag.getInstance().getTagManager().getPlayerTag(player);
            if (tag == null) return "";
            return Schooltag.getInstance().getConfigManager().getTagName(tag);
        }
        
        // %schooltag_raw%
        if (identifier.equals("raw")) {
            String tag = Schooltag.getInstance().getTagManager().getPlayerTag(player);
            return tag == null ? "" : tag;
        }
        
        // %schooltag_permissions%
        if (identifier.equals("permissions") || identifier.equals("perms")) {
            String tag = Schooltag.getInstance().getTagManager().getPlayerTag(player);
            if (tag == null) return "";
            return String.join(", ", 
                Schooltag.getInstance().getConfigManager().getTagGrantPermissions(tag));
        }
        
        // %schooltag_has_<tagid>% - Kiểm tra có danh hiệu không
        if (identifier.startsWith("has_")) {
            String tagId = identifier.substring(4);
            return String.valueOf(Schooltag.getInstance().getTagManager().hasTagPermission(player, tagId));
        }
        
        // %schooltag_damage% - % tăng sát thương
        if (identifier.equals("damage")) {
            String tag = Schooltag.getInstance().getTagManager().getPlayerTag(player);
            if (tag == null) return "0";
            double multiplier = Schooltag.getInstance().getConfigManager().getTagDamageMultiplier(tag);
            return String.valueOf((multiplier - 1) * 100);
        }
        
        // %schooltag_health% - % tăng máu
        if (identifier.equals("health")) {
            String tag = Schooltag.getInstance().getTagManager().getPlayerTag(player);
            if (tag == null) return "0";
            double multiplier = Schooltag.getInstance().getConfigManager().getTagHealthMultiplier(tag);
            return String.valueOf((multiplier - 1) * 100);
        }
        
        // %schooltag_count% - Số danh hiệu đã mở
        if (identifier.equals("count")) {
            int count = 0;
            for (String tagId : Schooltag.getInstance().getConfigManager().getAllTags()) {
                if (Schooltag.getInstance().getTagManager().hasTagPermission(player, tagId)) {
                    count++;
                }
            }
            return String.valueOf(count);
        }
        
        // %schooltag_total% - Tổng số danh hiệu
        if (identifier.equals("total")) {
            return String.valueOf(Schooltag.getInstance().getConfigManager().getAllTags().size());
        }
        
        return null;
    }
}