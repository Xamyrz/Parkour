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

package me.cmastudios.experience.tasks;

import me.cmastudios.experience.Experience;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class ExperienceSaveTask extends BukkitRunnable {
    private final int experience;
    private final OfflinePlayer player;
    private final Experience plugin;
    public ExperienceSaveTask(Experience plugin,OfflinePlayer player,int experience) {
        this.plugin = plugin;
        this.player= player;
        this.experience = experience;
    }
    @Override
    public void run() {
        try (PreparedStatement stmt = plugin.getExperienceDatabase().prepareStatement("INSERT INTO experience (xp,player) VALUES (?, ?) ON DUPLICATE KEY UPDATE `xp`=VALUES(`xp`)")) {
            stmt.setInt(1, experience);
            stmt.setString(2, player.getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, String.format("Could not save %s experience", player.getName()));
        }
    }
}
