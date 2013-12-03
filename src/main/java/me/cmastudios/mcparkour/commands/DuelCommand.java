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

import me.cmastudios.mcparkour.Duel;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerExperience;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private Parkour plugin;
    public DuelCommand(Parkour plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq", new Object[]{}));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            return false;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                sender.sendMessage(Parkour.getString("duel.bounty.required"));
                return true;
            }
            Duel duel = plugin.getDuel(player);
            if (duel == null || duel.getCompetitor() != player) {
                sender.sendMessage(Parkour.getString("duel.none"));
                return true;
            }
            try {
                int bounty = Integer.parseInt(args[1]);
                if (bounty != duel.getBounty()) {
                    sender.sendMessage(Parkour.getString("duel.bounty.incorrect"));
                    sender.sendMessage(Parkour.getString("duel.bounty.required"));
                    return true;
                }
                duel.setAccepted(true);
                duel.initiateDuel(plugin);
            } catch (NumberFormatException e) {
                sender.sendMessage(Parkour.getString("error.invalidint"));
            }
        } else if (args[0].equalsIgnoreCase("decline")) {
            Duel duel = plugin.getDuel(player);
            if (duel == null || duel.getCompetitor() != player) {
                sender.sendMessage(Parkour.getString("duel.none"));
                return true;
            }
            duel.getInitiator().sendMessage(Parkour.getString("duel.declined"));
            sender.sendMessage(Parkour.getString("duel.declined"));
            plugin.activeDuels.remove(duel);
        } else if (args.length >= 3) {
            Player competitor = plugin.getServer().getPlayer(args[0]);
            if (competitor == null) {
                sender.sendMessage(Parkour.getString("error.player404", args[0]));
                return true;
            }
            if (plugin.playerCourseTracker.containsKey(competitor) || plugin.getDuel(competitor) != null) {
                sender.sendMessage(Parkour.getString("duel.busy"));
                return true;
            }
            if (plugin.getDuel(player) != null || player == competitor) {
                sender.sendMessage(Parkour.getString("duel.multiple"));
                return true;
            }
            try {
                int courseId = Integer.parseInt(args[1]);
                ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
                if (course == null) {
                    sender.sendMessage(Parkour.getString("error.course404"));
                    return true;
                }
                int bounty = Integer.parseInt(args[2]);
                int minBounty = plugin.getConfig().getInt("duel.bounty.minimum");
                int maxBounty = plugin.getConfig().getInt("duel.bounty.maximum");
                if (bounty < minBounty || bounty > maxBounty) {
                    sender.sendMessage(Parkour.getString("duel.bounty.range", minBounty, maxBounty));
                    return true;
                }
                PlayerExperience selfXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), player);
                PlayerExperience otherXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), competitor);
                if (selfXp.getExperience() < bounty || otherXp.getExperience() < bounty) {
                    sender.sendMessage(Parkour.getString("duel.bounty.insufficient"));
                    return true;
                }
                if (!plugin.canDuel(selfXp.getExperience()) || !plugin.canDuel(otherXp.getExperience())) {
                    sender.sendMessage(Parkour.getString("duel.insufficient"));
                    return true;
                }
                if (course.getMode() == ParkourCourse.CourseMode.VIP) {
                    if (!player.hasPermission("parkour.vip") || !competitor.hasPermission("parkour.vip")) {
                        player.sendMessage(Parkour.getString("duel.novip"));
                        return true;
                    }
                }
                if (course.getMode() == ParkourCourse.CourseMode.ADVENTURE
                        || course.getMode() == ParkourCourse.CourseMode.GUILDWAR) {
                    player.sendMessage(Parkour.getString("duel.badcourse"));
                    return true;
                }
                Duel duel = new Duel(player, competitor, course, bounty);
                plugin.activeDuels.add(duel);
                duel.startTimeoutTimer(plugin);
                sender.sendMessage(Parkour.getString("duel.sent"));
                competitor.sendMessage(Parkour.getString("duel.notice", player.getName(), bounty, course.getId()));
            } catch (NumberFormatException e) {
                sender.sendMessage(Parkour.getString("error.invalidint"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
        return true;
    }

}
