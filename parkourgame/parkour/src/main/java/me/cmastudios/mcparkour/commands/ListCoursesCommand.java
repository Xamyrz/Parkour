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
import org.bukkit.util.ChatPaginator;

public class ListCoursesCommand implements CommandExecutor {

    private final Parkour plugin;

    public ListCoursesCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2,
            String[] args) {
        if(sender instanceof Player) {
            if(!Utils.canUse(plugin, (Player) sender, "pklist", 1)) {
                sender.sendMessage(Parkour.getString("error.cooldown"));
                return true;
            }
        }
        StringBuilder courses = new StringBuilder(
                Parkour.getString("course.list",0,0));
		if (args.length >= 1 && args[0].toLowerCase().startsWith("adv")) {
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

        StringBuilder parkours = new StringBuilder();
        try (PreparedStatement stmt = plugin.getCourseDatabase()
                .prepareStatement("SELECT * FROM courses WHERE `mode`!='hidden' ORDER BY id")) {
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    parkours.append(Parkour.getString("course.list.format", result.getInt("id"), result.getString("name"),result.getString("mode"),result.getString("difficulty"))).append('\n');
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        int pageId = 1;
        if (args.length == 1) {
            pageId = Integer.parseInt(args[0]);
            if (pageId <= 0) {
                pageId = 1;
            }
        }
        ChatPaginator.ChatPage page = ChatPaginator.paginate(parkours.toString(), pageId, ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH, ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 2);
        sender.sendMessage(Parkour.getString("course.list", page.getPageNumber(), page.getTotalPages()));
        sender.sendMessage(page.getLines());
        sender.sendMessage(Parkour.getString("course.list.end",page.getPageNumber(), page.getTotalPages()));
        return true;
    }

}
