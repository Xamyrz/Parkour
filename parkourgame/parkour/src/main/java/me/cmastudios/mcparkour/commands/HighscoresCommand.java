/*
 * Copyright (C) 2014 Connor Monahan and contributors
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
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HighscoresCommand implements CommandExecutor {

    private Parkour plugin;

    public HighscoresCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq"));
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            if (args.length < 3) {
                return false;
            }
            switch (args[1]) {
                case "course":
                    try {
                        int parkId = Integer.parseInt(args[2]);
                        ParkourCourse pc = plugin.courses.get(parkId);
                        if (pc == null) {
                            sender.sendMessage(Parkour.getString("error.playerorcourse404", args[2]));
                            return true;
                        }

                        PlayerHighScore.resetHighScores(plugin.getCourseDatabase(), parkId, false, args.length > 3 ? Utils.getPlayerUUID(args[3], plugin.getCourseDatabase()) : null);
                        sender.sendMessage(Parkour.getString("highscores.reset.success", parkId));
                        pc.setHighScores(plugin.getCourseDatabase());
                        pc.updateScoreBoard();
                        return true;
                    } catch (SQLException ex) {
                        Logger.getLogger(HighscoresCommand.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(Parkour.getString("error.playerorcourse404", args[2]));
                    }
                    break;
                case "player":
                    try {
                        PlayerHighScore.resetPlayerHighScores(plugin.getCourseDatabase(),args[2]);
                        sender.sendMessage(Parkour.getString("highscores.reset.success", args[2]));
                        plugin.setUpdateHighscores(plugin.getCourseDatabase());
                    } catch (SQLException e) {
                        Logger.getLogger(HighscoresCommand.class.getName()).log(Level.SEVERE, null, e);
                    }
                    return true;
            }


        }
        return false;
    }
}
