package com.schooltag.placeholder;

import com.schooltag.Schooltag;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    @Override
    public @NotNull String getIdentifier() {
        return "schooltag";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SchoolTagDev";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        return SchooltagExpansion.parsePlaceholders(player, identifier);
    }
}