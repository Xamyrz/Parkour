/*
 * Copyright (C) 2013 Maciej Mionskowski
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

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tk.maciekmm.achievements.data.PlayerAchievements;

import java.util.Map;

public class AchievementsListener implements Listener {
    private final Achievements plugin;

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
        if (event.getInventory().getName().equalsIgnoreCase(Achievements.getString("achievement.inventory.name"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().hasMetadata("achievements")) {
            MetadataValue val = event.getPlayer().getMetadata("achievements").get(0);
            if (val != null) {
                if (val.value() instanceof PlayerAchievements) {
                    PlayerAchievements achievements = (PlayerAchievements) val.value();
                    achievements.save();
                    event.getPlayer().removeMetadata("achievements", plugin);
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (Item.ACHIEVEMENTS_MENU.isSimilar(event.getItem())) {
            event.setCancelled(true);
            plugin.getManager().getPlayerAchievements(event.getPlayer()).openMenu();
        }
    }
}
