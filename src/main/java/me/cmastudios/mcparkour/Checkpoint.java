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

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.cmastudios.mcparkour.data.ParkourCourse;

public class Checkpoint {
    private final Player player;
    private int count;
    private ParkourCourse course;
    private Location location;

    public Checkpoint(Player player, ParkourCourse course, Location location) {
        this.player = player;
        this.count = 0;
        this.course = course;
        this.location = location;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ParkourCourse getCourse() {
        return course;
    }

    public void setCourse(ParkourCourse course) {
        this.course = course;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    public int getReducedExp(int original) {
        return original / (count + 1);
    }
}
