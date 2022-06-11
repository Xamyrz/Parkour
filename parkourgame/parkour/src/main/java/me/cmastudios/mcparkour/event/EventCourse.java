/*
 * Copyright (C) 2014 Maciej Mionskowski
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

package me.cmastudios.mcparkour.event;

import com.google.gson.internal.LinkedHashTreeMap;
import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EventCourse {
    private EventType type;
    private ParkourCourse course;

    public static EventCourse loadCourse(ParkourCourse course, Connection conn, int id) throws SQLException {
        if (course != null && course.getMode() == ParkourCourse.CourseMode.EVENT) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM events WHERE `id`=?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new EventCourse(EventType.valueOf(rs.getString("type")), course);
                    }
                }
            }
        }
        return null;
    }

    public static EventCourse getRandomCourse(Map<Integer, ParkourCourse> courses, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT FLOOR(RAND() * COUNT(*)) AS `offset` FROM `events`");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int offset = rs.getInt("offset");
                try (PreparedStatement statement = conn.prepareStatement("SELECT * FROM `events` LIMIT ?, 1")) {
                    statement.setInt(1, offset);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            return new EventCourse(EventType.valueOf(result.getString("type")), courses.get(result.getInt("id")));
                        }
                    }
                }
            }
        }
        return null;
    }

    public void save(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (`id`,`type`) VALUES (?,?) ON DUPLICATE KEY UPDATE type=VALUES(type)")) {
            stmt.setInt(1, course.getId());
            stmt.setString(2, type.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public EventCourse(EventType type, ParkourCourse course) {
        this.type = type;
        this.course = course;
    }

    public EventType getType() {
        return type;
    }

    public ParkourCourse getCourse() {
        return course;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public enum EventType {
        TIME_RUSH("time"), PLAYS_RUSH("plays"), DISTANCE_RUSH("distance");

        public final String key;

        private EventType(String key) {
            this.key = key;
        }

        public String getNameKey() {
            return "event." + key + ".title";
        }
    }

}
