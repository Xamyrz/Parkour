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
import me.cmastudios.mcparkour.event.ParkourEvent;
import me.cmastudios.mcparkour.data.*;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Main class for mcparkour Bukkit plugin.
 *
 * @author Connor Monahan
 */
public class Parkour extends JavaPlugin {

    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection courseDatabase;
    private boolean chat = true;
    private double ratio = 1;
    public static boolean isBarApiEnabled = false;
    private ParkourEvent event;
    public List<Player> blindPlayers = new ArrayList<>();
    public final List<Player> deafPlayers = new ArrayList<>();
    public ConcurrentHashMap<Player, Checkpoint> playerCheckpoints = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Player, PlayerCourseData> playerCourseTracker = new ConcurrentHashMap<>();
    public Map<Player, PlayerCourseData> completedCourseTracker = new HashMap<>();
    public Map<Player, GuildPlayer> guildChat = new HashMap<>();
    public Map<Player, List<Player>> blindPlayerExempts = new HashMap<>();
    public Map<String, Long> fireworkCooldown = new HashMap<>();
    public Map<String, Long> favoritesCooldown = new HashMap<>();
    public List<Duel> activeDuels = new ArrayList<>();
    public List<GuildWar> activeWars = new ArrayList<>();
    public static ExperienceManager experience;
    public final Random random = new Random();

    @Override
    public void onEnable() {
        this.getCommand("parkour").setExecutor(new ParkourCommand(this));
        this.getCommand("setcourse").setExecutor(new SetCourseCommand(this));
        this.getCommand("deletecourse").setExecutor(new DeleteCourseCommand(this));
        this.getCommand("listcourses").setExecutor(new ListCoursesCommand(this));
        this.getCommand("topscores").setExecutor(new TopScoresCommand(this));
        this.getCommand("checkpoint").setExecutor(new SetCheckpointCommand(this));
        this.getCommand("duel").setExecutor(new DuelCommand(this));
        this.getCommand("guild").setExecutor(new GuildCommand(this));
        this.getCommand("adventure").setExecutor(new AdventureCommand(this));
        this.getCommand("see").setExecutor(new BlindCommand(this));
        this.getCommand("highscores").setExecutor(new HighscoresCommand(this));
        this.getCommand("chat").setExecutor(new ChatCommand(this));
        this.getCommand("pkroom").setExecutor(new PkRoomCommand(this));
        this.getCommand("ratio").setExecutor(new RatioCommand(this));
        this.getCommand("event").setExecutor(new EventCommand(this));
        this.getServer().getPluginManager().registerEvents(new ParkourListener(this), this);
        this.setupExperience();
        this.saveDefaultConfig();
        this.connectDatabase();
        try {
            this.rebuildHeads();
        } catch (SQLException e) {
            this.getLogger().log(Level.WARNING, "Failed loading effect heads", e);
        }
        Plugin pln = Bukkit.getPluginManager().getPlugin("BarAPI");
        if (pln != null) {
            isBarApiEnabled = true;
        }
    }

    private boolean setupExperience() {
        RegisteredServiceProvider<ExperienceManager> xpProvider = getServer().getServicesManager().getRegistration(me.cmastudios.experience.ExperienceManager.class);
        if (xpProvider != null) {
            experience = xpProvider.getProvider();
        }

        return (experience != null);
    }


    @Override
    public void onDisable() {
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
            refreshHand(player);
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

    public static String[] getMessageArrayFromPrefix(String prefix) {
        Enumeration<String> keys = messages.getKeys();
        ArrayList<String> res = new ArrayList<>();

        for (; keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            if (key.startsWith(prefix)) {
                res.add(Parkour.getString(key));
            }
        }
        return res.toArray(new String[res.size()]);
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
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            this.courseDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                    this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.courseDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (id INTEGER, x REAL, y REAL, z REAL, pitch REAL, yaw REAL, world TEXT, detection INT, mode ENUM('normal', 'guildwar', 'adventure', 'vip', 'hidden', 'event') NOT NULL DEFAULT 'normal', difficulty ENUM('easy', 'medium', 'hard', 'veryhard') NOT NULL DEFAULT 'easy', name VARCHAR(20) NOT NULL DEFAULT '',PRIMARY KEY (id))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS events (id INTEGER, type enum('TIME_RUSH', 'POSITION_RUSH', 'PLAYS_RUSH', 'DISTANCE_RUSH') NOT NULL DEFAULT 'TIME_RUSH',PRIMARY KEY (id))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (player varchar(16), course INTEGER, time BIGINT, plays INT,PRIMARY KEY (player))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (player varchar(16), xp INTEGER,PRIMARY KEY (player))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds (tag varchar(5), name varchar(32),PRIMARY KEY (tag))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guildplayers (player varchar(16), guild varchar(5), rank enum('default','officer','leader') NOT NULL DEFAULT 'default',PRIMARY KEY (player))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS adventures (name varchar(32), course INTEGER,PRIMARY KEY (name))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courseheads (world_name varchar(32), x INTEGER, y INTEGER, z INTEGER, course_id INTEGER, skull_type_name varchar(32))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS gameresults (time TIMESTAMP, type enum('duel','guildwar'), winner varchar(16), loser varchar(16))");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load database driver", ex);
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    // TODO replace with connection pool
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

    public void refreshVision(Player player) {
        boolean isBlind = blindPlayers.contains(player);
        for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
            if (player != onlinePlayer && isBlind) {
                if (blindPlayerExempts.containsKey(player)) {
                    if (blindPlayerExempts.get(player).contains(onlinePlayer)) {
                        player.showPlayer(onlinePlayer);
                        continue;
                    }
                }
                player.hidePlayer(onlinePlayer);
            } else if (player != onlinePlayer) {
                player.showPlayer(onlinePlayer);
            }
        }
    }

    public void refreshHand(Player player) {
        boolean inHand = player.getItemInHand().getType() == Material.ENDER_PEARL || player.getItemInHand().getType() == Material.EYE_OF_ENDER;
        ItemStack item = Item.VISION.getItem();
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
        return Parkour.experience.getLevel(exp) >= this.getConfig().getInt("restriction.duel");
    }

    public PlayResult canPlay(Player player, int exp, ParkourCourse course) throws SQLException {
        if (course == null) {
            return PlayResult.NOT_FOUND;
        } else if (course.getMode() == CourseMode.GUILDWAR && getWar(player) == null) {
            return PlayResult.NOT_IN_GUILD_WAR;
        } else if ((course.getMode() == CourseMode.VIP || course.getMode() == CourseMode.ADVENTURE) && !player.hasPermission("parkour.vip")) {
            return PlayResult.VIP_NOT_BOUGHT;
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
        ALLOWED(null), NOT_IN_GUILD_WAR("guild.war.notin"), ADVENTURE_NOTPLAYED("adv.notplayed"), VIP_NOT_BOUGHT("vip.notbought"), INSUFFICIENT_XP("xp.insufficient"), NOT_FOUND("error.course404");
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

    public static void broadcast(List<Player> list, String message) {
        for (Player recipient : list) {
            recipient.sendMessage(message);
        }
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

    public void spawnRandomFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();
        int rt = random.nextInt(5) + 1;
        Type type = Type.BALL;
        switch (rt) {
            case 1:
                type = Type.BALL;
                break;
            case 2:
                type = Type.BURST;
                break;
            case 3:
                type = Type.CREEPER;
                break;
            case 4:
                type = Type.STAR;
                break;
            case 5:
                type = Type.BALL_LARGE;
                break;
        }
        FireworkEffect effect = FireworkEffect.builder().flicker(random.nextBoolean()).withColor(getRandomColor()).withFade(getRandomColor()).with(type).trail(random.nextBoolean()).build();
        fwm.addEffect(effect);
        fwm.setPower(0);
        fw.setFireworkMeta(fwm);
    }

    private Color getRandomColor() {
        Color c = null;
        Random r = new Random();
        int i = r.nextInt(17) + 1;
        switch (i) {
            case 1:
                c = Color.AQUA;
                break;
            case 2:
                c = Color.BLACK;
                break;
            case 3:
                c = Color.BLUE;
                break;
            case 4:
                c = Color.FUCHSIA;
                break;
            case 5:
                c = Color.GRAY;
                break;
            case 6:
                c = Color.GREEN;
                break;
            case 7:
                c = Color.LIME;
                break;
            case 8:
                c = Color.MAROON;
                break;
            case 9:
                c = Color.NAVY;
                break;
            case 10:
                c = Color.OLIVE;
                break;
            case 11:
                c = Color.ORANGE;
                break;
            case 12:
                c = Color.PURPLE;
                break;
            case 13:
                c = Color.RED;
                break;
            case 14:
                c = Color.SILVER;
                break;
            case 15:
                c = Color.TEAL;
                break;
            case 16:
                c = Color.WHITE;
                break;
            case 17:
                c = Color.YELLOW;
                break;
        }
        return c;
    }

    public SkullType getSkullFromDurability(short durability) {
        switch (durability) {
            case 1:
                return SkullType.WITHER;
            case 3:
                return SkullType.PLAYER;
            default:
                return SkullType.SKELETON;
        }
    }

    public ParkourEvent getEvent() {
        return event;
    }

    public void setEvent(ParkourEvent event) {
        this.event = event;
    }

    public boolean isChatEnabled() {
        return chat;
    }

    public void setChat(boolean state) {
        chat = state;
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
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.SLOW);
        }
    }
}
