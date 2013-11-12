package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import me.cmastudios.mcparkour.Parkour;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Guild {

    private final String tag;
    private String name;

    public Guild(String tag, String name) {
        this.tag = tag;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void broadcast(String message, Connection conn) throws SQLException {
        Parkour.broadcast(GuildPlayer.getPlayers(this.getPlayers(conn)),
                message);
    }

    public List<GuildPlayer> getPlayers(Connection conn) throws SQLException {
        List<GuildPlayer> ret = new ArrayList<GuildPlayer>();
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT player, rank FROM guildplayers WHERE guild = ?")) {
            stmt.setString(1, tag);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(result
                            .getString("player"));
                    GuildRank rank = GuildRank.valueOf(result.getString("rank")
                            .toUpperCase());
                    ret.add(new GuildPlayer(player, this, rank));
                }
            }
        }
        return ret;
    }

    public static Guild loadGuild(Connection conn, String tag)
            throws SQLException {
        Guild ret = null;
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT tag, name FROM guilds WHERE tag = ?")) {
            stmt.setString(1, tag);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new Guild(result.getString("tag"), result.getString("name"));
                }
            }
        }
        return ret;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE guilds SET name = ? WHERE tag = ?";
        } else {
            stmtText = "INSERT INTO guilds (name, tag) VALUES (?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setString(1, name);
            stmt.setString(2, tag);
            stmt.executeUpdate();
        }
    }

    public void delete(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn
                .prepareStatement("DELETE FROM guilds WHERE tag = ?")) {
            stmt.setString(1, tag);
            stmt.executeUpdate();
        }
    }

    public boolean exists(Connection conn) throws SQLException {
        return Guild.loadGuild(conn, tag) != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Guild other = (Guild) obj;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

    public static class GuildPlayer {
        private final String player;
        private Guild guild;
        private GuildRank rank;

        public GuildPlayer(OfflinePlayer player, Guild guild, GuildRank rank) {
            this.player = player.getName();
            this.setGuild(guild);
            this.setRank(rank);
        }

        public OfflinePlayer getPlayer() {
            return Bukkit.getOfflinePlayer(player);
        }

        public Guild getGuild() {
            return guild;
        }

        public void setGuild(Guild guild) {
            this.guild = guild;
        }

        public GuildRank getRank() {
            return rank;
        }

        public void setRank(GuildRank rank) {
            this.rank = rank;
        }

        public boolean inGuild() {
            return guild != null;
        }

        public void save(Connection conn) throws SQLException {
            final String stmtText;
            if (exists(conn)) {
                stmtText = "UPDATE guildplayers SET guild = ?, rank = ? WHERE player = ?";
            } else {
                stmtText = "INSERT INTO guildplayers (guild, rank, player) VALUES (?, ?, ?)";
            }
            try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
                stmt.setString(1, guild.getTag());
                stmt.setString(2, rank.name());
                stmt.setString(3, player);
                stmt.executeUpdate();
            }
        }

        public void delete(Connection conn) throws SQLException {
            try (PreparedStatement stmt = conn
                    .prepareStatement("DELETE FROM guildplayers WHERE player = ?")) {
                stmt.setString(1, player);
                stmt.executeUpdate();
            }
        }

        public boolean exists(Connection conn) throws SQLException {
            return GuildPlayer.loadGuildPlayer(conn, getPlayer()).inGuild();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((player == null) ? 0 : player.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GuildPlayer other = (GuildPlayer) obj;
            if (player == null) {
                if (other.player != null)
                    return false;
            } else if (!player.equals(other.player))
                return false;
            return true;
        }

        public static GuildPlayer loadGuildPlayer(Connection conn,
                OfflinePlayer player) throws SQLException {
            GuildPlayer ret = new GuildPlayer(player, null, null);
            try (PreparedStatement stmt = conn
                    .prepareStatement("SELECT guild, rank FROM guildplayers WHERE player = ?")) {
                stmt.setString(1, player.getName());
                try (ResultSet result = stmt.executeQuery()) {
                    if (result.next()) {
                        Guild guild = Guild.loadGuild(conn,
                                result.getString("guild"));
                        GuildRank rank = GuildRank.valueOf(result.getString(
                                "rank").toUpperCase());
                        ret = new GuildPlayer(Bukkit.getOfflinePlayer(player.getName()), guild, rank);
                    }
                }
            }
            return ret;
        }

        public static List<Player> getPlayers(List<GuildPlayer> guildPlayers) {
            List<Player> ret = new ArrayList<Player>();
            for (GuildPlayer gp : guildPlayers) {
                if (gp.getPlayer().isOnline()) {
                    ret.add(gp.getPlayer().getPlayer());
                }
            }
            return ret;
        }

    }

    public static enum GuildRank {
        DEFAULT, OFFICER, LEADER;
        public boolean canKick() {
            return this == OFFICER || this == LEADER;
        }

        public boolean canInvite() {
            return this == OFFICER || this == LEADER;
        }

        public boolean canDeclareWar() {
            return this == LEADER;
        }

        public String toString() {
            switch (this) {
                case DEFAULT:
                    return Parkour.getString("guild.rank.default");
                case OFFICER:
                    return Parkour.getString("guild.rank.officer");
                case LEADER:
                    return Parkour.getString("guild.rank.leader");
            }
            return null;
        }

        public static GuildRank getRank(String localized) {
            for (GuildRank rank : GuildRank.values()) {
                if (localized.equalsIgnoreCase(rank.name())
                        || rank.toString().equalsIgnoreCase(localized)) {
                    return rank;
                }
            }
            return null;
        }
    }

    public static class GuildWar {
        private static final int MAX_PLAYERS = 2; // Maximum players on each guild's team
        private static final int MAX_PLAYERS_TOTAL = MAX_PLAYERS * 2;
        private final Guild initiator;
        private final Guild competitor;
        private final ParkourCourse course;
        private long startTime; // to base winner time on or whatever idk
        private final List<GuildPlayer> warriors; // players in the war who are still alive
        // players who were in the war and lost connection
        private int offlineInit = 0;
        private int offlineComp = 0;
        private GuildPlayer offlineInitPlayer;
        private GuildPlayer offlineCompPlayer;
        // total time
        private long timeInit = 0;
        private long timeComp = 0;
        // longest time
        private long longInit = 0;
        private long longComp = 0;
        private boolean accepted;
        private BukkitTask xp;
 
        public GuildWar(Guild initiator, Guild competitor, ParkourCourse course) {
            this.initiator = initiator;
            this.competitor = competitor;
            this.course = course;
            this.warriors = new ArrayList<GuildPlayer>();
        }

        /**
         * Add a player to participate in the guild war.
         *
         * @param player Player to add.
         * @throws IllegalArgumentException Player is already a warrior.
         * @throws IllegalStateException Too many players signed up for the guild already.
         */
        public void addPlayer(GuildPlayer player) {
            synchronized (warriors) { // Chat is async
                if (warriors.contains(player))  {
                    throw new IllegalArgumentException(Parkour.getString("guild.war.add.already"));
                }
                if (this.getWarriors(player.getGuild()).size() >= MAX_PLAYERS) {
                    throw new IllegalStateException(Parkour.getString("guild.war.add.toomuch"));
                }
            }
        }

        public void initiateWar(Parkour plugin) throws SQLException {
            if (warriors.size() < MAX_PLAYERS_TOTAL) { // add subroutine prevents more than 5 players per team
                throw new IllegalStateException(Parkour.getString("guild.war.insufficient"));
            }
            if (warriors.size() > MAX_PLAYERS_TOTAL) {
                throw new AssertionError("More than " + MAX_PLAYERS_TOTAL + " players");
            }
            this.accepted = true;
            this.startTime = System.currentTimeMillis();
            for (GuildPlayer warrior : warriors) {
                Player player = warrior.getPlayer().getPlayer();
                if (plugin.playerCourseTracker.containsKey(player)) {
                    plugin.playerCourseTracker.remove(player).leave(player);
                }
                player.teleport(course.getTeleport(), TeleportCause.COMMAND);
                player.sendMessage(Parkour.getString("guild.war.start"));
            }
            xp = plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(this), 1L, 1L);
            this.startTimeoutTimer(plugin);
        }

        public void handleDisconnect(GuildPlayer player, Parkour plugin) {
            if (!warriors.contains(player)) return;
            warriors.remove(player);
            if (player.getGuild().equals(initiator)) {
                offlineInit++;
                offlineInitPlayer = player;
            }
            else if (player.getGuild().equals(competitor)) {
                offlineComp++;
                offlineCompPlayer = player;
            }
            if (offlineInit >= 2) win(competitor, plugin);
            else if (offlineComp >= 2) win(initiator, plugin);
            else {
                try {
                    player.getGuild().broadcast(Parkour.getString("guild.war.leave",
                            player.getPlayer().getName()), plugin.getCourseDatabase());
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "broadcast", e);
                }
            }
        }

        public void handleRejoin(Player player, Parkour plugin) throws SQLException {
            OfflinePlayer oplr = Bukkit.getOfflinePlayer(player.getName());
            GuildPlayer gp = GuildPlayer.loadGuildPlayer(plugin.getCourseDatabase(), oplr);
            if (offlineCompPlayer.equals(gp) || offlineInitPlayer.equals(gp)) {
                player.teleport(course.getTeleport(), TeleportCause.PLUGIN);
                if (gp.getGuild().equals(initiator)) {
                    offlineInitPlayer = null;
                    offlineInit--;
                } else if (gp.getGuild().equals(competitor)) {
                    offlineCompPlayer = null;
                    offlineComp--;
                }
                warriors.add(gp);
                gp.getGuild().broadcast(Parkour.getString("guild.war.wb", player.getName()), plugin.getCourseDatabase());
            }
        }

        public void handleFinish(GuildPlayer player, Parkour plugin) {
            warriors.remove(player);
            long time = System.currentTimeMillis() - startTime;
            if (player.getGuild().equals(initiator)) {
                timeInit += time;
                if (longInit < time) {
                    longInit = time;
                }
            } else if (player.getGuild().equals(competitor)) {
                timeComp += time;
                if (longComp < time) {
                    longComp = time;
                }
            }
            if (getWarriors(initiator).size() == 0 && getWarriors(competitor).size() == 0) {
                Guild winner = getWinner();
                win(winner, plugin);
            } else {
                // broadcast to guild this player is done 5 min left
                long rem = (System.currentTimeMillis() - startTime) / 1000 / 60;
                try {
                    player.getGuild().broadcast(Parkour.getString("guild.war.complete",
                            player.getPlayer().getName(), getWarriors(player.getGuild()).size(), rem),
                            plugin.getCourseDatabase());
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "broadcast", e);
                }
            }
        }

        public void win(Guild guild, Parkour plugin) {
            for (GuildPlayer warrior : warriors) {
                Player player = warrior.getPlayer().getPlayer();
                if (plugin.playerCourseTracker.containsKey(player)) {
                    plugin.playerCourseTracker.remove(player).leave(player);
                }
            }
            try {
                initiator.broadcast(Parkour.getString("guild.war.win", guild.getTag(), guild.getName()), plugin.getCourseDatabase());
                competitor.broadcast(Parkour.getString("guild.war.win", guild.getTag(), guild.getName()), plugin.getCourseDatabase());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, null, e);
            }
            xp.cancel();
            plugin.activeWars.remove(this);
        }

        public Guild getWinner() {
            if (offlineInit >= 2) return competitor;
            if (offlineComp >= 2) return initiator;
            // add total player time to the longest time plus 30 seconds if there is a player offline
            long totalTimeInit = timeInit + (offlineInit * (longInit + 30));
            long totalTimeComp = timeComp + (offlineComp * (longComp + 30));
            if (totalTimeInit > totalTimeComp) {
                return competitor; // Competitor has lowest time
            } else if (totalTimeComp > totalTimeInit) {
                return initiator;
            }
            assert false; // Should not tie.
            return initiator;
        }

        public GuildPlayer getPlayer(OfflinePlayer player) {
            for (GuildPlayer gp : warriors) {
                if (gp.getPlayer().getName().equals(player.getName())) {
                    return gp;
                }
            }
            return null;
        }

        public List<GuildPlayer> getWarriors(Guild guild) {
            List<GuildPlayer> guildWarriors = new ArrayList<GuildPlayer>();
            for (GuildPlayer gp : warriors) {
                if (gp.getGuild().equals(guild)) {
                    guildWarriors.add(gp);
                }
            }
            return guildWarriors;
        }

        public void startAcceptTimer(Parkour plugin) {
            plugin.getServer().getScheduler().runTaskLater(plugin, new AcceptTimeoutTimer(this, plugin), 1200L);
        }

        private class AcceptTimeoutTimer extends BukkitRunnable {

            private GuildWar war;
            private Parkour plugin;

            public AcceptTimeoutTimer(GuildWar guildWar, Parkour plugin) {
                this.war = guildWar;
                this.plugin = plugin;
            }

            @Override
            public void run() {
                if (war.hasStarted()) {
                    return;
                }
                plugin.activeWars.remove(war);
                try {
                    war.initiator.broadcast(Parkour.getString("guild.war.timeout"), plugin.getCourseDatabase());
                    war.competitor.broadcast(Parkour.getString("guild.war.timeout"), plugin.getCourseDatabase());
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "broadcast", e);
                }
            }
        }
 
        public static final long WAR_TIMEOUT = 12000L;
        public void startTimeoutTimer(Parkour plugin) {
            plugin.getServer().getScheduler().runTaskLater(plugin, new WarTimeoutTimer(this, plugin), WAR_TIMEOUT);
        }

        private class WarTimeoutTimer extends BukkitRunnable {

            private GuildWar war;
            private Parkour plugin;

            public WarTimeoutTimer(GuildWar guildWar, Parkour plugin) {
                this.war = guildWar;
                this.plugin = plugin;
            }

            @Override
            public void run() {
                war.broadcast(Parkour.getString("guild.war.overtime"));
                war.win(war.getWinner(), plugin);
            }
        }

        private class XpCounterTask extends BukkitRunnable {

            private final GuildWar war;

            public XpCounterTask(GuildWar war) {
                this.war = war;
            }

            @Override
            public void run() {
                for (GuildPlayer gp : war.getWarriors()) {
                    if (!gp.getPlayer().isOnline()) continue;
                    Player player = gp.getPlayer().getPlayer();
                    int secondsPassed = (int) ((System.currentTimeMillis() - war.startTime) / 1000);
                    float remainder = (int) ((System.currentTimeMillis() - war.startTime) % 1000);
                    float tenthsPassed = remainder / 1000F;
                    player.setLevel(secondsPassed);
                    player.setExp(tenthsPassed);
                }
            }
        }

        public void broadcast(String message) {
            for (GuildPlayer warrior : warriors) {
                warrior.getPlayer().getPlayer().sendMessage(message);
            }
        }

        public Guild getInitiator() {
            return initiator;
        }

        public Guild getCompetitor() {
            return competitor;
        }

        public ParkourCourse getCourse() {
            return course;
        }

        public long getStartTime() {
            return startTime;
        }

        public List<GuildPlayer> getWarriors() {
            return warriors;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public void setAccepted(boolean accepted) {
            this.accepted = accepted;
        }

        public boolean hasStarted() {
            return this.isAccepted() && this.getWarriors().size() >= 10;
        }
    }
}
