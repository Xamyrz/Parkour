package xamyr.net.parkourdonations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xamyr.net.parkourdonations.Donations.MapDonation;
import xamyr.net.parkourdonations.commands.ParkourDonation;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class ParkourDonations extends JavaPlugin {
    private Connection donationsDatabase;
    public Map<Integer, MapDonation> mapDonations = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();
        getCommand("ParkourDonate").setExecutor(new ParkourDonation(this));
        Bukkit.getScheduler().runTaskTimer(this, new checkMapExpiry(this), 20*60, 2L).getTaskId();
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
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS donationhistorypk (`id` INT NOT NULL AUTO_INCREMENT, `player` VARCHAR(25) NOT NULL, `pkid` INT NOT NULL, `date` DATETIME NOT NULL, `minutes` INT NOT NULL, PRIMARY KEY (`id`))");
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS donationpk (`pkid` INT NOT NULL, `start` DATETIME NOT NULL, `end` DATETIME NOT NULL, PRIMARY KEY (`pkid`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load donations database", ex);
        }
    }

    public Connection getDatabase(){
        try {
            if (!donationsDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return donationsDatabase;
    }

    public static class checkMapExpiry implements Runnable {
        ParkourDonations plugin;

        public checkMapExpiry(ParkourDonations plugin){
            this.plugin = plugin;
        }

        @Override
        public void run() {
            Date date = new Date();
            Timestamp currentTime = new Timestamp(date.getTime());
            plugin.mapDonations.forEach((key, value) -> {
                if(currentTime.after(value.getEndTime())) {
                    try {
                        value.expire(plugin.getDatabase());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    plugin.getServer().broadcastMessage("Parkour donation for " + key + " expired");
                    plugin.mapDonations.remove(key);
                }
            });
        }

    }
}
