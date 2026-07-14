package com.schooltag.commands;

import com.schooltag.Schooltag;
import com.schooltag.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DanhHieuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getMessage("player-only")));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("schooltag.use")) {
            player.sendMessage(ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getMessage("no-permission")));
            return true;
        }

        // Mở menu
        Schooltag.getInstance().getTagManager().openTagMenu(player);
        return true;
    }
}