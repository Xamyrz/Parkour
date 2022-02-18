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

package tk.maciekmm.favorites;

import me.cmastudios.mcparkour.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Map;

public class FavoritesListener implements Listener {
    private final Favorites plugin;

    public FavoritesListener(Favorites favs) {
        this.plugin = favs;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) throws SQLException {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.hasBlock() && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.GOLD_BLOCK) {
                Block op = event.getClickedBlock().getRelative(event.getBlockFace().getOppositeFace(), 1);
                if (op.getType() == Material.OAK_WALL_SIGN || op.getType() == Material.OAK_SIGN) {
                    Sign favsign = (Sign) op.getState();
                    try {
                        int parkID = Integer.parseInt(favsign.getLine(1));
                        FavoritesList favs;
                        if (plugin.pendingFavs.containsKey(event.getPlayer())) {
                            favs = plugin.pendingFavs.get(event.getPlayer());
                        } else {
                            favs = FavoritesList.loadFavoritesList(event.getPlayer(), plugin);
                            plugin.pendingFavs.put(event.getPlayer(), favs);
                        }
                        favs.addParkour(parkID);

                        event.setCancelled(true);
                    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException ignored) {
                    }
                    return;
                }

            }

        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!event.hasItem()) {
                return;
            }
            if (Item.FAVORITES.isSimilar(event.getItem())) {
                event.setCancelled(true);

                if(!Utils.canUse(plugin,event.getPlayer(),"favorites",2)) {
                    event.getPlayer().sendMessage(Favorites.getString("favorites.delay"));
                    return;
                }
                FavoritesList favs;
                if (plugin.pendingFavs.containsKey(event.getPlayer())) {
                    favs = plugin.pendingFavs.get(event.getPlayer());
                } else {
                    favs = FavoritesList.loadFavoritesList(event.getPlayer(), plugin);
                    plugin.pendingFavs.put(event.getPlayer(), favs);
                }
                favs.openMenu();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
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
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.pendingFavs.containsKey(event.getPlayer())) {
            plugin.pendingFavs.remove(event.getPlayer()).save(true);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) throws SQLException {
        if(plugin.pendingFavs.get(event.getWhoClicked()) == null || plugin.pendingFavs.get(event.getWhoClicked()).getInventory() == null){
            return;
        }
        if(!event.getInventory().equals(plugin.pendingFavs.get(event.getWhoClicked()).getInventory())){
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }
        event.setCancelled(true);
        FavoritesList favs;
        if (plugin.pendingFavs.containsKey(event.getWhoClicked())) {
            favs = plugin.pendingFavs.get(event.getWhoClicked());
        } else {
            favs = FavoritesList.loadFavoritesList((Player) event.getWhoClicked(), plugin);
        }
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            favs.handleSelection(favs.getCurrentPage(), event.getSlot(), event.getClick(), event.getInventory());
        }
    }
}
