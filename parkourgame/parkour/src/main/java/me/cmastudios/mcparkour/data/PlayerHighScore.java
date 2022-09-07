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

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * The best score for a player in a specific course.
 *
 * @author Connor Monahan
 */
public class PlayerHighScore {

    private final int course;
    private final UUID uuid;
    private double time;
    private int plays;

    public static PlayerHighScore loadHighScore(Connection conn, OfflinePlayer player, int course) {
        PlayerHighScore ret = new PlayerHighScore(course, player.getUniqueId(), Long.MAX_VALUE, 0);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE uuid = ? AND course = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setInt(2, course);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerHighScore(course, player.getUniqueId(), result.getDouble("time"), result.getInt("plays"));
                }
            }
        }catch(SQLException e){
            Bukkit.getLogger().info(e.getMessage() + " " + e.toString());
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE `course` = ? AND `time`>0 ORDER BY `time`,`timestamp` LIMIT 10")) {
            stmt.setInt(1, course);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, UUID.fromString(result.getString("uuid")), result.getDouble("time"), result.getInt("plays")));
                }
            }
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course, int limit) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE course = ? AND `time`>0 ORDER BY `time`,`timestamp` LIMIT 0, ?")) {
            stmt.setInt(1, course);
            stmt.setInt(2, limit);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, UUID.fromString(result.getString("uuid")), result.getDouble("time"), result.getInt("plays")));
                }
            }
        }
        return ret;
    }

    public static void resetHighScores(Connection conn, int course, boolean hard, OfflinePlayer player) throws SQLException {
        StringBuilder qs = new StringBuilder();
        if (hard) {
            qs.append("DELETE `highscores` WHERE course=?");
        } else {
            qs.append("UPDATE `highscores` SET time=0 WHERE course=?");
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

    public static void resetPlayerHighScores(Connection conn, String playerName) throws SQLException{
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE `highscores` SET time=0 WHERE `uuid` LIKE ?")) {
            stmt.setString(1, Utils.getPlayerUUID(playerName, conn).getUniqueId().toString());
            stmt.executeUpdate();
        }
    }

    public PlayerHighScore(int course, UUID uuid, double time, int plays) {
        this.course = course;
        this.uuid = uuid;
        this.time = time;
        this.plays = plays;
    }

    public void save(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO highscores (time, plays, course, uuid, timestamp) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE time = VALUES(time), plays = VALUES(plays)")) {
            stmt.setDouble(1, time);
            stmt.setInt(2, plays);
            stmt.setInt(3, course);
            stmt.setString(4, uuid.toString());
            stmt.setTimestamp(5, new Timestamp(new Date().getTime()));
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

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public int getPlays() {
        return plays;
    }

    public void setPlays(int plays) {
        this.plays = plays;
    }

    public UUID getPlayerUUID(){ return uuid; }

    public int getReducedXp(int startingXp) {
        return (int) (startingXp / Math.min(1.0 + (0.1 * (plays - 1)), 4.0D));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerHighScore that = (PlayerHighScore) o;
        return course == that.course && plays == that.plays && time == that.time && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        int result = course;
        result = 31 * result + uuid.hashCode();
        result = 31 * result + (int) ((long)time ^ ((long)time >>> 32));
        result = 31 * result + plays;
        return result;
    }
}
