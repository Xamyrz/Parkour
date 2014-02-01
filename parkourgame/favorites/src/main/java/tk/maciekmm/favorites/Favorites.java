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

package tk.maciekmm.favorites;

import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

public class Favorites extends JavaPlugin {
    private static ResourceBundle messages = ResourceBundle.getBundle("messages");
    public Map<Player, FavoritesList> pendingFavs = new HashMap<>();

    private Parkour parkour;
    @Override
    public void onEnable() {
        Plugin plugin = this.getServer().getPluginManager().getPlugin("Parkour");
        if(plugin!=null&&plugin.isEnabled()&&plugin instanceof Parkour) {
            parkour = (Parkour)plugin;
        } else {
            Bukkit.getLogger().log(Level.SEVERE,"Cannot hook into parkour plugin, exiting.");
            this.getServer().getPluginManager().disablePlugin(this);
        }
        try {
            parkour.getCourseDatabase().prepareStatement("CREATE TABLE IF NOT EXISTS favorites (`player` varchar(16) NOT NULL,`favorites` text NOT NULL, PRIMARY KEY (`player`))").execute();
        } catch (SQLException e) {
            this.getLogger().log(Level.SEVERE, "Failed to create tables", e);
        }
        this.getServer().getPluginManager().registerEvents(new FavoritesListener(this),this);
    }

    @Override
    public void onDisable() {
        for(FavoritesList list : pendingFavs.values()) {
            list.save(false);
        }
        pendingFavs.clear();
        parkour = null;
    }

    public Parkour getParkour() {
        return parkour;
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(messages.getString(key), args).replace("\u00A0", " ");
    }

    public static List<String> getMessageArrayFromPrefix(String prefix) {
        Set<String> keys = messages.keySet();
        TreeSet<String> res = new TreeSet<>();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                res.add(key);
            }
        }
        ArrayList<String> mess = new ArrayList<>();
        for(String key : res) {
            mess.add(Favorites.getString(key));
        }
        return mess;
    }

    public Connection getCourseDatabase() {
        return this.parkour.getCourseDatabase();
    }

    public boolean canUse(Player player, String cooldown, long seconds) {
        if(player.hasMetadata("achs"+cooldown)) {
            for(MetadataValue val : player.getMetadata("achs"+cooldown)) {
                if(val.getOwningPlugin()==this) {
                    if(val.asLong()/1000 <= seconds) {
                        return false;
                    }
                }
            }
        }
        player.setMetadata("achs"+cooldown,new FixedMetadataValue(this,System.currentTimeMillis()));
        return true;
    }
}
