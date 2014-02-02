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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;

import me.cmastudios.mcparkour.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListCoursesCommand implements CommandExecutor {

    private final Parkour plugin;

    public ListCoursesCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2,
            String[] arg3) {
        if(sender instanceof Player) {
            if(!Utils.canUse(plugin, (Player) sender, "pklist", 1)) {
                sender.sendMessage(Parkour.getString("error.cooldown"));
                return true;
            }
        }
        StringBuilder courses = new StringBuilder(
                Parkour.getString("course.list"));
		if (arg3.length >= 1 && arg3[0].toLowerCase().startsWith("adv")) {
			try (PreparedStatement stmt = plugin.getCourseDatabase()
					.prepareStatement("SELECT DISTINCT name FROM adventures")) {
				try (ResultSet result = stmt.executeQuery()) {
					while (result.next()) {
						courses.append(' ').append(result.getString("name"));
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			sender.sendMessage(courses.toString());
			return true;
		}
        try (PreparedStatement stmt = plugin.getCourseDatabase()
                .prepareStatement("SELECT * FROM courses WHERE `mode`!='hidden' ORDER BY id")) {
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    courses.append(' ').append(result.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        sender.sendMessage(courses.toString());
        return true;
    }

}
