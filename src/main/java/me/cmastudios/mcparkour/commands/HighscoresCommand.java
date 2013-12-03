/*
 * Copyright (C) 2013 maciekmm
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
import java.util.logging.Level;
import java.util.logging.Logger;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerExperience;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author maciekmm
 */
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
        Player player = (Player) sender;
        OfflinePlayer target;
        if (args.length < 2) {
            return false;
        }
        switch (args[0]) {
            case "set":
                if (args.length < 3) {
                    return false;
                }
                target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(Parkour.getString("error.player404", args[1]));
                    return true;
                }
                try {
                    int xp = Integer.parseInt(args[2]);
                    PlayerExperience pe = PlayerExperience.loadExperience(plugin.getCourseDatabase(), target);
                    pe.setExperience(xp);
                    pe.save(plugin.getCourseDatabase());
                    player.sendMessage(Parkour.getString("highscores.set.success", target.getName(), args[2]));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Parkour.getString("error.invalidint"));

                } catch (SQLException ex) {
                    Logger.getLogger(HighscoresCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
                return true;
            case "reset":
                target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    if (args.length >= 2) {
                        try {
                            int parkId = Integer.parseInt(args[1]);
                            ParkourCourse pc = ParkourCourse.loadCourse(plugin.getCourseDatabase(), parkId);
                            if (pc == null) {
                                sender.sendMessage(Parkour.getString("error.playerorcourse404", args[1]));
                                return true;
                            }
                            if (args.length > 2) {
                                target = Bukkit.getOfflinePlayer(args[2]);
                                pc.resetScores(plugin.getCourseDatabase(), target);
                            }
                            
                            sender.sendMessage(Parkour.getString("highscores.reset.success", parkId));
                            return true;
                        } catch (SQLException ex) {
                            Logger.getLogger(HighscoresCommand.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(Parkour.getString("error.playerorcourse404", args[1]));
                        }
                    }
                    break;
                } else {
                    try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("DELETE FROM `highscores` WHERE `player`=?")) {
                        stmt.setString(1, target.getName());
                        stmt.executeUpdate();
                        sender.sendMessage(Parkour.getString("highscores.reset.success", target.getName()));
                        return true;
                    } catch (SQLException ex) {
                        Logger.getLogger(HighscoresCommand.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

        }
        return false;
    }
}
