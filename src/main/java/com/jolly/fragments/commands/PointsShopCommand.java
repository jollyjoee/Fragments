package com.jolly.fragments.commands;

import com.jolly.fragments.Fragments;
import com.jolly.fragments.gui.PointsShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PointsShopCommand implements CommandExecutor {
    private final Fragments plugin;
    private final PointsShopGUI shopGUI;

    public PointsShopCommand(Fragments plugin, PointsShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        shopGUI.open(player);
        return true;
    }
}
