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
package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * The best score for a player in a specific course.
 *
 * @author Connor Monahan
 */
public class PlayerHighScore {

    private final int course;
    private final OfflinePlayer player;
    private long time;

    public static PlayerHighScore loadHighScore(Connection conn, OfflinePlayer player, int course) throws SQLException {
        PlayerHighScore ret = new PlayerHighScore(course, player, Long.MAX_VALUE);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE player = ? AND course = ?")) {
            stmt.setString(1, player.getName());
            stmt.setInt(2, course);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerHighScore(course, player, result.getLong("time"));
                }
            }
        }
        return ret;
    }

    public static List<PlayerHighScore> loadHighScores(Connection conn, int course) throws SQLException {
        List<PlayerHighScore> ret = new ArrayList<PlayerHighScore>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM highscores WHERE course = ? ORDER BY time")) {
            stmt.setInt(1, course);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ret.add(new PlayerHighScore(course, Bukkit.getOfflinePlayer(result.getString("player")), result.getLong("time")));
                }
            }
        }
        return ret;
    }

    public PlayerHighScore(int course, OfflinePlayer player, long time) {
        this.course = course;
        this.player = player;
        this.time = time;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE highscores SET time = ? WHERE player = ? AND course = ?";
        } else {
            stmtText = "INSERT INTO highscores (time, player, course) VALUES (?, ?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setLong(1, time);
            stmt.setString(2, player.getName());
            stmt.setInt(3, course);
            stmt.executeUpdate();
        }
    }

    public boolean exists(Connection conn) throws SQLException {
        return PlayerHighScore.loadHighScore(conn, player, course).time != Long.MAX_VALUE;
    }

    public int getCourse() {
        return course;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PlayerHighScore) {
            return o.hashCode() == this.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + this.course;
        hash = 37 * hash + Objects.hashCode(this.player);
        hash = 37 * hash + (int) (this.time ^ (this.time >>> 32));
        return hash;
    }
}
