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

import me.cmastudios.mcparkour.Checkpoint;
import me.cmastudios.mcparkour.Duel;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCheckpointCommand implements CommandExecutor {
    private final Parkour plugin;

    public SetCheckpointCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player player = (Player)sender;
        if (!plugin.playerCourseTracker.containsKey(player)) {
            sender.sendMessage(Parkour.getString("checkpoint.course404", new Object[]{}));
            return true;
        }
        Duel duel = plugin.getDuel(player);
        if (duel != null && duel.hasStarted()) {
            sender.sendMessage(Parkour.getString("checkpoint.duel"));
            return true;
        }
        ParkourCourse course = plugin.playerCourseTracker.get(player).course;
        Checkpoint cp = plugin.playerCheckpoints.containsKey(player) ? plugin.playerCheckpoints
                .get(player) : new Checkpoint(player, course, player.getLocation());
        if (cp.getCourse() != course) {
            cp.setCourse(course);
            cp.setCount(0);
        }
        cp.setCount(cp.getCount() + 1);
        cp.setLocation(player.getLocation());
        plugin.playerCheckpoints.put(player, cp);
        sender.sendMessage(Parkour.getString("checkpoint.set", new Object[] {
                cp.getCount(), course.getId(), cp.getReducedExp(100) }));
        return true;
    }

}
