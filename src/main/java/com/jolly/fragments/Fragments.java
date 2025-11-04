package com.jolly.fragments;

import com.jolly.fragments.commands.PointsShopCommand;
import com.jolly.fragments.gui.PointsShopGUI;
import com.jolly.fragments.managers.RewardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;

import java.util.UUID;

public class Fragments extends JavaPlugin {

    private PlayerPointsAPI ppAPI;
    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // connect to PlayerPoints
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            getLogger().severe("❌ PlayerPoints not found! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.ppAPI = PlayerPoints.getInstance().getAPI();

        // create and register reward manager
        rewardManager = new RewardManager(this);
        Bukkit.getPluginManager().registerEvents(rewardManager, this);

        if (config.getBoolean("shop-enabled")) {
            PointsShopGUI shopGUI = new PointsShopGUI(this);
            Bukkit.getPluginManager().registerEvents(shopGUI, this);
            getCommand("pointsshop").setExecutor(new PointsShopCommand(this, shopGUI));
        }

        getLogger().info("✅ Fragments enabled and ready!");
    }

    @Override
    public void onDisable() {
        if (rewardManager != null) rewardManager.shutdown();
    }

    public PlayerPointsAPI getPointsAPI() {
        return ppAPI;
    }

    public String getCurrencyName() {
        return getConfig().getString("currencyName", "point");
    }

    public Component mm(String message) {
        return MiniMessage.miniMessage().deserialize(ChatColor.translateAlternateColorCodes('&', message));
    }

    public String mmPlain(String message) {
        Component component = MiniMessage.miniMessage().deserialize(ChatColor.translateAlternateColorCodes('&', message));
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public void addPoints(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        this.getPointsAPI().give(uuid, amount);
    }

    public void removePoints(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        this.getPointsAPI().take(uuid, amount);
    }

    public int getPoints(Player player) {
        UUID uuid = player.getUniqueId();
        return this.getPointsAPI().look(uuid);
    }
}
