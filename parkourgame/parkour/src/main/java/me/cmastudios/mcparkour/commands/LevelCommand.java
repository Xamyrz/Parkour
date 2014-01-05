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

import java.sql.SQLException;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LevelCommand implements CommandExecutor {

    private final Parkour plugin;

    public LevelCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(sender.getName());
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        }
        try {
            IPlayerExperience xp = Parkour.experience.getPlayerExperience(target);
            int experience = xp.getExperience();
            sender.sendMessage(Parkour.getString("xp.has", target.getName(),
                    Parkour.experience.getLevel(experience), experience, Parkour.experience.getNextLevelRequiredXp(experience)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
