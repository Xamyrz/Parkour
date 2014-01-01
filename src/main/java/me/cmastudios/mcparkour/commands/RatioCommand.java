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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RatioCommand implements CommandExecutor {

    private final Parkour plugin;

    public RatioCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("get"))) {
            sender.sendMessage(Parkour.getString("ratio.get", plugin.getRatio()));
            return true;
        }
        if (args.length == 2 && args[0].equals("set")) {
            try {
                double ratio = Double.valueOf(args[1]);
                plugin.setRatio(ratio);
                sender.sendMessage(Parkour.getString("ratio.set", ratio));
            } catch (NumberFormatException e) {
                sender.sendMessage(Parkour.getString("error.invaliddouble"));
            }
            return true;
        }
        return false;
    }
}
