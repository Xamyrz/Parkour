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

package me.cmastudios.experience;

import me.cmastudios.experience.events.ChangeExperienceEvent;
import me.cmastudios.experience.tasks.ExperienceSaveTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;

public class PlayerExperience implements IPlayerExperience {

    private final OfflinePlayer player;
    private int experience;
    private long lastUsed;
    private final Experience plugin;

    public PlayerExperience(OfflinePlayer player, int experience, Experience plugin) {
        this.plugin = plugin;
        this.player = player;
        this.experience = experience;
    }

    public void save(boolean async) throws SQLException {
        ExperienceSaveTask task = new ExperienceSaveTask(plugin, player, experience);
        if (async) {
            task.runTaskAsynchronously(plugin);
        } else {
            task.run();
        }
    }

    public int getExperience() {
        this.lastUsed = System.currentTimeMillis();
        return experience;
    }

    public void setExperience(int experience, boolean fireEvent) throws SQLException {
        this.lastUsed = System.currentTimeMillis();
        if (fireEvent) {
            ChangeExperienceEvent event = new ChangeExperienceEvent(this.experience, experience, this);
            Bukkit.getPluginManager().callEvent(event);
            player.getPlayer().sendMessage(Experience.getString("xp.gain", event.getXp() - this.experience, this.experience));
            this.experience = event.getXp();
        } else {
            player.getPlayer().sendMessage(Experience.getString("xp.gain", experience - this.experience, this.experience+(experience-this.experience
            )));
            this.experience = experience;
        }
        if (!player.isOnline()) {
            save(true);  // To be 100% sure that if we change experience while offline the player will get that if log in on other server
        }
    }

    public OfflinePlayer getPlayer() {
        this.lastUsed = System.currentTimeMillis();
        return player;
    }

    public long getLastUsed() {
        return lastUsed;
    }
}