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

import me.cmastudios.mcparkour.commands.*;
import me.cmastudios.mcparkour.data.EffectHead;
import me.cmastudios.mcparkour.data.Guild;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.cmastudios.mcparkour.data.AchievementMilestone;
import me.cmastudios.mcparkour.data.FavoritesList;
import me.cmastudios.mcparkour.data.ParkourAchievement;
import me.cmastudios.mcparkour.data.ParkourAchievement.AchievementType;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;
import me.cmastudios.mcparkour.data.PlayerAchievements;
import me.cmastudios.mcparkour.data.PlayerExperience;
import me.cmastudios.mcparkour.data.SimpleAchievement;
import me.cmastudios.mcparkour.data.SimpleAchievement.AchievementCriteria;
import me.cmastudios.mcparkour.data.SimpleMilestone;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Main class for mcparkour Bukkit plugin.
 *
 * @author Connor Monahan
 */
public class Parkour extends JavaPlugin {

    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection courseDatabase;
    private boolean chat = true;
    public List<Player> blindPlayers = new ArrayList<>();
    public final List<Player> deafPlayers = new ArrayList<>();
    public Map<Player, Checkpoint> playerCheckpoints = new HashMap<>();
    public Map<Player, PlayerCourseData> playerCourseTracker = new HashMap<>();
    public Map<Player, PlayerCourseData> completedCourseTracker = new HashMap<>();
    public Map<Player, GuildPlayer> guildChat = new HashMap<>();
    public Map<Player, List<Player>> blindPlayerExempts = new HashMap<>();
    public Map<String, Long> fireworkCooldown = new HashMap<>();
    public Map<String, Long> favoritesCooldown = new HashMap<>();
    public Map<Player, FavoritesList> pendingFavs = new HashMap<>();
    public List<ParkourAchievement> achievements = new ArrayList<>();
    public List<AchievementMilestone> milestones = new ArrayList<>();
    public List<Player> disabledScoreboards = new ArrayList<>();
    public List<Duel> activeDuels = new ArrayList<>();
    public List<GuildWar> activeWars = new ArrayList<>();
    public final ItemStack VISION = new ItemStack(Material.EYE_OF_ENDER);
    public final ItemStack CHAT = new ItemStack(Material.PAPER);
    public final ItemStack SPAWN = new ItemStack(Material.NETHER_STAR);
    public final ItemStack POINT = new ItemStack(Material.STICK);
    public final ItemStack HELMET = new ItemStack(Material.GOLD_HELMET);
    public final ItemStack CHESTPLATE = new ItemStack(Material.GOLD_CHESTPLATE);
    public final ItemStack LEGGINGS = new ItemStack(Material.GOLD_LEGGINGS);
    public final ItemStack BOOTS = new ItemStack(Material.GOLD_BOOTS);
    public final ItemStack FIREWORK_SPAWNER = new ItemStack(Material.FIREWORK);
    public final ItemStack SCOREBOARD = new ItemStack(Material.BOOK);
    public final ItemStack FAVORITES = new ItemStack(Material.EMERALD);
    public final ItemStack NEXT_PAGE = new ItemStack(Material.ACTIVATOR_RAIL);
    public final ItemStack PREV_PAGE = new ItemStack(Material.RAILS);
    public final ItemStack EASY = new ItemStack(Material.MINECART);
    public final ItemStack MEDIUM = new ItemStack(Material.STORAGE_MINECART);
    public final ItemStack HARD = new ItemStack(Material.POWERED_MINECART);
    public final ItemStack HIDDEN = new ItemStack(Material.HOPPER_MINECART);
    public final ItemStack V_HARD = new ItemStack(Material.EXPLOSIVE_MINECART);
    public final ItemStack THEMATIC = new ItemStack(Material.BOAT);
    public final ItemStack ADVENTURE = new ItemStack(Material.SADDLE);
    public final ItemStack ACHIEVEMENT = new ItemStack(Material.COAL);
    public final ItemStack ACHIEVEMENT_ACHIEVED = new ItemStack(Material.DIAMOND);
    public final ItemStack ACHIEVEMENTS_MENU = new ItemStack(Material.EXP_BOTTLE);
    private final Random random = new Random();

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
        this.getCommand("see").setExecutor(new BlindCommand(this));
        this.getCommand("highscores").setExecutor(new HighscoresCommand(this));
        this.getCommand("chat").setExecutor(new ChatCommand(this));
        this.getCommand("pkroom").setExecutor(new PkRoomCommand(this));
        this.getServer().getPluginManager().registerEvents(new ParkourListener(this), this);
        this.saveDefaultConfig();
        this.connectDatabase();
        this.setupItems();
        this.setupAchievements();
        try {
            this.rebuildHeads();
        } catch (SQLException e) {
            this.getLogger().log(Level.WARNING, "Failed loading effect heads", e);
        }
    }

    @Override
    public void onDisable() {
        if (this.courseDatabase != null) {
            try {
                this.courseDatabase.close();
            } catch (SQLException ignored) {
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
        blindPlayerExempts.clear();
        pendingFavs.clear();

    }

    private void setupAchievements() {
        try {
            PreparedStatement stmt = courseDatabase.prepareStatement("SELECT * FROM achievements");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String[] opt = rs.getString("options").split(",");
                System.out.println(rs.getString("options"));
                List<Integer> opts = new ArrayList<>();
                for (String s : opt) {
                    opts.add(Integer.parseInt(s));
                }
                achievements.add(new ParkourAchievement(rs.getInt("id"), rs.getString("name"), AchievementCriteria.valueOf(rs.getString("criteria")), AchievementType.valueOf(rs.getString("type")), opts.toArray(new Integer[opts.size()])));
            }
            rs.close();

            stmt = courseDatabase.prepareStatement("SELECT * FROM milestones");
            rs = stmt.executeQuery();
            List<ParkourAchievement> achList = new ArrayList<>();
            while (rs.next()) {
                for (String s : rs.getString("options").split(",")) {
                    if (s.equals("")) {
                        continue;
                    }
                    achList.add(achievements.get(Integer.parseInt(s)));
                }
                milestones.add(new AchievementMilestone(rs.getInt("id"), rs.getString("name"), rs.getString("desc"), (ParkourAchievement[]) achList.toArray()));
                achList.clear();
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Parkour.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupItems() {
        ItemMeta meta = VISION.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.vision"));
        VISION.setItemMeta(meta);

        meta = FIREWORK_SPAWNER.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.firework"));
        FIREWORK_SPAWNER.setItemMeta(meta);

        meta = CHAT.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.chat"));
        CHAT.setItemMeta(meta);

        meta = SPAWN.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.spawn"));
        SPAWN.setItemMeta(meta);

        meta = POINT.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.point"));
        String[] lore = {
            Parkour.getString("item.point.description.0"),
            Parkour.getString("item.point.description.1"),
            Parkour.getString("item.point.description.2")};
        meta.setLore(Arrays.asList(lore));
        POINT.setItemMeta(meta);

        meta = SCOREBOARD.getItemMeta();
        meta.setDisplayName(Parkour.getString("item.scoreboard"));
        SCOREBOARD.setItemMeta(meta);

        meta = NEXT_PAGE.getItemMeta();
        meta.setDisplayName(Parkour.getString("favorites.item.next"));
        NEXT_PAGE.setItemMeta(meta);

        meta = PREV_PAGE.getItemMeta();
        meta.setDisplayName(Parkour.getString("favorites.item.prev"));
        PREV_PAGE.setItemMeta(meta);

        meta = FAVORITES.getItemMeta();
        meta.setDisplayName(Parkour.getString("favorites.item.base"));
        String[] favlore = {
            Parkour.getString("favorites.item.base.lore0")};
        meta.setLore(Arrays.asList(favlore));
        FAVORITES.setItemMeta(meta);

        meta = ACHIEVEMENTS_MENU.getItemMeta();
        meta.setDisplayName(Parkour.getString("achievement.inventory.opener"));
        ACHIEVEMENTS_MENU.setItemMeta(meta);

        HELMET.addEnchantment(Enchantment.DURABILITY, 3);
        CHESTPLATE.addEnchantment(Enchantment.DURABILITY, 3);
        LEGGINGS.addEnchantment(Enchantment.DURABILITY, 3);
        BOOTS.addEnchantment(Enchantment.DURABILITY, 3);
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
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
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (id INTEGER, x REAL, y REAL, z REAL, pitch REAL, yaw REAL, world TEXT, detection INT, mode ENUM('normal', 'guildwar', 'adventure', 'vip', 'hidden') NOT NULL DEFAULT 'normal', difficulty ENUM('easy', 'medium', 'hard', 'veryhard') NOT NULL DEFAULT 'easy')");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (player varchar(16), course INTEGER, time BIGINT, plays INT)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (player varchar(16), xp INTEGER)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds (tag varchar(5), name varchar(32))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS guildplayers (player varchar(16), guild varchar(5), rank enum('default','officer','leader') NOT NULL DEFAULT 'default')");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS adventures (name varchar(32), course INTEGER)");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS courseheads (world_name varchar(32), x INTEGER, y INTEGER, z INTEGER, course_id INTEGER, skull_type_name varchar(32))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS gameresults (time TIMESTAMP, type enum('duel','guildwar'), winner varchar(16), loser varchar(16))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS favorites (`player` varchar(16) NOT NULL,`favorites` text NOT NULL, PRIMARY KEY (`player`))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS achievements (`id` int(11) NOT NULL AUTO_INCREMENT,`name` VARCHAR(20) NOT NULL,`description` mediumtext NOT NULL, `type` enum('BRONZE','SILVER','GOLD','PLATINUM','HIDDEN') NOT NULL,`criteria` enum('PARKOUR_COMPLETE','DUELS_PLAYED','PARKOURS_COMPLETED','TOTAL_PLAYTIME','PLAYS_ON_CERTAIN_PARKOUR','TOTAL_PLAYS_ON_PARKOURS','LEVEL_ACQUIRE','FAVORITES_NUMBER','BEST_SCORE','GUILD_CREATE','GUILD_MEMBERSHIP','BEST_HIGHSCORE','TOP_10','BEAT_PREVIOUS_SCORE') NOT NULL, `options` text NOT NULL, PRIMARY KEY (`id`))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS milestones (`id` int(11) NOT NULL AUTO_INCREMENT,`name` VARCHAR(20) NOT NULL,`options` text NOT NULL, PRIMARY KEY (`id`))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS playerachievements (`player` varchar(16) NOT NULL,`completed` text NOT NULL,`progress` mediumtext NOT NULL,`milestones` text NOT NULL, PRIMARY KEY (`player`))");
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
            return base - experience;
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
        return this.getLevel(exp) >= getLevelRequiredToPlay(diff);
    }

    public int getLevelRequiredToPlay(CourseDifficulty diff) {
        return this.getConfig().getInt("restriction." + diff.name().toLowerCase());
    }

    public boolean teleportToCourse(Player player, int tpParkourId, TeleportCause teleport) {
        try {
            ParkourCourse tpCourse = ParkourCourse.loadCourse(this.getCourseDatabase(), tpParkourId);
            if (tpCourse == null) {
                player.sendMessage(Parkour.getString("error.course404", new Object[]{}));
            } else {

                if (tpCourse.getMode() == CourseMode.HIDDEN && teleport == TeleportCause.COMMAND && !player.hasPermission("parkour.vip")) {
                    player.sendMessage(Parkour.getString("error.course404", new Object[]{}));
                    return false;
                }
                if ((tpCourse.getMode() == CourseMode.VIP || tpCourse.getMode() == CourseMode.ADVENTURE) && !player.hasPermission("parkour.vip")) {
                    player.sendMessage(Parkour.getString("vip.notbought", new Object[]{}));
                    return false;
                }
                PlayerExperience pcd = PlayerExperience.loadExperience(this.getCourseDatabase(), player);
                if (!this.canPlay(pcd.getExperience(), tpCourse.getDifficulty()) && !player.hasPermission("parkour.bypasslevel")) {
                    player.sendMessage(Parkour.getString("xp.insufficient"));
                } else {
                    player.teleport(tpCourse.getTeleport());
                    if (tpCourse.getMode() != CourseMode.ADVENTURE) {
                        player.sendMessage(Parkour.getString("course.teleport", new Object[]{tpCourse.getId()}));
                    }
                    return true;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parkour.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
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

    public boolean isChatEnabled() {
        return chat;
    }

    public void setChat(boolean state) {
        chat = state;
    }

    public boolean containsSimiliarMilestone(List<? extends SimpleMilestone> milestones, SimpleMilestone achieved) {
        for (SimpleMilestone milestone : milestones) {
            if (achieved.isSimiliar(milestone)) {
                return true;
            }
        }
        return false;
    }

    public List<ParkourAchievement> getSimiliarAchievements(SimpleAchievement ach) {
        List<ParkourAchievement> result = new ArrayList<>();
        for (ParkourAchievement achievement : achievements) {
            if (ach.isSimiliar(achievement)) {
                result.add(achievement);
            }
        }
        return result;
    }

    public List<AchievementMilestone> getSimiliarMilestones(SimpleMilestone milestone) {
        List<AchievementMilestone> result = new ArrayList<>();
        for (AchievementMilestone mile : milestones) {
            if (mile.isSimiliar(milestone)) {
                result.add(mile);
            }
        }
        return result;
    }

    public ParkourAchievement getAchievementById(int id) {
        for (ParkourAchievement achievement : achievements) {
            if (achievement.getId() == id) {
                return achievement;
            }
        }
        return null;
    }

    public PlayerAchievements getPlayerAchievements(Player p) {
        PlayerAchievements achs= null;
        if (p.hasMetadata("achievements")) {
            achs = (PlayerAchievements) p.getMetadata("achievements").get(0).value();
        }
        if (achs == null) {
            PlayerAchievements newAchievements = new PlayerAchievements(p, this);
            achs = newAchievements;
            p.setMetadata("achievements", new FixedMetadataValue(this, achs));
        }
        return achs;
    }

    public AchievementMilestone getMilestoneById(int id) {
        for (AchievementMilestone milestone : milestones) {
            if (milestone.getId() == id) {
                return milestone;
            }
        }
        return null;
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
