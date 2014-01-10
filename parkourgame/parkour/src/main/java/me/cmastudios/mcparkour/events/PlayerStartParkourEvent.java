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

package me.cmastudios.mcparkour.events;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerStartParkourEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Parkour.PlayerCourseData data;
    private IPlayerExperience xp;

    public PlayerStartParkourEvent(Player player, IPlayerExperience xp, Parkour.PlayerCourseData data) {
        this.player = player;
        this.data = data;
        this.xp = xp;

    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public ParkourCourse getCourse() {
        return data.course;
    }

    public IPlayerExperience getXp() {
        return xp;
    }

}
