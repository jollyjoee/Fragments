package com.jolly.fragments;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Fragments extends JavaPlugin implements Listener {
    private PlayerPointsAPI ppAPI;
    private int ppm;
    private int rate;
    private String currencyName;
    private final Map<UUID, ScheduledTask> rewardTasks = new HashMap<>();
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        ppm = config.getInt("ppm");
        rate = config.getInt("rate");
        currencyName = config.getString("currencyName");
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
                this.ppAPI = PlayerPoints.getInstance().getAPI();
            }
            if (this.ppAPI != null) {
                this.getLogger().info("Fragments have been enabled!");
            }
        } catch (Exception e) {
            getLogger().severe("❌ Failed to initialize PlayerPointsAPI: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
        getLogger().info("Loaded config: ppm=" + ppm + ", rate=" + rate + ", currencyName=" + currencyName);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Fragments plugin...");
        for (ScheduledTask task : rewardTasks.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        rewardTasks.clear();
        getLogger().info("All reward timers stopped successfully!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID UUID = player.getUniqueId();
        if (rewardTasks.containsKey(UUID)) {
            rewardTasks.get(UUID).cancel();
        }
        getLogger().info(ChatColor.GOLD + player.getName() + " joined the server! They wil now start receiving " + currencyName + "s.");
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> {
            if (!player.isOnline()) {
                t.cancel();
                rewardTasks.remove(UUID);
                return;
            }
            ppAPI.give(UUID, ppm);
            player.sendActionBar(mm("<gold>You have received </gold><bold><aqua>" + ppm + "</aqua></bold> <gold>" + currencyName + "/s</gold>"));
        }, 1L, (20L * 60L) * rate);
        rewardTasks.put(UUID, task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ScheduledTask task = rewardTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private Component mm(String message) {
        return MiniMessage.miniMessage().deserialize(
                ChatColor.translateAlternateColorCodes('&', message != null ? message : "")
        );
    }
}
