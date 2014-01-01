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
package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.cmastudios.mcparkour.Item;
import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FavoritesList implements ItemMenu {

    private Player player;
    private List<Integer> favorites = new ArrayList<>();
    private Connection conn;
    private Parkour plugin;
    private int page = 1;

    public FavoritesList(Player player, Parkour plugin) throws SQLException {
        this.player = player;
        this.conn = plugin.getCourseDatabase();
        this.plugin = plugin;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM favorites WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    for (String s : result.getString("favorites").split(",")) {
                        try {
                            favorites.add(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            Bukkit.getLogger().log(Level.WARNING,Parkour.getString("error.invalidint"));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void openMenu() {
        openMenu(page);
    }

    public void openMenu(int page) {
        if (favorites.isEmpty()) {
            player.sendMessage(Parkour.getString("favorites.empty"));
            return;
        }
        Inventory inv = Bukkit.createInventory(player, 54, Parkour.getString("favorites.inventory.name"));
        Collections.sort(favorites);
        if (favorites.size() > 45*page) {
            ItemStack next = Item.NEXT_PAGE.getItem();
            next.setAmount(page + 1);
            inv.setItem(53, next);
        }
        if (page > 1) {
            ItemStack prev = Item.PREV_PAGE.getItem();
            prev.setAmount(page - 1);
            inv.setItem(45, prev);
        }

        int target = favorites.size();
        if (favorites.size() > 45) {
            target = 45;
        }
        for (int i = 0; i < target; i++) {
            try {
                ItemStack item = null;
                ItemMeta meta;
                if(favorites.size()<45 * (page - 1) + i + 1) {
                    break;
                }
                int courseId = favorites.get(45 * (page - 1) + i);
                ParkourCourse current = ParkourCourse.loadCourse(conn, courseId);

                switch (current.getMode()) {
                    case NORMAL:
                        switch (current.getDifficulty()) {
                            case EASY:
                                item = Item.EASY.getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("favorites.item.easy", current.getId()));
                                item.setItemMeta(meta);
                                break;
                            case MEDIUM:
                                item = Item.MEDIUM.getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("favorites.item.medium", current.getId()));
                                item.setItemMeta(meta);
                                break;
                            case HARD:
                                item = Item.HARD.getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("favorites.item.hard", current.getId()));
                                item.setItemMeta(meta);
                                break;
                            case VERYHARD:
                                item = Item.V_HARD.getItem();
                                meta = item.getItemMeta();
                                meta.setDisplayName(Parkour.getString("favorites.item.vhard", current.getId()));
                                item.setItemMeta(meta);
                                break;
                        }
                        break;
                    case HIDDEN:
                        item = Item.HIDDEN.getItem();
                        meta = item.getItemMeta();
                        meta.setDisplayName(Parkour.getString("favorites.item.hidden", current.getId()));
                        item.setItemMeta(meta);
                        break;
                    case ADVENTURE:
                        item = Item.ADVENTURE.getItem();
                        meta = item.getItemMeta();
                        meta.setDisplayName(Parkour.getString("favorites.item.adventure", current.getId()));
                        item.setItemMeta(meta);
                        break;
                    case VIP:
                        item = Item.THEMATIC.getItem();
                        meta = item.getItemMeta();
                        meta.setDisplayName(Parkour.getString("favorites.item.thematic", current.getId()));
                        item.setItemMeta(meta);
                        break;

                }
                if (item != null) {
                    meta = item.getItemMeta();
                    String[] lore = {
                        Parkour.getString("favorites.item.diffs.lore0"),
                        Parkour.getString("favorites.item.diffs.lore1", current.getId()),
                        Parkour.getString("favorites.item.diffs.lore2")};
                    meta.setLore(Arrays.asList(lore));
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            } catch (SQLException ex) {
                Logger.getLogger(FavoritesList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        player.openInventory(inv);
    }

    public void handleSelection(int page, int slot, ClickType click, Inventory inv) {
        int pos = (page-1) * 45 + slot;
        if (click.isLeftClick()) {
            if (slot == 45 && inv.getItem(45) != null) {
                this.page--;
                destroyMenu();
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        openMenu();
                    }
                }, 2L);
                return;
            } else if (slot == 53 && inv.getItem(53) != null) {
                this.page++;
                destroyMenu();
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        openMenu();
                    }
                }, 2L);
                return;
            }
            if (favorites.get(pos) != null) {
                plugin.teleportToCourse(player, favorites.get(pos), PlayerTeleportEvent.TeleportCause.PLUGIN);
                destroyMenu();
            }
        } else if (click.isShiftClick() && click.isRightClick()) {
            Integer pkID = favorites.get(pos);
            if (pkID != null) {
                favorites.remove(pos);
                inv.setItem(slot, null);
                save();
            }
        }
    }

    @Override
    public void destroyMenu() {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        }, 1L);
    }

    public void addParkour(int i) {
        if (favorites.contains(i)) {
            player.sendMessage(Parkour.getString("favorites.alreadyadded"));
            return;
        }
        player.sendMessage(Parkour.getString("favorites.added"));
        favorites.add(i);
        plugin.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(SimpleAchievement.AchievementCriteria.FAVORITES_NUMBER, (long) favorites.size()));
    }

    @Override
    public void save() {
        try {
            PreparedStatement statement = conn.prepareStatement("INSERT INTO favorites (`player`,`favorites`) VALUES (?,?) ON DUPLICATE KEY UPDATE favorites = ?");
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

    public int getCurrentPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
    
    public int size() {
        return favorites.size();
    }
}
