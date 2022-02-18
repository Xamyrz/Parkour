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

package me.cmastudios.mcparkour.tasks;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.Guild;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.logging.Level;
import java.lang.Runnable;

public class GuildRejoinHandling implements Runnable {
    private final Player player;
    private final Parkour plugin;

    public GuildRejoinHandling(Player player, Parkour plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        final Guild.GuildPlayer gp;
        try {
            gp = Guild.GuildPlayer.loadGuildPlayer(plugin.getCourseDatabase(), player);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Guild.GuildWar war = plugin.getWar(gp.getGuild());
                    if (war != null) {
                        try {
                            war.handleRejoin(player, plugin);
                        } catch (SQLException e) {
                            Bukkit.getLogger().log(Level.SEVERE,"Could not handle rejoin in guild");
                        }
                    }
                }
            });
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE,"Could not handle rejoin in guild");
        }

    }
}
