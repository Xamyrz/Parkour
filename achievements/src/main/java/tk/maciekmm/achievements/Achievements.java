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

package tk.maciekmm.achievements;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import tk.maciekmm.achievements.data.PlayerAchievements;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class Achievements extends JavaPlugin {
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection achievementsDatabase;
    private AchievementsManager manager;
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();
        if (!new File(this.getDataFolder(), "achievements.yml").exists()) {
            this.saveResource("achievements.yml", false); //That's silly it's te second argument is senseless, because when the file exists it gives warning on console that it can't save file -.-
        }
        FileConfiguration achievements = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "achievements.yml"));
        PlayerAchievements.setupCriterias(this.getConfig());
        PlayerAchievements.setupAchievements(achievements);
        Bukkit.getPluginManager().registerEvents(new AchievementsListener(this),this);
        this.manager = new AchievementsManager(this);
        this.getServer().getServicesManager().register(AchievementsManager.class, manager, this, ServicePriority.Normal);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasMetadata("achievements")) {
                p.removeMetadata("achievements", this);
            }
            p.setMetadata("achievements", new FixedMetadataValue(this, PlayerAchievements.loadPlayerAchievements(p.getPlayer(), this)));
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(p.hasMetadata("achievements")) {
                PlayerAchievements achievement = (PlayerAchievements) p.getMetadata("achievements").get(0).value();
                achievement.save();
                p.removeMetadata("achievements",this);
            }
        }
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
    }



    public void connectDatabase() {
        try {
            if (achievementsDatabase != null && !achievementsDatabase.isClosed()) {
                achievementsDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            this.achievementsDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                    this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.achievementsDatabase.createStatement()) {
               initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS playerachievements (`player` varchar(16) NOT NULL,`completed` text NOT NULL,`progress` mediumtext NOT NULL,`milestones` text NOT NULL, PRIMARY KEY (`player`))");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load database driver", ex);
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    // TODO replace with connection pool
    public Connection getAchievementsDatabase() {
        try {
            if (!achievementsDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return achievementsDatabase;
    }

    public AchievementsManager getManager() {
        return manager;
    }
}
