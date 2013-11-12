/*
 * Copyright (C) 2013 Connor Monahan
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

package me.cmastudios.mcparkour.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.OfflinePlayer;

/**
 * Amount of experience a player has.
 *
 * @author Connor Monahan
 */
public class PlayerExperience {

    private final OfflinePlayer player;
    private int experience;

    public static PlayerExperience loadExperience(Connection conn, OfflinePlayer player) throws SQLException {
        PlayerExperience ret = new PlayerExperience(player, 0);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM experience WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    ret = new PlayerExperience(player, result.getInt("xp"));
                }
            }
        }
        return ret;
    }

    public PlayerExperience(OfflinePlayer player, int experience) {
        this.player = player;
        this.experience = experience;
    }

    public void save(Connection conn) throws SQLException {
        final String stmtText;
        if (exists(conn)) {
            stmtText = "UPDATE experience SET xp = ? WHERE player = ?";
        } else {
            stmtText = "INSERT INTO experience (xp, player) VALUES (?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(stmtText)) {
            stmt.setLong(1, experience);
            stmt.setString(2, player.getName());
            stmt.executeUpdate();
        }
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public boolean exists(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT xp FROM experience WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        }
    }
}
