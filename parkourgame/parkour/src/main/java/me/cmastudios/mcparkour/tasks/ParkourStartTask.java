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
import me.cmastudios.mcparkour.data.CustomCourse;
import me.cmastudios.mcparkour.event.EventCourse;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.event.PlayerEventRushData;
import me.cmastudios.mcparkour.events.PlayerStartParkourEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;

public class ParkourStartTask implements Runnable {
    private final Sign data;
    private final Player player;
    private final Parkour plugin;
    private final long startTime;

    public ParkourStartTask(Sign data, Player player, Parkour plugin, long startTime) {
        this.data = data;
        this.player = player;
        this.startTime = startTime;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            final ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), Integer.parseInt(data.getLine(1)));
            final IPlayerExperience exp = Parkour.experience.getPlayerExperience(player);
            if(course==null) {
                return;
            }
            final Parkour.PlayResult result = plugin.canPlay(player, exp.getExperience(), course);
            final CustomCourse customCourse = course.getMode() == ParkourCourse.CourseMode.CUSTOM ? CustomCourse.loadCourse(plugin.getCourseDatabase(),course.getId()): null;
            if (result == Parkour.PlayResult.ALLOWED && course.getMode() != ParkourCourse.CourseMode.EVENT) {
                new DisplayHighscoresTask(plugin, player, course).run();
            }
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (course.getMode() == ParkourCourse.CourseMode.EVENT) {
                        if (plugin.getEvent() == null || !plugin.getEvent().hasStarted()) {
                            player.sendMessage(Parkour.getString("event.notrunning"));
                            if (plugin.getEvent() != null && (!plugin.getEvent().hasStarted() || plugin.getEvent().getCourse().getCourse().getId() != course.getId())) {
                                player.teleport(plugin.getEvent().getCourse().getCourse().getTeleport());
                            } else if (plugin.getEvent() == null) {
                                player.teleport(plugin.getSpawn());
                            }
                            return;
                        }
                        EventCourse.EventType type = plugin.getEvent().getCourse().getType();
                        switch (type) {
                            case DISTANCE_RUSH:
                                try {
                                    plugin.playerCourseTracker.put(player, new PlayerEventRushData.PlayerDistanceRushData(course, player, startTime, Integer.parseInt(data.getLine(2))));
                                    plugin.getEvent().showScoreboard(player);
                                } catch (NumberFormatException e) {
                                    return;
                                }
                                break;
                            case PLAYS_RUSH:
                            case TIME_RUSH:
                                try {
                                    plugin.playerCourseTracker.put(player, new PlayerEventRushData(course, player, startTime));
                                    plugin.getEvent().showScoreboard(player);
                                } catch (NumberFormatException e) {
                                    return;
                                }
                                break;

                        }
                    } else {
                        if (result != Parkour.PlayResult.ALLOWED) {
                            player.sendMessage(Parkour.getString(result.key));
                            player.teleport(plugin.getSpawn());
                            return;
                        }
                        Parkour.PlayerCourseData data = new Parkour.PlayerCourseData(course, player, startTime);
                        plugin.playerCourseTracker.put(player, data);
                        if (customCourse!=null) {
                            for(PotionEffect effect : customCourse.getEffects()) {
                                effect.apply(player);
                                player.addPotionEffect(effect);
                            }
                        }
                        Bukkit.getPluginManager().callEvent(new PlayerStartParkourEvent(player, exp, data));
                    }
                }
            });


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
