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

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runnable;

import com.google.common.collect.ImmutableList;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.tasks.TeleportToCourseTask;
import me.cmastudios.mcparkour.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class FavoritesList {

    private Player player;
    private List<Integer> favorites;
    private Favorites plugin;
    private int page = 1;
    private OpenFavsTask favTask;

    public static FavoritesList loadFavoritesList(Player player, Favorites plugin) throws SQLException {
        ArrayList<Integer> favs = new ArrayList<>();
        try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("SELECT * FROM favorites WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    for (String s : result.getString("favorites").split(",")) {
                        try {
                            favs.add(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            Bukkit.getLogger().log(Level.WARNING, Parkour.getString("error.invalidint"));
                        }
                    }
                }
            }
        }
        Collections.sort(favs);
        return new FavoritesList(player, plugin, favs);
    }

    public FavoritesList(Player player, Favorites plugin, List<Integer> favorites) throws SQLException {
        this.player = player;
        this.plugin = plugin;
        this.favorites = favorites;
    }

    public void openMenu() {
        if (favorites.isEmpty()) {
            player.sendMessage(Favorites.getString("favorites.empty"));
            return;
        }
        favTask = new OpenFavsTask(ImmutableList.copyOf(favorites), plugin, page, player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, favTask);
    }

    public Inventory getInventory(){
        return favTask.getInv();
    }

    public void handleSelection(int page, int slot, ClickType click, Inventory inv) {
        int pos = (page - 1) * 45 + slot;
        if(favorites.size() < pos) return;
        if (click.isLeftClick()) {
            if (slot == 45 && inv.getItem(45) != null) {
                this.page--;
                player.closeInventory();
                openMenu();
                return;
            } else if (slot == 53 && inv.getItem(53) != null) {
                this.page++;
                player.closeInventory();
                openMenu();
                return;
            }
            if (favorites.get(pos) != null) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new TeleportToCourseTask(plugin.getParkour(), player, PlayerTeleportEvent.TeleportCause.PLUGIN, favorites.get(pos)));
                player.closeInventory();
            }
        } else if (click.isShiftClick() && click.isRightClick()) {
            Integer pkID = favorites.get(pos);
            if (pkID != null) {
                favorites.remove(pos);
                inv.setItem(slot, null);
                if (favorites.size() == 0) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, getSaveTask());
                    //getSaveTask().runTaskAsynchronously(plugin);
                }
            }
        }
    }

    public void addParkour(int i) {
        if (favorites.contains(i)) {
            player.sendMessage(Favorites.getString("favorites.alreadyadded"));
            return;
        }
        FavoritesAddParkourEvent event = new FavoritesAddParkourEvent(this, i, player);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            player.sendMessage(Favorites.getString("favorites.added"));
            favorites.add(i);
        }
        Collections.sort(favorites);
    }

    private SaveFavsTask getSaveTask() {
        return new SaveFavsTask(ImmutableList.copyOf(favorites), plugin.getCourseDatabase(), player);
    }

    public void save(boolean async) {
        if (favorites.size() == 0) {
            return;
        }
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, getSaveTask());
            //getSaveTask().runTaskAsynchronously(plugin);
        } else {
            getSaveTask().run();
        }
    }

    public int getCurrentPage() {
        return page;
    }

    public int size() {
        return favorites.size();
    }
}

class SaveFavsTask implements Runnable {
    private final ImmutableList<Integer> favorites;
    private final Player player;
    private final Connection conn;

    public SaveFavsTask(ImmutableList<Integer> favs, Connection conn, Player player) {
        this.favorites = favs;
        this.player = player;
        this.conn = conn;
    }

    @Override
    public void run() {
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO favorites (`player`,`favorites`) VALUES (?,?) ON DUPLICATE KEY UPDATE favorites = ?")) {
            StringBuilder favs = new StringBuilder();
            for (int i : favorites) {
                favs.append(i).append(",");
            }
            statement.setString(1, player.getName());
            statement.setString(2, favs.toString());
            statement.setString(3, favs.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(FavoritesList.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class OpenFavsTask implements Runnable {
    private final ImmutableList<Integer> favs;
    private final int page;
    private final Player player;
    private final Favorites plugin;
    private Inventory inv = null;


    public Inventory getInv(){
        return this.inv;
    }

    public OpenFavsTask(ImmutableList<Integer> parkours, Favorites plugin, int page, Player player) {
        this.favs = parkours;
        this.plugin = plugin;
        this.page = page;
        this.player = player;
    }

    @Override
    public void run() {
        inv = getInventory(page);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.openInventory(inv);
            }
        });
    }

    public Inventory getInventory(int page) {
        inv = Bukkit.createInventory(null, 54, Favorites.getString("favorites.inventory.name"));
        if (favs.size() > 45 * page) {
            ItemStack next = Item.NEXT_PAGE.getItem();
            next.setAmount(page + 1);
            inv.setItem(53, next);
        }
        if (page > 1) {
            ItemStack prev = Item.PREV_PAGE.getItem();
            prev.setAmount(page - 1);
            inv.setItem(45, prev);
        }

        int target = favs.size();
        if (favs.size() > 45) {
            target = 45;
        }
        StringBuilder builder = new StringBuilder();
        for (Integer num : favs) {
            builder.append(num).append(",");
        }
        builder.deleteCharAt(builder.lastIndexOf(","));
        try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("SELECT * FROM courses WHERE id IN (" + builder.toString() + ") LIMIT ?,?")) {
            stmt.setInt(1, (page - 1) * 45);
            stmt.setInt(2, target);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemStack item;
                    ItemMeta meta;
                    try {
                        switch (ParkourCourse.CourseMode.valueOf(rs.getString("mode").toUpperCase())) {
                            case NORMAL:
                                item = Item.valueOf(rs.getString("difficulty").toUpperCase()).getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("item.icons." + rs.getString("difficulty").toLowerCase(), rs.getString("name"), rs.getInt("id")));
                                item.setItemMeta(meta);
                                break;
                            default:
                                item = Item.valueOf(rs.getString("mode").toUpperCase()).getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("item.icons." + rs.getString("mode").toLowerCase(), rs.getString("name"), rs.getInt("id")));
                                item.setItemMeta(meta);
                                break;

                        }
                    } catch (IllegalArgumentException e) {
                        inv.addItem(Item.EASY.getItem());
                        continue;
                    }
                    if (item != null) {
                        meta = item.getItemMeta();
                        String[] lore = {
                                Favorites.getString("favorites.item.diffs.lore.0"),
                                Favorites.getString("favorites.item.diffs.lore.1", rs.getInt("id")),
                                Favorites.getString("favorites.item.diffs.lore.2")};
                        meta.setLore(Arrays.asList(lore));
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(FavoritesList.class.getName()).log(Level.SEVERE, null, ex);
        }


        return inv;
    }

}
