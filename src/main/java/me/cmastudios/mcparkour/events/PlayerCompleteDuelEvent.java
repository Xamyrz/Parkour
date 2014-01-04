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

import me.cmastudios.mcparkour.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCompleteDuelEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Duel duel;
    private Player winner;
    private long time;


    public PlayerCompleteDuelEvent(Duel duel,Player winner,long time) {
        this.duel = duel;
        this.winner = winner;
        this.time = time;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Duel getDuel() {
        return duel;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getLoser() {
        return duel.getInitiator()==winner ? winner : duel.getCompetitor();
    }

    public long getTime() {
        return time;
    }

}
