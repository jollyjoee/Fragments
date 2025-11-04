package com.jolly.fragments.gui;

import com.jolly.fragments.Fragments;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class PointsShopGUI implements Listener {
    private final Fragments plugin;

    public PointsShopGUI(Fragments plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        // Load config
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("gui");
        if (guiSection == null) {
            player.sendMessage("§cGUI section missing in config.yml!");
            return;
        }

        // Build inventory
        String guiTitle = guiSection.getString("name", "<blue><b>PointsShop");
        int rows = guiSection.getInt("rows", 1);
        int guiSize = Math.min(rows * 9, 54); // max 6 rows (54 slots)
        Inventory gui = Bukkit.createInventory(null, guiSize, plugin.mm(guiTitle));

        // Load items
        ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
                if (itemSec == null) continue;

                int slot = itemSec.getInt("slot");
                String name = itemSec.getString("name", key);
                String matName = itemSec.getString("material", "STONE");
                boolean enchantGlint = itemSec.getBoolean("enchant-glint", false);
                boolean hideEnchants = itemSec.getBoolean("hide-enchants", false);
                List<String> enchantments = new ArrayList<>();
                enchantments.addAll(itemSec.getStringList("enchantments"));
                Material material = Material.matchMaterial(matName.toUpperCase());
                if (material == null) material = Material.BARRIER;

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(plugin.mm(name));

                    if (enchantments != null && !enchantments.isEmpty()) {
                        for (String enchantment : enchantments) {
                            try {
                                String[] split = enchantment.split(":");
                                String enchantName = split[0].toUpperCase();
                                int level = (split.length > 1) ? Integer.parseInt(split[1]) : 1;

                                Enchantment ench = Enchantment.getByName(enchantName);
                                if (ench != null) {
                                    meta.addEnchant(ench, level, true);
                                } else {
                                    plugin.getLogger().warning("Invalid enchantment name in config: " + enchantName);
                                }
                            } catch (Exception ex) {
                                plugin.getLogger().warning("Failed to parse enchantment: " + enchantment);
                            }
                        }
                        if (hideEnchants) {
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }
                    }
                    // Optional glint (fake enchantment)
                    if (enchantGlint) {
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    item.setItemMeta(meta);
                }

                // Add to GUI
                if (slot < guiSize) {
                    gui.setItem(slot, item);
                }
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String guiTitle = plugin.getConfig().getString("gui.name", "<blue><b>PointsShop");
        if (!e.getView().title().equals(plugin.mm(guiTitle))) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
            if (itemSec == null) continue;

            int slot = itemSec.getInt("slot", -1);
            if (slot != e.getSlot()) continue; // ✅ Slot-specific check

            Material mat = Material.matchMaterial(itemSec.getString("material", "RED_CONCRETE"));
            if (mat == null || clicked.getType() != mat) continue;

            String name = itemSec.getString("name");

            int price = itemSec.getInt("price", 0);
            if (plugin.getPoints(player) < price) {
                player.sendMessage(Component.text("You don’t have enough points!"));
                return;
            }

            plugin.removePoints(player, price);

            for (String commandKey : itemSec.getStringList("commands")) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        commandKey.replace("%player%", player.getName())
                );
            }
            player.sendActionBar(plugin.mm(
                    "<b><aqua>You paid </aqua></b><gold>" + price + "</gold> <aqua>"
                            + plugin.getConfig().getString("currencyName")
                            + "/s</aqua><b><aqua> for </aqua></b><gold>" + name + "</gold>"
            ));
            break;
        }
    }

}
