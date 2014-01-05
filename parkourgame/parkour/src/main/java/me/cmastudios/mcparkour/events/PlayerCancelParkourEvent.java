/*
 * Copyright (C) 2013 Maciej Mionskowski
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

package me.cmastudios.mcparkour.events;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.Parkour.PlayerCourseData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCancelParkourEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private CancelReason reason;
    private Parkour.PlayerCourseData courseData;
    private Player player;


    public PlayerCancelParkourEvent(CancelReason reason, PlayerCourseData course, Player player) {
        this.reason = reason;
        this.courseData = course;
        this.player = player;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public CancelReason getReason() {
        return reason;
    }

    public Player getPlayer() {
        return player;
    }

    public ParkourCourse getCourse() {
        return courseData.course;
    }

    public Parkour.PlayerCourseData getData() {
        return courseData;
    }

    public enum CancelReason {
        TELEPORT,
        SIGN,
        STARTING_OTHER,
        BED_ROCK,
        LEAVE
    }
}
