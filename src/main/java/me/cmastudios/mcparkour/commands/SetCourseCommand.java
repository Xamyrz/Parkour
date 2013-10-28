/*
 * Copyright (C) 2013 Connor Monahan
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
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseDifficulty;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCourseCommand implements CommandExecutor {

    private final Parkour plugin;

    public SetCourseCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player player = (Player) sender;
        try {
            int id = Integer.parseInt(args[0]);
            int detection = args.length >= 2 ? Integer.parseInt(args[1]) : 2;
            CourseMode mode = args.length >= 3 ? CourseMode.valueOf(args[2].toUpperCase()) : CourseMode.NORMAL;
            CourseDifficulty diff = args.length >= 4 ? CourseDifficulty.valueOf(args[3].toUpperCase()) : CourseDifficulty.EASY;
            ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), id);
            if (course != null) {
                course.setTeleport(player.getLocation());
                course.setDetection(detection);
                course.setMode(mode);
                course.setDifficulty(diff);
                sender.sendMessage(Parkour.getString("course.updated", id));
            } else {
                course = new ParkourCourse(id, player.getLocation(), detection, mode, diff);
                sender.sendMessage(Parkour.getString("course.created", id));
            }
            course.save(plugin.getCourseDatabase());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint", new Object[]{}));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
