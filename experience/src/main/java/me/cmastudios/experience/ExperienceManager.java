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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;

public class ExperienceManager {
    private final Experience plugin;


    public ExperienceManager(Experience instance) {
        this.plugin = instance;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<OfflinePlayer, PlayerExperience> entry : plugin.playerExperience.entrySet()) {
                    try {
                        entry.getValue().save();
                    } catch (SQLException e) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error occured while saving player experience");
                    }
                    if (System.currentTimeMillis() - entry.getValue().getLastUsed() > 180000) {
                        plugin.playerExperience.remove(entry.getKey());

                    }
                }
            }
        }, 3000, 3000);
    }

    private PlayerExperience loadExperience(Connection conn, OfflinePlayer player) throws SQLException {
        PlayerExperience ret = new PlayerExperience(player, 0,plugin.getExperienceDatabase());
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM experience WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerExperience(player, result.getInt("xp"),plugin.getExperienceDatabase());
                }
            }
        }
        return ret;
    }

    public IPlayerExperience getPlayerExperience(OfflinePlayer player) throws SQLException {
        if (!plugin.playerExperience.containsKey(player)) {
            PlayerExperience exp = loadExperience(plugin.getExperienceDatabase(), player);
            plugin.playerExperience.put(player, exp);
            return exp;
        } else {
            return plugin.playerExperience.get(player);
        }
    }

    public int getLevel(int experience) {
        int base = plugin.getConfig().getInt("levels.base");
        int addt = plugin.getConfig().getInt("levels.addition");
        if (experience < base) {
            return 1;
        }
        int xplast = 0;
        for (int x = 2; x < Integer.MAX_VALUE; x++) {
            int xpreq = xplast + base + (addt * (x - 2));
            if (experience < xpreq) {
                return x - 1;
            }
            xplast = xpreq;
        }
        return Integer.MAX_VALUE;
    }

    public int getNextLevelRequiredXp(int experience) {
        int base = plugin.getConfig().getInt("levels.base");
        int addt = plugin.getConfig().getInt("levels.addition");
        if (experience < base) {
            return base - experience;
        }
        int xplast = 0;
        for (int x = 2; x < Integer.MAX_VALUE; x++) {
            int xpreq = xplast + base + (addt * (x - 2));
            if (experience < xpreq) {
                return xpreq - experience;
            }
            xplast = xpreq;
        }
        return Integer.MAX_VALUE;
    }

}
