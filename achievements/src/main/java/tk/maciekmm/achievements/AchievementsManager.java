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

package tk.maciekmm.achievements;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tk.maciekmm.achievements.data.OfflinePlayerAchievements;
import tk.maciekmm.achievements.data.PlayerAchievements;

public class AchievementsManager {
    private final Achievements plugin;

    public AchievementsManager(Achievements plugin) {
        this.plugin = plugin;
    }

    public PlayerAchievements getPlayerAchievements(Player p) {
        if (p.getPlayer().hasMetadata("achievements")) {
            for(MetadataValue value :  p.getPlayer().getMetadata("achievements")) {
                if(value.getOwningPlugin() instanceof Achievements) {
                    return (PlayerAchievements)value.value();
                }
            }
        }
        OfflinePlayerAchievements off = OfflinePlayerAchievements.loadPlayerAchievements(p.getPlayer(), plugin);
        PlayerAchievements achs = new PlayerAchievements(off,plugin,p);
        p.getPlayer().setMetadata("achievements", new FixedMetadataValue(plugin, achs)); //Bug? returning null
        return achs;
    }
    public OfflinePlayerAchievements getOfflinePlayerAchievements(OfflinePlayer player) {
        if(player.isOnline()) {
            return getPlayerAchievements(player.getPlayer());
        } else {
            return OfflinePlayerAchievements.loadPlayerAchievements(player,plugin);
        }
    }
}
