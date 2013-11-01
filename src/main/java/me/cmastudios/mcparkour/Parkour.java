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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;

import me.cmastudios.mcparkour.commands.AdventureCommand;
import me.cmastudios.mcparkour.commands.DeleteCourseCommand;
import me.cmastudios.mcparkour.commands.DuelCommand;
import me.cmastudios.mcparkour.commands.GuildCommand;
import me.cmastudios.mcparkour.commands.LevelCommand;
import me.cmastudios.mcparkour.commands.ListCoursesCommand;
import me.cmastudios.mcparkour.commands.ParkourCommand;
import me.cmastudios.mcparkour.commands.SetCheckpointCommand;
import me.cmastudios.mcparkour.commands.SetCourseCommand;
import me.cmastudios.mcparkour.commands.TopScoresCommand;
import me.cmastudios.mcparkour.data.Guild;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

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
    public Map<Player, Checkpoint> playerCheckpoints = new HashMap<Player, Checkpoint>();
    public Map<Player, PlayerCourseData> playerCourseTracker = new HashMap<Player, PlayerCourseData>();
    public Map<Player, PlayerCourseData> completedCourseTracker = new HashMap<Player, PlayerCourseData>();
    public Map<Player, GuildPlayer> guildChat = new HashMap<Player, GuildPlayer>();
    public List<Duel> activeDuels = new ArrayList<Duel>();
    public List<GuildWar> activeWars = new ArrayList<GuildWar>();
    public final ItemStack VISION = new ItemStack(Material.EYE_OF_ENDER);
    public final ItemStack CHAT = new ItemStack(Material.PAPER);
    public final ItemStack SPAWN = new ItemStack(Material.NETHER_STAR);
    public final ItemStack POINT = new ItemStack(Material.STICK);

    @Override
    public void onEnable() {
        this.getCommand("parkour").setExecutor(new ParkourCommand(this));
        this.getCommand("setcourse").setExecutor(new SetCourseCommand(this));
        this.getCommand("deletecourse").setExecutor(new DeleteCourseCommand(this));
        this.getCommand("listcourses").setExecutor(new ListCoursesCommand(this));
        this.getCommand("topscores").setExecutor(new TopScoresCommand(this));
        this.getCommand("checkpoint").setExecutor(new SetCheckpointCommand(this));
        this.getCommand("duel").setExecutor(new DuelCommand(this));
        this.getCommand("lvl").setExecutor(new LevelCommand(this));
        this.getCommand("guild").setExecutor(new GuildCommand(this));
        this.getCommand("adventure").setExecutor(new AdventureCommand(this));
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
        meta = POINT.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.point", new Object[]{}));
        String[] lore = {
                Parkour.getString("item.point.description.0", new Object[] {}),
                Parkour.getString("item.point.description.1", new Object[] {}),
                Parkour.getString("item.point.description.2", new Object[] {}) };
        meta.setLore(Arrays.asList(lore));
        POINT.setItemMeta(meta);
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
        playerCheckpoints.clear();
        for (Iterator<Entry<Player, PlayerCourseData>> it = playerCourseTracker.entrySet().iterator(); it.hasNext();) {
            Entry<Player, PlayerCourseData> entry = it.next();
            it.remove();
            entry.getValue().leave(entry.getKey());
        }
        for (Player player : completedCourseTracker.keySet()) {
            player.teleport(this.getSpawn());
        }
        completedCourseTracker.clear();
    }

    public static String getString(String key, Object... args) {
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
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (id INTEGER, x REAL, y REAL, z REAL, pitch REAL, yaw REAL, world TEXT, detection INT, mode ENUM('normal', 'guildwar', 'adventure', 'vip') NOT NULL DEFAULT 'normal', difficulty ENUM('easy', 'medium', 'hard', 'veryhard') NOT NULL DEFAULT 'easy')");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (player varchar(16), course INTEGER, time BIGINT, plays INT)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (player varchar(16), xp INTEGER)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds (tag varchar(5), name varchar(32))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guildplayers (player varchar(16), guild varchar(5), rank enum('default','officer','leader') NOT NULL DEFAULT 'default')");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS adventures (name varchar(32), course INTEGER)");
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

    public int getNextLevelRequiredXp(int experience) {
        int base = this.getConfig().getInt("levels.base");
        int addt = this.getConfig().getInt("levels.addition");
        if (experience < base) {
            return 1;
        }
        int xplast = 0;
        for (int x = 2; x < Integer.MAX_VALUE; x++) {
            int xpreq = xplast + base + (addt * (x - 2));
            if (experience < xpreq) {
                return xpreq - experience;
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

    public Location getSpawn() {
        World world = this.getServer().getWorld(this.getConfig().getString("spawn.world"));
        double x = this.getConfig().getDouble("spawn.x");
        double y = this.getConfig().getDouble("spawn.y");
        double z = this.getConfig().getDouble("spawn.z");
        float pitch = (float) this.getConfig().getDouble("spawn.pitch");
        float yaw = (float) this.getConfig().getDouble("spawn.yaw");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Duel getDuel(Player participator) {
        for (Duel duel : this.activeDuels) {
            if (duel.getInitiator() == participator || duel.getCompetitor() == participator) {
                return duel;
            }
        }
        return null;
    }

    public GuildWar getWar(Guild belligerent) {
        for (GuildWar war : this.activeWars) {
            if (war.getInitiator().equals(belligerent) || war.getCompetitor().equals(belligerent)) {
                return war;
            }
        }
        return null;
    }

    public GuildWar getWar(Player belligerent) {
        for (GuildWar war : this.activeWars) {
            for (GuildPlayer warrior : war.getWarriors()) {
                if (warrior.getPlayer().getName().equals(belligerent.getName())) {
                    return war;
                }
            }
        }
        return null;
    }

    public boolean canDuel(int exp) {
        return this.getLevel(exp) >= 8;
    }

    public boolean canPlay(int exp, CourseDifficulty diff) {
        return this.getLevel(exp) >= this.getConfig().getInt("restriction." + diff.name().toLowerCase());
    }

    public static void broadcast(List<Player> list, String message) {
        for (Player receipient : list) {
            receipient.sendMessage(message);
        }
    }

    public static class PlayerCourseData {

        public final ParkourCourse course;
        public final long startTime;
        public final int previousLevel;

        public void restoreState(Player player) {
            player.setExp(0.0F);
            player.setLevel(previousLevel);
        }

        public void leave(Player player) {
            this.restoreState(player);
            try {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            } catch (Exception ex) {}
            player.teleport(((Parkour)player.getServer().getPluginManager().getPlugin("Parkour")).getSpawn());
        }

        public PlayerCourseData(ParkourCourse course, Player player) {
            this.course = course;
            this.startTime = System.currentTimeMillis();
            this.previousLevel = player.getLevel();
            player.setExp(0.0F);
            player.setLevel(0);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.SLOW);
        }
    }
}
