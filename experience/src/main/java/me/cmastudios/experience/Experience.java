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

package me.cmastudios.experience;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Experience extends JavaPlugin {

    private Connection experienceDatabase;
    private ExperienceManager manager;
    ConcurrentHashMap<OfflinePlayer, PlayerExperience> playerExperience = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();
        this.manager = new ExperienceManager(this);
        this.getServer().getServicesManager().register(ExperienceManager.class, manager, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (this.experienceDatabase != null) {
            try {
                this.experienceDatabase.close();
            } catch (SQLException ignored) {
            }
        }
        this.getServer().getScheduler().cancelTasks(this);
        for (Map.Entry<OfflinePlayer, PlayerExperience> entry : this.playerExperience.entrySet()) {
            try {
                PlayerExperience exp = this.playerExperience.remove(entry.getKey());
                exp.save();
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error occured while saving player experience");
            }
        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (this.playerExperience.containsKey(event.getPlayer())) {
            try {
                this.playerExperience.remove(event.getPlayer()).save();
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error occured while saving player experience");
            }
        }
    }

    private void connectDatabase() {
        try {
            if (experienceDatabase != null && !experienceDatabase.isClosed()) {
                experienceDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            this.experienceDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                    this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.experienceDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (player varchar(16), xp INTEGER)");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load database driver", ex);
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    // TODO replace with connection pool
    public Connection getExperienceDatabase() {
        try {
            if (!experienceDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return experienceDatabase;
    }

}
