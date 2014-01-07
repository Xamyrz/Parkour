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

package me.cmastudios.experience.events;

import me.cmastudios.experience.ExperienceManager;
import me.cmastudios.experience.IPlayerExperience;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChangeExperienceEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private int xpBefore,xp;
    private IPlayerExperience experience;

    public ChangeExperienceEvent(int xpBefore, int xp, IPlayerExperience experience) {
        this.xpBefore = xpBefore;
        this.xp = xp;
        this.experience = experience;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int getXpBefore() {
        return xpBefore;
    }

    public int getXp() {
        return xp;
    }

    public int getDifference() {
        return xp-xpBefore;
    }

    public IPlayerExperience getPlayerExperience() {
        return experience;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

}
