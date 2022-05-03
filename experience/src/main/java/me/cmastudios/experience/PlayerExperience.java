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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;

public class PlayerExperience implements IPlayerExperience {

    private final OfflinePlayer player;
    private int experience;
    private long lastUsed;
    private final Experience plugin;
    private String playerUUID;

    public PlayerExperience(OfflinePlayer player, String playerUUID, int experience, Experience plugin) {
        this.plugin = plugin;
        this.player = player;
        this.experience = experience;
        this.playerUUID = playerUUID;
    }

    public void save(boolean async) throws SQLException {
        ExperienceSaveTask task = new ExperienceSaveTask(plugin, player, experience);
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
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
        if(player.isOnline()) {
            if (fireEvent) {
                ChangeExperienceEvent event = new ChangeExperienceEvent(this.experience, experience, this);
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
                player.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Experience.getString("xp.got", event.getXp() - this.experience)));
                this.experience = event.getXp();
            } else {
                player.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Experience.getString("xp.got", experience - this.experience)));
                this.experience = experience;
            }
        }else{
            this.experience = experience;
            save(true);  // To be 100% sure that if we change experience while offline the player will get that if log in on other server
        }
    }

    public OfflinePlayer getPlayer() {
        this.lastUsed = System.currentTimeMillis();
        return player;
    }

    public String getPlayerDbName(){
        return this.playerUUID;
    }

    public long getLastUsed() {
        return lastUsed;
    }
}