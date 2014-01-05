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
    private long time;
    private int plays;

    public static PlayerHighScore loadHighScore(Connection conn, OfflinePlayer player, int course) throws SQLException {
        PlayerHighScore ret = new PlayerHighScore(course, player.getName(), Long.MAX_VALUE, 0);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE player = ? AND course = ?")) {
            stmt.setString(1, player.getName());
            stmt.setInt(2, course);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerHighScore(course, player.getName(), result.getLong("time"), result.getInt("plays"));
                }
            }
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE course = ? AND time>0 ORDER BY time")) {
            stmt.setInt(1, course);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, result.getString("player"), result.getLong("time"), result.getInt("plays")));
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
                    ret.add(new PlayerHighScore(course, result.getString("player"), result.getLong("time"), result.getInt("plays")));
                }
            }
        }
        return ret;
    }

    public PlayerHighScore(int course, String player, long time, int plays) {
        this.course = course;
        this.player = player;
        this.time = time;
        this.plays = plays;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE highscores SET time = ?, plays = ? WHERE player = ? AND course = ?";
        } else {
            stmtText = "INSERT INTO highscores (time, plays, player, course) VALUES (?, ?, ?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setLong(1, time);
            stmt.setInt(2, plays);
            stmt.setString(3, player);
            stmt.setInt(4, course);
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
        return Bukkit.getOfflinePlayer(player);
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
