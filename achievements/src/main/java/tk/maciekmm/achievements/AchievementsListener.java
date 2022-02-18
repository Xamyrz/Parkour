/*
 * Copyright (C) 2014 Maciej Mionskowski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tk.maciekmm.achievements;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tk.maciekmm.achievements.data.PlayerAchievements;

import java.util.Map;

public class AchievementsListener implements Listener {
    private final Achievements plugin;
    private Inventory achievementsInv;

    public AchievementsListener(Achievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        parent:
        for (Item item : Item.getItemsByType(Item.ItemType.SPAWN)) {
            if (!event.getPlayer().getInventory().contains(item.getItem().getType())) {
                if (event.getPlayer().getInventory().contains(item.getItem().getType())) {
                    for (Map.Entry<Integer, ? extends ItemStack> entry : event.getPlayer().getInventory().all(item.getItem().getType()).entrySet()) {
                        if (item.isSimilar(entry.getValue())) {
                            continue parent;
                        }
                    }
                }
                event.getPlayer().getInventory().addItem(item.getItem());
            }
        }
        //if (event.getPlayer().hasMetadata("achievements")) {
        //    event.getPlayer().removeMetadata("achievement", plugin);
        //}
        //event.getPlayer().setMetadata("achievements", new FixedMetadataValue(plugin, PlayerAchievements.loadPlayerAchievements(event.getPlayer(), plugin)));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!event.getInventory().equals(achievementsInv)){
            return;
        }
        event.setCancelled(true);
        PlayerAchievements achs = this.plugin.getManager().getPlayerAchievements((Player) event.getWhoClicked());
        if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
            achs.handleSelection(achs.getPage(), event.getSlot(), event.getClick(), event.getInventory());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getManager().getPlayerAchievements(event.getPlayer()).save(true);
        plugin.getManager().removePlayerAchievementsCache(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (Item.ACHIEVEMENTS_MENU.isSimilar(event.getItem())) {
            event.setCancelled(true);
            if(plugin.canUse(event.getPlayer(),"menu",1)) {
                plugin.getManager().getPlayerAchievements(event.getPlayer()).openMenu();
                achievementsInv = plugin.getManager().getPlayerAchievements(event.getPlayer()).achievementsInv;
            } else {
                event.getPlayer().sendMessage(Achievements.getString("achievement.inventory.cooldown"));
            }

        }
    }
}