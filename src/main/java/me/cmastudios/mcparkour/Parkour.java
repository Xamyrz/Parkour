/*
 * Copyright (C) 2013 Connor Monahan
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
package me.cmastudios.mcparkour;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import me.cmastudios.mcparkour.commands.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for mcparkour Bukkit plugin.
 *
 * @author Connor Monahan
 */
public class Parkour extends JavaPlugin {

    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection courseDatabase;
    public List<Player> blindPlayers = new ArrayList<Player>();
    public List<Player> deafPlayers = new ArrayList<Player>();
    public final ItemStack VISION = new ItemStack(Material.EYE_OF_ENDER);
    public final ItemStack CHAT = new ItemStack(Material.PAPER);
    public final ItemStack SPAWN = new ItemStack(Material.NETHER_STAR);

    @Override
    public void onEnable() {
        this.getCommand("parkour").setExecutor(new ParkourCommand(this));
        this.getCommand("setcourse").setExecutor(new SetCourseCommand(this));
        this.getCommand("topscores").setExecutor(new TopScoresCommand(this));
        this.getServer().getPluginManager().registerEvents(new ParkourListener(this), this);
        this.getDataFolder().mkdirs();
        this.saveDefaultConfig();
        this.connectDatabase();
        ItemMeta meta = VISION.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.vision", new Object[]{}));
        VISION.setItemMeta(meta);
        meta = CHAT.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.chat", new Object[]{}));
        CHAT.setItemMeta(meta);
        meta = SPAWN.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.spawn", new Object[]{}));
        SPAWN.setItemMeta(meta);
    }

    @Override
    public void onDisable() {
        if (this.courseDatabase != null) {
            try {
                this.courseDatabase.close();
            } catch (SQLException ex) {
            }
        }
        for (Iterator<Player> it = blindPlayers.iterator(); it.hasNext();) {
            Player player = it.next();
            it.remove();
            refreshVision(player);
            refreshHand(player);
        }
        synchronized (deafPlayers) {
            deafPlayers.clear();
        }
    }

    public static String getString(String key, Object[] args) {
        return MessageFormat.format(messages.getString(key), args);
    }

    public void connectDatabase() {
        try {
            if (courseDatabase != null && !courseDatabase.isClosed()) {
                courseDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            if (this.getConfig().getBoolean("mysql.enabled", false)) {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                this.courseDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                        this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                        this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            } else {
                Class.forName("org.sqlite.JDBC").newInstance();
                File courseDatabaseFile = new File(this.getDataFolder(), "courses.sl3");
                this.courseDatabase = DriverManager.getConnection("jdbc:sqlite:" + courseDatabaseFile.getPath());
            }
            try (Statement initStatement = this.courseDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (id INTEGER, x REAL, y REAL, z REAL, pitch REAL, yaw REAL, world TEXT)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (player TEXT, course INTEGER, time BIGINT)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (player TEXT, xp INTEGER)");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load database driver", ex);
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    public Connection getCourseDatabase() {
        try {
            if (!courseDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return courseDatabase;
    }

    public int getLevel(int experience) {
        int base = this.getConfig().getInt("levels.base");
        int addt = this.getConfig().getInt("levels.addition");
        if (experience < base) {
            return 1;
        }
        int xplast = 0;
        for (int x = 2; x < Integer.MAX_VALUE; x++) {
            int xpreq = xplast + base + (addt * (x - 2));
            if (experience < xpreq) {
                return x - 1;
            }
            xplast = xpreq;
        }
        return Integer.MAX_VALUE;
    }

    public void refreshVision(Player player) {
        boolean isBlind = blindPlayers.contains(player);
        for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
            if (player != onlinePlayer && isBlind) {
                player.hidePlayer(onlinePlayer);
            } else if (player != onlinePlayer) {
                player.showPlayer(onlinePlayer);
            }
        }
    }

    public void refreshHand(Player player) {
        boolean inHand = player.getItemInHand().getType() == Material.ENDER_PEARL || player.getItemInHand().getType() == Material.EYE_OF_ENDER;
        ItemStack item = VISION.clone();
        item.setType(blindPlayers.contains(player) ? Material.ENDER_PEARL : Material.EYE_OF_ENDER);
        player.getInventory().remove(Material.ENDER_PEARL);
        player.getInventory().remove(Material.EYE_OF_ENDER);
        if (inHand) {
            player.setItemInHand(item);
        } else {
            player.getInventory().addItem(item);
        }
    }
}
