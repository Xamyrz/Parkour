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
import me.cmastudios.mcparkour.data.PlayerHighScore;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCompleteParkourEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Parkour.PlayerCourseData endData;
    private PlayerHighScore highScore;
    private long completionTime;
    private boolean isPersonalBest;
    private boolean isBest;
    private double xp;
    private boolean isTopTen;

    public PlayerCompleteParkourEvent(Parkour.PlayerCourseData endData, PlayerHighScore highScore, long completionTime, boolean isPersonalBest, boolean isBest, double xp, boolean isTopTen) {
        this.completionTime = completionTime;
        this.highScore = highScore;
        this.isPersonalBest = isPersonalBest;
        this.isBest = isBest;
        this.xp = xp;
        this.isTopTen = isTopTen;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public PlayerHighScore getHighScore() {
        return highScore;
    }

    public boolean isPersonalBest() {
        return isPersonalBest;
    }

    public boolean isBest() {
        return isBest;
    }

    public double getXp() {
        return xp;
    }

    public boolean isTopTen() {
        return isTopTen;
    }

    public Parkour.PlayerCourseData getEndData() {
        return endData;
    }
}