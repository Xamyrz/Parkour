package me.cmastudios.experience;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Utils {
    public static OfflinePlayer getPlayerUUID(String playerName, Connection conn) throws SQLException {
        OfflinePlayer player = null;
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT `uuid` FROM `players` WHERE `name` = ?")) {
            stmt.setString(1, playerName);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    player = Bukkit.getOfflinePlayer(UUID.fromString(result
                            .getString("uuid")));
                }
            }
        }
        return player;
    }

    public static void savePlayer(Connection conn, Player player) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO `players` (`uuid`, `name`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `name`=VALUES(name)")){
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());

            stmt.executeUpdate();
        }
    }
}
