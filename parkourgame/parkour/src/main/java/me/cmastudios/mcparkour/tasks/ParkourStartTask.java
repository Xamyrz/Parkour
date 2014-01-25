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

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.events.PlayerStartParkourEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;

/**
 * Created by Maciej on 25.01.14.
 */
public class ParkourStartTask extends BukkitRunnable {
    private final int courseId;
    private final Player player;
    private final Parkour plugin;
    private final long startTime;

    public ParkourStartTask(int courseId, Player player, Parkour plugin, long startTime) {
        this.courseId = courseId;
        this.player = player;
        this.startTime = startTime;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            final ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
            final IPlayerExperience exp = Parkour.experience.getPlayerExperience(player);
            final Parkour.PlayResult result = plugin.canPlay(player, exp.getExperience(), course);
            if (result == result.ALLOWED && course.getMode() != ParkourCourse.CourseMode.EVENT) {
                new DisplayHighscoresTask(plugin, player, course).run();
            }
            if (course.getMode() != ParkourCourse.CourseMode.EVENT) {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (result != result.ALLOWED) {
                            player.sendMessage(Parkour.getString(result.key));
                            player.teleport(plugin.getSpawn());
                            return;
                        }
                        Parkour.PlayerCourseData data = new Parkour.PlayerCourseData(course, player, startTime);
                        plugin.playerCourseTracker.put(player, data);
                        Bukkit.getPluginManager().callEvent(new PlayerStartParkourEvent(player, exp, data));
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
