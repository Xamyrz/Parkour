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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Experience extends JavaPlugin {
    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
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
        this.getServer().getServicesManager().unregisterAll(this);
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
                exp.save(false);
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error occured while saving player experience");
            }
        }

    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (this.playerExperience.containsKey(event.getPlayer())) {
            try {
                this.playerExperience.remove(event.getPlayer()).save(true);
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        OfflinePlayer target;
        if (cmd.getName().equalsIgnoreCase("exp")) {
            if (args.length < 2) {
                return false;
            }
            switch (args[0]) {
                case "set":
                case "add":
                    try {
                        if (args.length < 3) {
                            return false;
                        }
                        target = Bukkit.getOfflinePlayer(args[1]);

                        IPlayerExperience pe = manager.getPlayerExperience(target);
                        int xp;

                        if (args[0].equalsIgnoreCase("set")) {
                            xp = Integer.parseInt(args[2]);
                        } else {
                            xp = pe.getExperience() + Integer.parseInt(args[2]);
                        }
                        pe.setExperience(xp, false);
                        if (target.isOnline()) {
                            target.getPlayer().sendMessage(getString("experience.set.target", xp));
                        }
                        sender.sendMessage(getString("experience.set.success", target.getName(), xp));

                    } catch (NumberFormatException ex) {
                        sender.sendMessage(getString("error.invalidint"));

                    } catch (SQLException ex) {
                        Bukkit.getLogger().log(Level.SEVERE, null, ex);
                    }
                    return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("lvl")) {
            target = Bukkit.getOfflinePlayer(sender.getName());
            if (args.length >= 1) {
                target = Bukkit.getOfflinePlayer(args[0]);
            }
            try {
                IPlayerExperience xp = manager.getPlayerExperience(target);
                int experience = xp.getExperience();
                sender.sendMessage(Experience.getString("xp.has", target.getName(),
                        manager.getLevel(experience), experience, manager.getNextLevelRequiredXp(experience)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
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
