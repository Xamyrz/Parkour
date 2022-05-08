package xamyr.net.platformer;

import org.bukkit.plugin.java.JavaPlugin;
import xamyr.net.platformer.commands.PlatformCreate;
import xamyr.net.platformer.platform.Platform;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class Platformer extends JavaPlugin {
    private Connection platformerDatabase;
    public Map<String, Platform> platforms = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();

        getCommand("platformcreate").setExecutor(new PlatformCreate(this));
        //getCommand("platformmove").setExecutor(new PlatformMoveCommand());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void connectDatabase() {
        try {
            if (platformerDatabase != null && !platformerDatabase.isClosed()) {
                platformerDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            this.platformerDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                            this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.platformerDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS platforms (`id` int NOT NULL AUTO_INCREMENT, `world` text NOT NULL, `block` text NOT NULL,`x` double NOT NULL,`y` double NOT NULL, `z` double NOT NULL, `movenoblocks` double NOT NULL, `wait` double NOT NULL, `speed` double NOT NULL,PRIMARY KEY (`id`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    public Connection getDatabase(){return platformerDatabase;}
}
