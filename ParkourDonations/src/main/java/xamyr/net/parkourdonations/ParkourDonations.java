package xamyr.net.parkourdonations;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class ParkourDonations extends JavaPlugin {
    private Connection donationsDatabase;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void connectDatabase() {
        try {
            if (donationsDatabase != null && !donationsDatabase.isClosed()) {
                donationsDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            this.donationsDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                            this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.donationsDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS donationpk (`pkid` INT NOT NULL, `start` DATETIME NOT NULL, `end` DATETIME NOT NULL, `wait` int NOT NULL, PRIMARY KEY (`pkid`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }
}
