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
import java.util.List;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ParkourCommand implements CommandExecutor {

    private final Parkour plugin;

    public ParkourCommand(Parkour instance) {
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
        try {
            int id = Integer.parseInt(args[0]);
            ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), id);
            if (course != null) {
                ((Player)sender).teleport(course.getTeleport());
                sender.sendMessage(Parkour.getString("course.teleport", new Object[] {id}));
                List<PlayerHighScore> highScores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), id);
                ((Player)sender).setScoreboard(course.getScoreboard(highScores));
            } else {
                sender.sendMessage(Parkour.getString("error.course404", new Object[]{}));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint", new Object[]{}));
        }
        return true;
    }
}
