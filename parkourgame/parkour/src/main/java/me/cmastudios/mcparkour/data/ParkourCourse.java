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

import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Team;

public class ParkourCourse {

    private final int id;
    private Location teleport;
    private int detection;
    private CourseMode mode;
    private String name;
    private CourseDifficulty diff;

    public static ParkourCourse loadCourse(Connection conn, int id) throws SQLException {
        ParkourCourse ret = null;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courses WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new ParkourCourse(id,
                            result.getString("name"),new Location(
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

    public ParkourCourse(int id, String name, Location teleport, int detection, CourseMode mode, CourseDifficulty diff) {
        this.id = id;
        this.teleport = teleport;
        this.detection = detection;
        this.mode = mode;
        this.diff = diff;
        this.name = name;
    }

    public void save(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO courses (x, y, z, pitch, yaw, world, detection, mode, difficulty, name, id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE x = VALUES(x), y = VALUES(y), z = VALUES(z), pitch = VALUES(pitch), yaw = VALUES(yaw), world = VALUES(world), detection = VALUES(detection), mode = VALUES(mode), difficulty = VALUES(difficulty), name = VALUES(name)")) {
            stmt.setDouble(1, teleport.getX());
            stmt.setDouble(2, teleport.getY());
            stmt.setDouble(3, teleport.getZ());
            stmt.setFloat(4, teleport.getPitch());
            stmt.setFloat(5, teleport.getYaw());
            stmt.setString(6, teleport.getWorld().getName());
            stmt.setInt(7, detection);
            stmt.setString(8, mode.name());
            stmt.setString(9, diff.name());
            stmt.setString(10, name);
            stmt.setInt(11, id);

            stmt.executeUpdate();
        }
    }

    public void clearHeads(Parkour plugin) throws SQLException {
        for (EffectHead head : EffectHead.loadHeads(plugin.getCourseDatabase(), this)) {
            head.getLocation().getBlock().removeMetadata("mcparkour-head", plugin);
        }
        try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement(
                "DELETE FROM courseheads WHERE course_id = ?")) {
            stmt.setInt(1, id);
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

    public String getName() { return name; }

    public void setName(String name) { this.name = name;}

    public Scoreboard getScoreboard(List<PlayerHighScore> highScores) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("scores", "dummy");
        obj.setDisplayName(Parkour.getString("scoreboard.title",name.substring(0,Math.min(name.length(),32-(String.valueOf(id).length()+6))),id));

        for (int count = 0; count < 10; count++) {
            if (highScores.size() <= count) {
                break;
            }

            PlayerHighScore highScore = highScores.get(count);
            String name = highScore.getPlayer().getName();
            Team team = sb.registerNewTeam(name);
            ChatColor color = ChatColor.WHITE;
            if (count == 0) {
                color = ChatColor.YELLOW;
            } else if (count == 1) {
                color = ChatColor.GRAY;
            } else if (count == 2) {
                color = ChatColor.GOLD;
            }

            DecimalFormat df = new DecimalFormat("#.###");

            OfflinePlayer result = Bukkit.getOfflinePlayer(name);
            team.setPrefix(Parkour.getString("scoreboard.prefix", df.format(((double) highScore.getTime()) / 1000.0D),"\u00A7"+color.getChar()));
            team.addPlayer(result);
            obj.getScore(result).setScore(-(count + 1));
        }
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return sb;
    }

    public static enum CourseMode implements ParkourChooseMenu.ParkourChooseCriterium {

        NORMAL, GUILDWAR, ADVENTURE, VIP, HIDDEN, EVENT, CUSTOM, THEMATIC;

        @Override
        public String getType() {
            return "mode";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    public static enum CourseDifficulty implements ParkourChooseMenu.ParkourChooseCriterium {

        EASY, MEDIUM, HARD, VERYHARD;

        @Override
        public String getType() {
            return "difficulty";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }
}
