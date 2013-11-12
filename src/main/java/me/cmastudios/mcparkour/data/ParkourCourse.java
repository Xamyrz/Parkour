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
import java.util.List;

import me.cmastudios.mcparkour.Parkour;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Playable parkour course.
 *
 * @author Connor Monahan
 */
public class ParkourCourse {

    private final int id;
    private Location teleport;
    private int detection;
    private CourseMode mode;
    private CourseDifficulty diff;

    public static ParkourCourse loadCourse(Connection conn, int id) throws SQLException {
        ParkourCourse ret = null;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courses WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new ParkourCourse(id, new Location(
                            Bukkit.getWorld(result.getString("world")),
                            result.getDouble("x"), result.getDouble("y"),
                            result.getDouble("z"), result.getFloat("yaw"),
                            result.getFloat("pitch")),
                            result.getInt("detection"),
                            CourseMode.valueOf(result.getString("mode").toUpperCase()),
                            CourseDifficulty.valueOf(result.getString("difficulty").toUpperCase()));
                }
            }
        }
        return ret;
    }

    public ParkourCourse(int id, Location teleport, int detection, CourseMode mode, CourseDifficulty diff) {
        this.id = id;
        this.teleport = teleport;
        this.detection = detection;
        this.mode = mode;
        this.diff = diff;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE courses SET x = ?, y = ?, z = ?, pitch = ?, yaw = ?, world = ?, detection = ?, mode = ?, difficulty = ? WHERE id = ?";
        } else {
            stmtText = "INSERT INTO courses (x, y, z, pitch, yaw, world, detection, mode, difficulty, id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setDouble(1, teleport.getX());
            stmt.setDouble(2, teleport.getY());
            stmt.setDouble(3, teleport.getZ());
            stmt.setFloat(4, teleport.getPitch());
            stmt.setFloat(5, teleport.getYaw());
            stmt.setString(6, teleport.getWorld().getName());
            stmt.setInt(7, detection);
            stmt.setString(8, mode.name());
            stmt.setString(9, diff.name());
            stmt.setInt(10, id);
            stmt.executeUpdate();
        }
    }

    public void delete(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public boolean exists(Connection conn) throws SQLException {
        return ParkourCourse.loadCourse(conn, id) != null;
    }

    public int getId() {
        return id;
    }

    public Location getTeleport() {
        return teleport;
    }

    public void setTeleport(Location teleport) {
        this.teleport = teleport;
    }

    public int getDetection() {
        return detection;
    }

    public void setDetection(int detection) {
        this.detection = detection;
    }

    public CourseMode getMode() {
        return mode;
    }

    public void setMode(CourseMode mode) {
        this.mode = mode;
    }

    public CourseDifficulty getDifficulty() {
        return diff;
    }

    public void setDifficulty(CourseDifficulty diff) {
        this.diff = diff;
    }

    public Scoreboard getScoreboard(List<PlayerHighScore> highScores) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("scores", "dummy");
        obj.setDisplayName(Parkour.getString("scoreboard.title", id));
        for (int count = 0; count < 10; count++) {
            if (highScores.size() <= count) {
                break;
            }
            PlayerHighScore highScore = highScores.get(count);
            String name = highScore.getPlayer().getName();
            if (count == 0) {
                name = ChatColor.YELLOW + name;
            } else if (count == 1) {
                name = ChatColor.GRAY + name;
            } else if (count == 2) {
                name = ChatColor.GOLD + name;
            }
            double score = ((double) highScore.getTime()) / 1000.0D;
            String oplr = Parkour.getString("scoreboard.format", score, name);
            if (oplr.length() > 16) {
                oplr = oplr.substring(0, 16);
            }
            obj.getScore(Bukkit.getOfflinePlayer(oplr)).setScore(-(count + 1));
        }
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return sb;
    }

    public static enum CourseMode {
        NORMAL, GUILDWAR, ADVENTURE, VIP
    }

    public static enum CourseDifficulty {
        EASY, MEDIUM, HARD, VERYHARD
    }
}
