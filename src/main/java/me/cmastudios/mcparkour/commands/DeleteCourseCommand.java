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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeleteCourseCommand implements CommandExecutor {

    private final Parkour plugin;

    public DeleteCourseCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2,
            String[] args) {
        if (args.length < 1) {
            return false;
        }
        try {
            int id = Integer.parseInt(args[0]);
            ParkourCourse course = ParkourCourse.loadCourse(
                    plugin.getCourseDatabase(), id);
            if (course != null) {
                course.delete(plugin.getCourseDatabase());
                sender.sendMessage(Parkour.getString("course.delete"));
            } else {
                sender.sendMessage(Parkour.getString("error.course404"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint"));
        }
        return true;
    }

}
