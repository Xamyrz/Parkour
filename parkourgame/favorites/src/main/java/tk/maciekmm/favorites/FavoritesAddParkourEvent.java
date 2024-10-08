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

package tk.maciekmm.favorites;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FavoritesAddParkourEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private FavoritesList favorites;
    private int course;
    private Player player;
    private boolean cancelled;

    public FavoritesAddParkourEvent(FavoritesList favs, int course, Player player) {
        this.player = player;
        this.favorites = favs;
        this.course = course;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    public int getCourseId() {
        return course;
    }

    public Player getPlayer() {
        return player;
    }

    public FavoritesList getFavorites() {
        return favorites;
    }
}
