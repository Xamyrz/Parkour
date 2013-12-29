/*
 * Copyright (C) 2013 Maciej Mionskowski
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

import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PkRoomCommand implements CommandExecutor {

    private final Parkour plugin;

    public PkRoomCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player p = (Player) sender;
        int x, y, z;
        String opt;
        String[] optarray;
        World world;
        float pitch, yaw;
        try {
            if (command.getAliases().contains(label)) {
                x = plugin.getConfig().getInt("pkrooms." + label + ".x");
                y = plugin.getConfig().getInt("pkrooms." + label + ".y");
                z = plugin.getConfig().getInt("pkrooms." + label + ".z");
                pitch = (float) plugin.getConfig().getDouble("pkrooms." + label + ".pitch");
                yaw = (float) plugin.getConfig().getDouble("pkrooms." + label + ".yaw");
                world = Bukkit.getWorld(plugin.getConfig().getString("pkrooms." + label + ".world"));
                if (world == null) {
                    return false;
                }
                Location loc = new Location(world, x, y, z,yaw,pitch);
                p.teleport(loc);
            } else {
                if (args.length < 1) {
                    return false;
                } else {
                    x = plugin.getConfig().getInt("pkrooms." + args[0] + ".x");
                    y = plugin.getConfig().getInt("pkrooms." + args[0] + ".y");
                    z = plugin.getConfig().getInt("pkrooms." + args[0] + ".z");
                    pitch = (float) plugin.getConfig().getDouble("pkrooms." + args[0] + ".pitch");
                    yaw = (float) plugin.getConfig().getDouble("pkrooms." + args[0] + ".yaw");
                    world = Bukkit.getWorld(plugin.getConfig().getString("pkrooms." + args[0] + ".world"));
                    if (world == null) {
                        return false;
                    }
                    Location loc = new Location(world, x, y, z,yaw,pitch);
                    p.teleport(loc);
                }
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint", new Object[]{}));
        }
        return true;
    }

}
