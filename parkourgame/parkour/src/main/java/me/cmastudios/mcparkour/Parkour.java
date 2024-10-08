/*
 * Copyright (C) 2014 Connor Monahan
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

import me.cmastudios.experience.ExperienceManager;
import me.cmastudios.mcparkour.commands.*;
import me.cmastudios.mcparkour.data.*;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;
import me.cmastudios.mcparkour.event.ParkourEvent;
import me.cmastudios.mcparkour.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main class for mcparkour Bukkit plugin.
 *
 * @author Connor Monahan
 */
public class Parkour extends JavaPlugin {

    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection courseDatabase;
    private double ratio = 1;
    public static boolean isBarApiEnabled = true;
    private ParkourEvent event;
    public Map<Player, Menu> playersMenus = new HashMap<>();
    public Map<Integer, ParkourCourse> courses = new HashMap<>();
    public List<Player> blindPlayers = new ArrayList<>();
    public final List<Player> deafPlayers = new ArrayList<>();
    public ConcurrentHashMap<Player, Checkpoint> playerCheckpoints = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Player, PlayerCourseData> playerCourseTracker = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Player, PlayerTrackerData> playerTracker = new ConcurrentHashMap<>();
    public Map<Player, PlayerCourseData> completedCourseTracker = new HashMap<>();
    public Map<Player, GuildPlayer> guildChat = new HashMap<>();
    public Map<Player, List<Player>> blindPlayerExempts = new HashMap<>();
    public List<Player> vanishedPlayers = new ArrayList<>();
    public List<Duel> activeDuels = new ArrayList<>();
    public List<GuildWar> activeWars = new ArrayList<>();
    public static ExperienceManager experience;
    public NamespacedKey jumpBlockKey;
    public JumpBlocks jumpBlocks;

    @Override
    public void onEnable() {
        this.getCommand("parkour").setExecutor(new ParkourCommand(this));
        this.getCommand("setcourse").setExecutor(new SetCourseCommand(this));
        this.getCommand("setjumps").setExecutor(new SetJumpsCommand(this));
        this.getCommand("deletecourse").setExecutor(new DeleteCourseCommand(this));
        this.getCommand("listcourses").setExecutor(new ListCoursesCommand(this));
        this.getCommand("topscores").setExecutor(new TopScoresCommand(this));
        this.getCommand("checkpoint").setExecutor(new SetCheckpointCommand(this));
        this.getCommand("duel").setExecutor(new DuelCommand(this));
        this.getCommand("guild").setExecutor(new GuildCommand(this));
        this.getCommand("adventure").setExecutor(new AdventureCommand(this));
        this.getCommand("see").setExecutor(new BlindCommand(this));
        this.getCommand("highscores").setExecutor(new HighscoresCommand(this));
        this.getCommand("pkroom").setExecutor(new PkRoomCommand(this));
        this.getCommand("ratio").setExecutor(new RatioCommand(this));
        this.getCommand("event").setExecutor(new EventCommand(this));
        this.getCommand("custom").setExecutor(new CustomCourseCommand(this));
        this.getCommand("vanish").setExecutor(new VanishCommand(this));
        this.getServer().getPluginManager().registerEvents(new ParkourListener(this), this);
        this.setupExperience();
        this.saveDefaultConfig();
        try {
            this.connectDatabase();
            this.loadCourses(courseDatabase);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            this.rebuildHeads();
        } catch (SQLException e) {
            this.getLogger().log(Level.WARNING, "Failed loading effect heads", e);
        }
        this.jumpBlockKey = new NamespacedKey(this, "jumpBlock");
        jumpBlocks = new JumpBlocks(this);
    }

    private void setupExperience() {
        RegisteredServiceProvider<ExperienceManager> xpProvider = getServer().getServicesManager().getRegistration(me.cmastudios.experience.ExperienceManager.class);
        if (xpProvider != null) {
            experience = xpProvider.getProvider();
        }
    }

    @Override
    public void onDisable() {
        jumpBlocks.removeJumpBlockEntities();
        this.getServer().getServicesManager().unregisterAll(this);
        if (this.courseDatabase != null) {
            try {
                this.courseDatabase.close();
            } catch (SQLException ignored) {
            }
        }
        for (Iterator<Player> it = blindPlayers.iterator(); it.hasNext(); ) {
            Player player = it.next();
            it.remove();
            refreshVision(player);
        }
        synchronized (deafPlayers) {
            deafPlayers.clear();
        }
        playerCheckpoints.clear();
        for (Iterator<Entry<Player, PlayerCourseData>> it = playerCourseTracker.entrySet().iterator(); it.hasNext(); ) {
            Entry<Player, PlayerCourseData> entry = it.next();
            it.remove();
            entry.getValue().leave(entry.getKey());
        }
        for (Player player : completedCourseTracker.keySet()) {
            player.teleport(this.getSpawn());
        }
        completedCourseTracker.clear();
        blindPlayerExempts.clear();
        experience = null;
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
    }

    public static List<String> getMessageArrayFromPrefix(String prefix, String... args) {
        Set<String> keys = messages.keySet();
        TreeSet<String> res = new TreeSet<>();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                res.add(key);
            }
        }
        ArrayList<String> mess = new ArrayList<>();
        for (String key : res) {
            mess.add(Parkour.getString(key, args));
        }
        return mess;
    }

    public void connectDatabase() throws SQLException{
        try {
            if (courseDatabase != null && !courseDatabase.isClosed()) {
                courseDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            this.courseDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                    this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.courseDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (id INTEGER, x REAL, y REAL, z REAL, pitch REAL, yaw REAL, world TEXT, detection INT, `mode` ENUM('normal', 'guildwar', 'adventure', 'vip', 'hidden', 'event', 'custom', 'thematic','donation','locked') NOT NULL DEFAULT 'normal', difficulty ENUM('easy', 'medium', 'hard', 'veryhard') NOT NULL DEFAULT 'easy', name VARCHAR(25) NOT NULL DEFAULT '',PRIMARY KEY (id))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS events (id INTEGER, type enum('TIME_RUSH', 'POSITION_RUSH', 'PLAYS_RUSH', 'DISTANCE_RUSH') NOT NULL DEFAULT 'TIME_RUSH',PRIMARY KEY (id))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (uuid varchar(255), course INTEGER, time BIGINT, plays INT, `timestamp` TIMESTAMP, UNIQUE KEY (uuid,course))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS custom (id int(11) NOT NULL AUTO_INCREMENT, effects mediumtext NOT NULL, PRIMARY KEY (id))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds (tag varchar(5), name varchar(32),PRIMARY KEY (tag))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guildplayers (uuid varchar(255), guild varchar(5), `rank` ENUM('default', 'officer', 'leader') NOT NULL DEFAULT 'default', PRIMARY KEY (uuid))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS adventures (name varchar(32), course INTEGER,PRIMARY KEY (name))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courseheads (world_name varchar(32), x INTEGER, y INTEGER, z INTEGER, course_id INTEGER, skull_type_name varchar(32))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS gameresults (time TIMESTAMP, `type` ENUM('duel','guildwar'), uuidwin varchar(255), uuidloss varchar(255))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (`uuid` varchar(255) NOT NULL, `name` varchar(16) NOT NULL, PRIMARY KEY (`uuid`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    // TODO replace with connection pool
    public Connection getCourseDatabase() throws SQLException {
        try {
            if (!courseDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return courseDatabase;
    }

    public void loadCourses(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courses")) {
            try (ResultSet result = stmt.executeQuery()) {
                while(result.next()) {
                    courses.put(result.getInt("id"), new ParkourCourse(result.getInt("id"),
                                        result.getString("name"),new Location(
                                        Bukkit.getWorld(result.getString("world")),
                                        result.getDouble("x"), result.getDouble("y"),
                                        result.getDouble("z"), result.getFloat("yaw"),
                                        result.getFloat("pitch")),
                                        result.getInt("detection"),
                                        CourseMode.valueOf(result.getString("mode").toUpperCase()),
                                        CourseDifficulty.valueOf(result.getString("difficulty").toUpperCase())));
                }
                setUpdateHighscores(conn);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void setUpdateHighscores(Connection conn) throws SQLException {
        courses.forEach((key, value) -> {
            try {
                value.setHighScores(conn);
                value.updateScoreBoard();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshVision(Player player) {
        boolean isBlind = blindPlayers.contains(player);
        for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
            if (player == onlinePlayer) {
                continue;
            }
            if (!canSee(player, onlinePlayer) || (isBlind && (!blindPlayerExempts.containsKey(player) || !blindPlayerExempts.get(player).contains(onlinePlayer)))) {
                player.hidePlayer(this, onlinePlayer);
                continue;
            }
            player.showPlayer(this, onlinePlayer);
        }
    }

    public void refreshVisionOfOnePlayer(Player player) {
        for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
            if (player == onlinePlayer) {
                continue;
            }
            if (canSee(onlinePlayer, player)) {
                onlinePlayer.showPlayer(this, player);
            } else {
                onlinePlayer.hidePlayer(this, player);
            }
        }
    }

    public boolean canSee(Player seeker, Player target) {
        return (seeker.hasPermission("parkour.vanish.seevanished") || (!vanishedPlayers.contains(target) && !target.hasPermission("parkour.vanish.alwaysvanished"))) && (blindPlayers.contains(seeker) && (blindPlayerExempts.containsKey(seeker) && blindPlayerExempts.get(seeker).contains(target)) || !blindPlayers.contains(seeker));
    }

    public Location getSpawn() {
        World world = this.getServer().getWorld(this.getConfig().getString("spawn.world"));
        return world != null ? world.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
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
        return Parkour.experience.getLevel(exp) >= this.getConfig().getInt("restriction.duel");
    }

    public PlayResult canPlay(Player player, int exp, ParkourCourse course) throws SQLException {
        if (course == null) {
            return PlayResult.NOT_FOUND;
        } else if (!player.hasPermission("parkour.play") || (course.getMode() == CourseMode.EVENT && !player.hasPermission("parkour.event"))) {
            return PlayResult.NO_PERMS;
        } else if (course.getMode() == CourseMode.GUILDWAR && getWar(player) == null) {
            return PlayResult.NOT_IN_GUILD_WAR;
        } else if ((course.getMode() == CourseMode.VIP || course.getMode() == CourseMode.ADVENTURE) && !player.hasPermission("parkour.vip")) {
            return PlayResult.VIP_NOT_BOUGHT;
        } else if (course.getMode() == CourseMode.LOCKED) {
            if (player.hasPermission("parkour.locked")) {
                return PlayResult.ALLOWED;
            }
            return PlayResult.MAP_NOT_DONATED;
        } else if (Parkour.experience.getLevel(exp) < getLevelRequiredToPlay(course.getDifficulty()) && !player.hasPermission("parkour.bypasslevel")) {
            return PlayResult.INSUFFICIENT_XP;
        } else if (course.getMode() == CourseMode.ADVENTURE) {
            AdventureCourse adv = AdventureCourse.loadAdventure(getCourseDatabase(), course);
            if (adv != null && adv.getChapter(course) > 1) {
                ParkourCourse parent = adv.getCourses().get(adv.getChapter(course) - 2);
                PlayerHighScore score = PlayerHighScore.loadHighScore(getCourseDatabase(), player, parent.getId());
                if (score.getTime() == Long.MAX_VALUE) {
                    return PlayResult.ADVENTURE_NOTPLAYED;
                }
            }
        }
        return PlayResult.ALLOWED;
    }

    public enum PlayResult {
        ALLOWED(null), NO_PERMS("course.noperms"), NOT_IN_GUILD_WAR("guild.war.notin"), ADVENTURE_NOTPLAYED("adv.notplayed"), VIP_NOT_BOUGHT("vip.notbought"), INSUFFICIENT_XP("xp.insufficient"), NOT_FOUND("error.course404"), MAP_NOT_DONATED("mapdonations.notdonated");
        public final String key;

        private PlayResult(String messageKey) {
            this.key = messageKey;
        }
    }

    public int getLevelRequiredToPlay(CourseDifficulty diff) {
        return this.getConfig().getInt("restriction." + diff.name().toLowerCase());
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public void rebuildHeads() throws SQLException {
        for (EffectHead head : EffectHead.loadHeads(this.getCourseDatabase())) {
            head.setBlock(this);
        }
    }

    public void rebuildHeads(ParkourCourse course) throws SQLException {
        for (EffectHead head : EffectHead.loadHeads(this.getCourseDatabase(), course)) {
            head.setBlock(this);
        }
    }

    public ParkourEvent getEvent() {
        return event;
    }

    public void setEvent(ParkourEvent event) {
        this.event = event;
    }

    public static class PlayerCourseData {

        public final ParkourCourse course;
        public final long startTime;
        public final int previousLevel;
        public boolean lagged = false;

        public void restoreState(Player player) {
            player.setExp(0.0F);
            player.setLevel(previousLevel);
        }

        public void leave(Player player) {
            this.restoreState(player);
            try {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            } catch (Exception ignored) {
            }
            player.teleport(((Parkour) player.getServer().getPluginManager().getPlugin("Parkour")).getSpawn());
        }

        public PlayerCourseData(ParkourCourse course, Player player, long time) {
            this.course = course;
            this.startTime = time;
            this.previousLevel = player.getLevel();
            player.setExp(0.0F);
            player.setLevel(0);
            Utils.removeEffects(player);
        }
    }

    public static class PlayerTrackerData {
        public boolean lagged = false;
        public int packets = 0;
    }
}
