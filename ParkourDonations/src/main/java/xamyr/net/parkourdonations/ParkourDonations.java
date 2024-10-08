package xamyr.net.parkourdonations;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;
import xamyr.net.parkourdonations.Donations.MapDonation;
import xamyr.net.parkourdonations.commands.ParkourDonation;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public final class ParkourDonations extends JavaPlugin {
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");
    private Connection donationsDatabase;
    public Map<Integer, MapDonation> mapDonations = new HashMap<>();

    public Parkour parkour;
    @Override
    public void onEnable() {
        Plugin plugin = this.getServer().getPluginManager().getPlugin("Parkour");
        if(plugin!=null&&plugin.isEnabled()&&plugin instanceof Parkour) {
            parkour = (Parkour) plugin;
        } else {
            Bukkit.getLogger().log(Level.SEVERE,"Cannot hook into parkour plugin, exiting.");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        this.saveDefaultConfig();
        this.connectDatabase();
        getCommand("ParkourDonate").setExecutor(new ParkourDonation(this));
        Bukkit.getScheduler().runTaskTimer(this, new checkMapExpiry(this), 1L, 20*60).getTaskId();
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

    public String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
    }

    public int getMessageArrayLength(String prefix) {
        Set<String> keys = messages.keySet();
        TreeSet<String> res = new TreeSet<>();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                res.add(key);
            }
        }
        return res.size();
    }

    public static class checkMapExpiry implements Runnable {
        ParkourDonations plugin;
        int autoMessageCounter = 0;
        int autoMessagesLength;

        public checkMapExpiry(ParkourDonations plugin){
            this.plugin = plugin;
            autoMessagesLength = plugin.getMessageArrayLength("auto.message");
        }

        @Override
        public void run() {
            Date date = new Date();
            Timestamp currentTime = new Timestamp(date.getTime());
            List<Integer> toDelete = new ArrayList<>();
            if(plugin.mapDonations.size() > 0) {
                plugin.mapDonations.forEach((key, value) -> {
                    if (currentTime.after(value.getEndTime())) {
                        toDelete.add(key);
                    }
                });
            }

            for(Integer i: toDelete) {
                if (plugin.parkour.courses.get(i) != null) {
                    plugin.parkour.courses.get(i).setMode(ParkourCourse.CourseMode.LOCKED);
                    try {
                        plugin.mapDonations.get(i).expire();
                        plugin.mapDonations.remove(i);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    String pkname = plugin.parkour.courses.get(i).getName();
                    plugin.getServer().broadcastMessage(plugin.getString("map.expired", pkname, i));
                }
            }
            String autoMessageTemp = plugin.getString("auto.message."+autoMessageCounter);
            ComponentBuilder autoMessage = new ComponentBuilder();
            TextComponent link = null;
            for(String word : autoMessageTemp.split(" ")) {
                if (word.endsWith(".net")) {
                    autoMessage.append(" "+word).event(new ClickEvent( ClickEvent.Action.OPEN_URL, "http://" + word.substring(4)));
                    continue;
                }
                autoMessage.append(" "+word);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(autoMessage.create());
            }
            autoMessageCounter++;
            if (autoMessageCounter == autoMessagesLength) autoMessageCounter = 0;
        }

    }
}
