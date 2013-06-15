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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Playable parkour course.
 *
 * @author Connor Monahan
 */
public class ParkourCourse {

    private final int id;
    private Location teleport;

    public static ParkourCourse loadCourse(Connection conn, int id) throws SQLException {
        Location teleport = null;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courses WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    teleport = new Location(Bukkit.getWorld(result.getString("world")), result.getDouble("x"),
                            result.getDouble("y"), result.getDouble("z"), result.getFloat("yaw"), result.getFloat("pitch"));
                }
            }
        }
        if (teleport != null) {
            return new ParkourCourse(id, teleport);
        } else {
            return null;
        }
    }

    public ParkourCourse(int id, Location teleport) {
        this.id = id;
        this.teleport = teleport;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE courses SET x = ?, y = ?, z = ?, pitch = ?, yaw = ?, world = ? WHERE id = ?";
        } else {
            stmtText = "INSERT INTO courses (x, y, z, pitch, yaw, world, id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setDouble(1, teleport.getX());
            stmt.setDouble(2, teleport.getY());
            stmt.setDouble(3, teleport.getZ());
            stmt.setFloat(4, teleport.getPitch());
            stmt.setFloat(5, teleport.getYaw());
            stmt.setString(6, teleport.getWorld().getName());
            stmt.setInt(7, id);
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
}
