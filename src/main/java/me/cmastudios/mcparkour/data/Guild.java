package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import me.cmastudios.mcparkour.Parkour;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
                .prepareStatement("SELECT name FROM guilds WHERE tag = ?")) {
            stmt.setString(1, tag);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new Guild(tag, result.getString("name"));
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
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

    public static class GuildPlayer {
        private final OfflinePlayer player;
        private Guild guild;
        private GuildRank rank;

        public GuildPlayer(OfflinePlayer player, Guild guild, GuildRank rank) {
            this.player = player;
            this.setGuild(guild);
            this.setRank(rank);
        }

        public OfflinePlayer getPlayer() {
            return player;
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
                stmt.setString(3, player.getName());
                stmt.executeUpdate();
            }
        }

        public void delete(Connection conn) throws SQLException {
            try (PreparedStatement stmt = conn
                    .prepareStatement("DELETE FROM guildplayers WHERE player = ?")) {
                stmt.setString(1, player.getName());
                stmt.executeUpdate();
            }
        }

        public boolean exists(Connection conn) throws SQLException {
            return GuildPlayer.loadGuildPlayer(conn, player).inGuild();
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
                        ret = new GuildPlayer(player, guild, rank);
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

        public String toString() {
            return Parkour.getString("guild.rank." + this.name().toLowerCase());
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

}
