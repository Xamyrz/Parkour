/*
 * Copyright (C) 2014 Connor Monahan
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
package me.cmastudios.mcparkour.commands;

import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import me.cmastudios.mcparkour.data.AdventureCourse;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;

import me.cmastudios.mcparkour.tasks.TeleportToCourseTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class AdventureCommand implements CommandExecutor {

    private final Parkour plugin;

    public AdventureCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if(!Utils.canUse(plugin, (Player) sender, "adventurecmd", 1)) {
            sender.sendMessage(Parkour.getString("error.cooldown"));
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        String advName = args[0];
        try {
            AdventureCourse course = AdventureCourse.loadAdventure(plugin.getCourseDatabase(), advName);
            if (args.length == 3) {
                switch (args[1]) {
                    case "add":
                        if (sender.hasPermission("parkour.set")) {
                            ParkourCourse chap = ParkourCourse.loadCourse(plugin.getCourseDatabase(), Integer.parseInt(args[2]));
                            if (chap == null) {
                                sender.sendMessage(Parkour.getString("error.course404"));
                            } else {
                                if (course == null) {
                                    course = new AdventureCourse(advName);
                                }
                                course.addCourse(chap);
                                course.save(plugin.getCourseDatabase());
                                int pos = course.getCourses().indexOf(chap) + 1;
                                sender.sendMessage(Parkour.getString("adv.add", chap.getId(), course.getName(), pos));
                            }
                        } else {
                            sender.sendMessage(Parkour.getString("error.permission"));
                        }
                        break;
                }
            } else if (course == null) {
                sender.sendMessage(Parkour.getString("error.course404"));
            } else if (!(sender instanceof Player)) {
                sender.sendMessage(Parkour.getString("error.playerreq"));
            } else if (args.length == 2) {
                int chapter = Integer.parseInt(args[1]);
                ParkourCourse chap = course.getCourses().get(chapter - 1);
                if (chap == null) {
                    sender.sendMessage(Parkour.getString("error.course404"));
                } else if (chapter > 1) {
                    ParkourCourse parent = course.getCourses().get(chapter - 2);
                    PlayerHighScore score = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), (Player) sender, parent.getId());
                    if (score.getTime() == Long.MAX_VALUE) {
                        sender.sendMessage(Parkour.getString("adv.notplayed"));
                    } else {
                        ((Player) sender).teleport(chap.getTeleport());
                        sender.sendMessage(Parkour.getString("adv.tp", chapter, course.getName()));
                    }
                } else {
                    ((Player) sender).teleport(chap.getTeleport());
                    sender.sendMessage(Parkour.getString("adv.tp", chapter, course.getName()));
                }
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new TeleportToCourseTask(plugin,((Player) sender),TeleportCause.COMMAND,course.getCourses().get(0).getId()));
                sender.sendMessage(Parkour.getString("adv.tp", 1, course.getName()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            sender.sendMessage(Parkour.getString("error.invalidint"));
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage(Parkour.getString("adv.chap404"));
        }
        return true;
    }

}
