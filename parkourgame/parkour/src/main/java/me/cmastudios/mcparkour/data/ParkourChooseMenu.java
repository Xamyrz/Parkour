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

package me.cmastudios.mcparkour.data;

import me.cmastudios.mcparkour.Item;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.tasks.ParkourStartTask;
import me.cmastudios.mcparkour.tasks.TeleportToCourseTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.lang.Runnable;

public class ParkourChooseMenu {
    private int page = 0;
    private ParkourChooseCriterium criterium = ParkourCourse.CourseDifficulty.EASY;

    public ParkourChooseCriterium getCriterium() {
        return criterium;
    }

    public void setCriterium(ParkourChooseCriterium criterium) {
        this.criterium = criterium;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void render(Inventory inv, Player player, Parkour plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin,  new GetAndRender(plugin, player, criterium, page, inv));
//        new GetAndRender(plugin, player, criterium, page, inv).runTaskAsynchronously(plugin);
    }

    public void handleClick(Inventory inv, ItemStack is, Parkour plugin, Player player) {
        if (is == null || is.getType() == Material.AIR) {
            return;
        }
        boolean functional = inv.first(is) > 44;
        if (functional) {
            Item item = Item.getSimiliarItem(is);
            if (item != null) {
                switch (item) {
                    case NEXT_PAGE:
                        page++;
                        break;
                    case PREV_PAGE:
                        if (page != 0) {
                            page--;
                        }
                        break;
                    default:
                        try {
                            this.criterium = ParkourCourse.CourseDifficulty.valueOf(item.name());
                        } catch (IllegalArgumentException e) {
                            try {
                                this.criterium = ParkourCourse.CourseMode.valueOf(item.name());
                            } catch (IllegalArgumentException ex) {
                                return;
                            }
                        }
                        page = 0;
                }
            }
            player.closeInventory();
            render(inv, player, plugin);
            player.openInventory(inv);
            return;
        }
        if(inv.first(is) + 1 + (page * 45) != 0)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new GetParkourIdAndTeleport(plugin, inv.first(is) + 1 + (page * 45), player));
        //new GetParkourIdAndTeleport(plugin, inv.first(is) + 1 + (page * 45), player).runTaskAsynchronously(plugin);
    }

    public static interface ParkourChooseCriterium {
        public String getType();

        public String getName();
    } //No better idea, sorry :D

    private class GetParkourIdAndTeleport implements Runnable {
        private final Parkour plugin;
        private final int rowNumber;
        private final Player player;

        public GetParkourIdAndTeleport(Parkour plugin, int rowNumber, Player player) {
            this.plugin = plugin;
            this.player = player;
            this.rowNumber = rowNumber;
        }

        @Override
        public void run() {
            StringBuilder builder = new StringBuilder("SELECT id FROM courses WHERE (courses.mode = ?");
            if (player.hasPermission("parkour.vip") && criterium == ParkourCourse.CourseMode.THEMATIC) {
                builder.append(" OR courses.mode = 'VIP'");
            }
            builder.append(")");
            if (criterium.getType().equalsIgnoreCase("difficulty")) {
                builder.append(" AND courses.difficulty = ?");
            }
            builder.append(" ORDER BY id ASC LIMIT ?,1");
            try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement(builder.toString())) {
                stmt.setString(1, criterium.getType().equalsIgnoreCase("mode") ? criterium.getName() : ParkourCourse.CourseMode.NORMAL.getName());
                if (criterium.getType().equalsIgnoreCase("difficulty")) {
                    stmt.setString(2, criterium.getName());
                    stmt.setInt(3, rowNumber - 1);
                } else {
                    stmt.setInt(2, rowNumber - 1);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        if (new TeleportToCourseTask(plugin, player, PlayerTeleportEvent.TeleportCause.COMMAND, rs.getInt("id")).performWithResult() != Parkour.PlayResult.ALLOWED) {
                            player.closeInventory();
                            Inventory inv = Bukkit.createInventory(player, 54, Parkour.getString("choosemenu.title"));
                            render(inv, player, plugin);
                            player.openInventory(inv);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetAndRender implements Runnable {
        private final int page;
        private final ParkourChooseCriterium criterium;
        private Inventory inventory;
        private Player player;
        private final Parkour plugin;

        public GetAndRender(Parkour plugin, Player player, ParkourChooseCriterium criterium, int page, Inventory inv) {
            this.page = page;
            this.plugin = plugin;
            this.player = player;
            this.criterium = criterium;
            this.inventory = inv;
        }

        @Override
        public void run() {
            try {
                StringBuilder query = new StringBuilder("SELECT courses.id,courses.mode,courses.difficulty,courses.name,b.time,b.plays FROM courses LEFT JOIN (SELECT * FROM highscores WHERE `uuid` = ?) b ON b.course = courses.id WHERE (courses.mode = ?");
                if (player.hasPermission("parkour.vip") && criterium == ParkourCourse.CourseMode.THEMATIC) {
                    query.append(" OR courses.mode = 'VIP'");
                }
                query.append(")");
                if (criterium.getType().equalsIgnoreCase("difficulty")) {
                    query.append(" AND courses.difficulty = ?");
                }
                query.append(" LIMIT ?,?");
                try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement(query.toString())) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, criterium.getType().equalsIgnoreCase("mode") ? criterium.getName() : ParkourCourse.CourseMode.NORMAL.getName());
                    if (criterium.getType().equalsIgnoreCase("difficulty")) {
                        stmt.setString(3, criterium.getName());
                        stmt.setInt(4, page * 45);
                        stmt.setInt(5, ((page + 1) * 45));
                    } else {
                        stmt.setInt(3, page * 45);
                        stmt.setInt(4, ((page + 1) * 45));
                    }
                    try (final ResultSet rs = stmt.executeQuery()) {
                        final ItemStack is = Item.valueOf(criterium.getName()).getItem();
                        final ItemMeta meta = is.getItemMeta();

                        DecimalFormat format = new DecimalFormat("#.###");
                        inventory.clear();
                        int control = 0;
                        while (rs.next()) {
                            meta.setDisplayName(Parkour.getString("item.icons." + criterium.getName().toLowerCase(), rs.getString("name"), rs.getInt("id")));
                            meta.setLore(Parkour.getMessageArrayFromPrefix("choosemenu.entry.lore", String.valueOf(rs.getInt("plays")), format.format((rs.getLong("time") == -1 ? 0 : rs.getLong("time")) / 1000.0)));
                            is.setItemMeta(meta);
                            inventory.addItem(is.clone());
                            control++;
                        }
                        if (page > 0) {
                            ItemStack iss = Item.PREV_PAGE.getItem();
                            is.setAmount(page - 1);
                            inventory.setItem(45, iss);
                        }
                        inventory.setItem(46, Item.EASY.getItem());
                        inventory.setItem(47, Item.MEDIUM.getItem());
                        inventory.setItem(48, Item.HARD.getItem());
                        inventory.setItem(49, Item.VERYHARD.getItem());
                        inventory.setItem(50, Item.THEMATIC.getItem());
                        inventory.setItem(51, Item.CUSTOM.getItem());
                        inventory.setItem(52, Item.ADVENTURE.getItem());
                        if (control >= 45) {
                            ItemStack iss = Item.NEXT_PAGE.getItem();
                            iss.setAmount(page + 1);
                            inventory.setItem(53, iss);
                        }

                    } catch (IllegalArgumentException e) {
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }
}