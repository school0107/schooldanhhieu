package com.schooltag.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern RGB_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public static String colorize(String message) {
        if (message == null) return "";
        
        Matcher matcher = RGB_PATTERN.matcher(message);
        while (matcher.find()) {
            String color = matcher.group(1);
            message = message.replace("&#" + color, ChatColor.of("#" + color).toString());
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static List<String> colorizeList(List<String> messages) {
        List<String> colored = new ArrayList<>();
        for (String msg : messages) {
            colored.add(colorize(msg));
        }
        return colored;
    }
}