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

import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerExperience implements IPlayerExperience {

    private final OfflinePlayer player;
    private int experience;
    private long lastUsed;
    private final Connection connection;

    public PlayerExperience(OfflinePlayer player, int experience, Connection conn) {
        this.connection = conn;
        this.player = player;
        this.experience = experience;
    }

    public void save() throws SQLException {
        final String stmtText;
        if (exists()) {
            stmtText = "UPDATE experience SET xp = ? WHERE player = ?";
        } else {
            stmtText = "INSERT INTO experience (xp, player) VALUES (?, ?)";
        }
        try (PreparedStatement stmt = connection.prepareStatement(stmtText)) {
            stmt.setLong(1, experience);
            stmt.setString(2, player.getName());
            stmt.executeUpdate();
        }
    }

    public int getExperience() {
        this.lastUsed = System.currentTimeMillis();
        return experience;
    }

    public void setExperience(int experience) throws SQLException {
        this.lastUsed = System.currentTimeMillis();
        this.experience = experience;
        this.save();
    }

    public OfflinePlayer getPlayer() {
        this.lastUsed = System.currentTimeMillis();
        return player;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public boolean exists() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT xp FROM experience WHERE player = ?")) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        }
    }

    public IPlayerExperience getApiInstance() {
        return this;
    }
}