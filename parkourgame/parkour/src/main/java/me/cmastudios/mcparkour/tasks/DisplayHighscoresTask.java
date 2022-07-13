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
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;
import java.lang.Runnable;

public class DisplayHighscoresTask implements Runnable {
    private final Parkour plugin;
    private final Player player;
    private final ParkourCourse course;

    public DisplayHighscoresTask(Parkour plugin, Player player, ParkourCourse course) {
        this.plugin = plugin;
        this.player = player;
        this.course = course;
    }

    @Override
    public void run() {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                if (!player.hasMetadata("disableScoreboard")) {
                    if (player.getScoreboard().getTeam("parkour") != null) {
                        player.getScoreboard().getTeam("parkour").removeEntry(player.getName());
                    }
                    player.setScoreboard(course.getScoreboard());
                    player.getScoreboard().getTeam("parkour").addEntry(player.getName());
                } else {
                    player.getScoreboard().getTeam("parkour").removeEntry(player.getName());
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        });
    }
}
