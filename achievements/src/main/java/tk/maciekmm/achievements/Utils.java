package tk.maciekmm.achievements;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Utils {
    public static void savePlayer(Connection conn, Player player) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO `players` (`uuid`, `name`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `name`=VALUES(name)")){
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());

            stmt.executeUpdate();
        }
    }
}
