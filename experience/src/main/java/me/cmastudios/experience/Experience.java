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

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Experience extends JavaPlugin implements Listener {
    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection experienceDatabase;
    private ExperienceManager manager;
    ConcurrentHashMap<UUID, PlayerExperience> playerExperience = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();
        this.manager = new ExperienceManager(this);
        Bukkit.getServer().getPluginManager().registerEvents(this,this);
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
        for (Map.Entry<UUID, PlayerExperience> entry : this.playerExperience.entrySet()) {
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

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(final PlayerJoinEvent event) throws SQLException {
        IPlayerExperience playerExp = manager.getPlayerExperience(event.getPlayer());
        if(playerExp != null){
            if(!playerExp.getPlayerDbName().equals(event.getPlayer().getName()) && this.getConfig().getBoolean("mysql.playerstable")){
                Utils.savePlayer(this.experienceDatabase, event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (this.playerExperience.containsKey(event.getPlayer().getUniqueId())) {
            try {
                this.playerExperience.remove(event.getPlayer().getUniqueId()).save(true);
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
            this.experienceDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                    this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.experienceDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS experience (`uuid` varchar(255), xp INTEGER, PRIMARY KEY (`uuid`))");
                if(this.getConfig().getBoolean("mysql.playerstable"))
                    initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (`uuid` varchar(255) NOT NULL, `name` varchar(16) NOT NULL, PRIMARY KEY (`uuid`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        OfflinePlayer target = null;

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
                        try (PreparedStatement stmt = experienceDatabase.prepareStatement("SELECT uuid from `players` WHERE `name`=?")){
                            stmt.setString(1, args[1]);
                            ResultSet rs = stmt.executeQuery();

                            if(rs.next()){
                                target = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("uuid")));
                            }else{
                                return false;
                            }
                        } catch (SQLException ex) {
                            return false;
                        }

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
            try {
                target = Utils.getPlayerUUID(sender.getName(), getExperienceDatabase());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (args.length >= 1) {
                try {
                    target = Utils.getPlayerUUID(args[0], getExperienceDatabase());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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
