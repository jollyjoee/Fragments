package com.jolly.fragments.managers;

import com.jolly.fragments.Fragments;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardManager implements Listener {
    private final Fragments plugin;
    private final Map<UUID, ScheduledTask> rewardTasks = new HashMap<>();

    private final int ppm;
    private final int rate;
    private final String currencyName;

    public RewardManager(Fragments plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();
        this.ppm = config.getInt("reward");
        this.rate = config.getInt("rate");
        this.currencyName = config.getString("currencyName", "points");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // cancel old task if any
        rewardTasks.computeIfPresent(uuid, (k, task) -> {
            task.cancel();
            return null;
        });

        plugin.getLogger().info(ChatColor.GOLD + player.getName() + " joined, starting reward task!");

        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            if (!player.isOnline()) {
                t.cancel();
                rewardTasks.remove(uuid);
                return;
            }

            plugin.getPointsAPI().give(uuid, ppm);
            player.sendActionBar(plugin.mm("<gold>+<aqua>" + ppm + "</aqua> " + currencyName + "</gold>"));
        }, (20L * 60L) * rate, (20L * 60L) * rate);

        rewardTasks.put(uuid, task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ScheduledTask task = rewardTasks.remove(uuid);
        if (task != null) task.cancel();
        plugin.getLogger().info(event.getPlayer().getName() + " left, reward task stopped.");
    }

    public void shutdown() {
        for (ScheduledTask task : rewardTasks.values()) task.cancel();
        rewardTasks.clear();
    }
}
