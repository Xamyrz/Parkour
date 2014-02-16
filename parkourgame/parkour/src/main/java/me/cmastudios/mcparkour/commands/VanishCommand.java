/*
 * Copyright (C) 2014 Maciej Mionskowski
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {
    private final Parkour plugin;

    public VanishCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player player = (Player)sender;
        if(plugin.vanishedPlayers.contains(player)) {
            plugin.vanishedPlayers.remove(player);
            player.sendMessage(Parkour.getString("vanish.unvanished"));
        } else {
            plugin.vanishedPlayers.add(player);
            player.sendMessage(Parkour.getString("vanish.vanished"));
        }
        plugin.refreshVisionOfOnePlayer(player);
        return true;
    }
}
