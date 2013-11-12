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

import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class AdventureCourse {

    private final String name;
    private final List<ParkourCourse> courses;

    public AdventureCourse(String name, List<ParkourCourse> courses) {
        this.name = name;
        this.courses = courses;
    }

    public AdventureCourse(String name) {
        this(name, new ArrayList<ParkourCourse>());
    }

    public String getName() {
        return name;
    }

    public List<ParkourCourse> getCourses() {
        return courses;
    }

    public void addCourse(ParkourCourse course) {
        courses.add(course);
    }

    // TODO Add command to remove courses from adv parkour
    public void removeCourse(ParkourCourse course) {
        courses.remove(course);
    }

    public static AdventureCourse loadAdventure(Connection conn, String name) throws SQLException {
        List<ParkourCourse> courses = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM adventures JOIN courses ON adventures.course = courses.id WHERE adventures.name = ? ORDER BY course")) {
            stmt.setString(1, name);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ParkourCourse currCourse = new ParkourCourse(result.getInt("id"), new Location(
                            Bukkit.getWorld(result.getString("world")),
                            result.getDouble("x"), result.getDouble("y"),
                            result.getDouble("z"), result.getFloat("yaw"),
                            result.getFloat("pitch")),
                            result.getInt("detection"),
                            CourseMode.valueOf(result.getString("mode").toUpperCase()),
                            CourseDifficulty.valueOf(result.getString("difficulty").toUpperCase()));
                    courses.add(currCourse);
                }
            }
        }
        if (courses.isEmpty()) {
            return null;
        }
        return new AdventureCourse(name, courses);
    }

    public static AdventureCourse loadAdventure(Connection conn, ParkourCourse member) throws SQLException {
        String parent = findParent(conn, member.getId());
        if (parent != null && !parent.isEmpty()) {
            return loadAdventure(conn, parent);
        } else {
            return null;
        }
    }

    public static String findParent(Connection conn, int course) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM adventures WHERE course = ? LIMIT 1")) {
            stmt.setInt(1, course);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return result.getString("name");
                }
            }
        }
        return null;
    }

    public void delete(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM adventures WHERE name = ?")) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        }
    }

    public boolean exists(Connection conn) throws SQLException {
        return AdventureCourse.loadAdventure(conn, name) != null;
    }

    public void save(Connection conn) throws SQLException {
        this.delete(conn); // Probably bad practice but should be fine (for now) with small amounts of chapters in an adventure
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO adventures (course, name) VALUES (?, ?)")) {
            for (ParkourCourse course : courses) {
                stmt.setInt(1, course.getId());
                stmt.setString(2, name);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public int getChapter(ParkourCourse member) {
        int chapter = 1;
        for (ParkourCourse course : courses) {
            if (course.getId() == member.getId()) {
                return chapter;
            }
            chapter++;
        }
        return Integer.MAX_VALUE;
    }
}
