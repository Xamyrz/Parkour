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

package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * The best score for a player in a specific course.
 *
 * @author Connor Monahan
 */
public class PlayerHighScore {

    private final int course;
    private final String player;
    private final UUID uuid;
    private long time;
    private int plays;

    public static PlayerHighScore loadHighScore(Connection conn, OfflinePlayer player, int course) throws SQLException {
        PlayerHighScore ret = new PlayerHighScore(course, player.getUniqueId(), player.getName(), Long.MAX_VALUE, 0);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE uuid = ? AND player = ? AND course = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.setInt(3, course);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerHighScore(course, player.getUniqueId(), player.getName(), result.getLong("time"), result.getInt("plays"));
                }
            }
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE course = ? AND time>0 ORDER BY time LIMIT 10")) {
            stmt.setInt(1, course);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, UUID.fromString(result.getString("uuid")), result.getString("player"), result.getLong("time"), result.getInt("plays")));
                }
            }
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course, int limit) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE course = ? AND time>0 ORDER BY time LIMIT 0, ?")) {
            stmt.setInt(1, course);
            stmt.setInt(2, limit);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, UUID.fromString(result.getString("uuid")), result.getString("player"), result.getLong("time"), result.getInt("plays")));
                }
            }
        }
        return ret;
    }

    public static void resetHighScores(Connection conn, int course, boolean hard, OfflinePlayer player) throws SQLException {
        StringBuilder qs = new StringBuilder();
        Bukkit.getLogger().info(player.getName() + " hello");
        if (hard) {
            qs.append("DELETE `highscores` WHERE course=?");
        } else {
            qs.append("UPDATE `highscores` SET time=-1 WHERE course=?");
        }
        if (player != null) {
            qs.append(" AND `uuid` LIKE ?");
        }
        try (PreparedStatement stmt = conn.prepareStatement(qs.toString())) {
            stmt.setInt(1, course);
            if (player != null) {
                stmt.setString(2, player.getUniqueId().toString());
            }
            stmt.executeUpdate();
        }
    }

    public static void resetPlayerHighScores(Connection conn, String player) throws SQLException{
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE `highscores` SET time=-1 WHERE `player` LIKE ?")) {
            stmt.setString(1, player);
            stmt.executeUpdate();
        }
    }

    public PlayerHighScore(int course, UUID uuid, String player, long time, int plays) {
        this.course = course;
        this.uuid = uuid;
        this.player = player;
        this.time = time;
        this.plays = plays;
    }

    public void save(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO highscores (time, plays, player, course, uuid) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE time = VALUES(time), plays = VALUES(plays)")) {
            stmt.setLong(1, time);
            stmt.setInt(2, plays);
            stmt.setString(3, player);
            stmt.setInt(4, course);
            stmt.setString(5, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public boolean exists(Connection conn) throws SQLException {
        return PlayerHighScore.loadHighScore(conn, this.getPlayer(), course).time != Long.MAX_VALUE;
    }

    public int getCourse() {
        return course;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getPlays() {
        return plays;
    }

    public void setPlays(int plays) {
        this.plays = plays;
    }

    public int getReducedXp(int startingXp) {
        return (int) (startingXp / Math.min(1.0 + (0.1 * (plays - 1)), 4.0D));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerHighScore that = (PlayerHighScore) o;
        return course == that.course && plays == that.plays && time == that.time && player.equals(that.player);
    }

    @Override
    public int hashCode() {
        int result = course;
        result = 31 * result + player.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + plays;
        return result;
    }
}
