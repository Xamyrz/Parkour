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

import java.util.ArrayList;
import java.util.List;
import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlindCommand implements CommandExecutor {

    private Parkour plugin;

    public BlindCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            return false;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Parkour.getString("error.player404", args[0]));
            return true;
        }
        if (plugin.blindPlayerExempts.containsKey(player)) {
            List<Player> pl = plugin.blindPlayerExempts.get(player);
            if (pl.contains(target)) {
                pl.remove(target);
                plugin.refreshVision(player);
                sender.sendMessage(Parkour.getString("blind.exempts.removed", target.getName()));
                return true;
            } else {
                pl.add(target);
                plugin.refreshVision(player);
                sender.sendMessage(Parkour.getString("blind.exempts.success", target.getName()));
                return true;
            }
        } else {
            List<Player> pl = new ArrayList<>();
            pl.add(target);
            plugin.blindPlayerExempts.put(player, pl);
            plugin.refreshVision(player);
            sender.sendMessage(Parkour.getString("blind.exempts.success", target.getName()));
            return true;
        }

    }
}
