package com.schooltag.commands;

import com.schooltag.Schooltag;
import com.schooltag.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TagCommand implements CommandExecutor {
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

        if (args.length > 0) {
            // Xử lý subcommands
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("reload") && player.hasPermission("schooltag.admin")) {
                Schooltag.getInstance().reload();
                player.sendMessage(ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getMessage("reloaded")));
                return true;
            }
            
            if (subCommand.equals("remove")) {
                Schooltag.getInstance().getTagManager().removeTag(player);
                player.sendMessage(ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getMessage("tag-removed")));
                return true;
            }
        }

        // Mở menu
        Schooltag.getInstance().getTagManager().openTagMenu(player);
        return true;
    }
}