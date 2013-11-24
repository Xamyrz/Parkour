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
import java.util.logging.Level;
import java.util.logging.Logger;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerExperience;

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
        PlayerExperience pcd;
        try {
            pcd = PlayerExperience.loadExperience(plugin.getCourseDatabase(), (Player)sender);
            
            if(plugin.getLevel(pcd.getExperience())<plugin.getConfig().getInt("restriction.levelRequiredToUsePkCommand")) {
                sender.sendMessage(Parkour.getString("xp.insufficient.command", new Object[]{}));
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(ParkourCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            int id = Integer.parseInt(args[0]);
            plugin.teleportToCourse((Player)sender, id,true);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint", new Object[]{}));
        }
        return true;
    }
}
