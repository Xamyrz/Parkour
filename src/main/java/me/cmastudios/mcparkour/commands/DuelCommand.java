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
            if (plugin.getDuel(player) != null) {
                sender.sendMessage(Parkour.getString("duel.multiple"));
                return true;
            }
            try {
                int courseId = Integer.parseInt(args[1]);
                ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
                if (course == null) {
                    sender.sendMessage(Parkour.getString("error.course404"));
                }
                int bounty = Integer.parseInt(args[2]);
                int minBounty = plugin.getConfig().getInt("duel.bounty.minimum");
                int maxBounty = plugin.getConfig().getInt("duel.bounty.maximum");
                if (bounty < minBounty || bounty > maxBounty) {
                    sender.sendMessage(Parkour.getString("duel.bounty.range", minBounty, maxBounty));
                    return true;
                }
                PlayerExperience selfXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), player);
                PlayerExperience otherXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), player);
                if (selfXp.getExperience() < bounty || otherXp.getExperience() < bounty) {
                    sender.sendMessage(Parkour.getString("duel.bounty.insufficient"));
                    return true;
                }
                Duel duel = new Duel(player, competitor, course, bounty);
                plugin.activeDuels.add(duel);
                duel.startTimeoutTimer(plugin);
                sender.sendMessage(Parkour.getString("duel.sent"));
                competitor.sendMessage(Parkour.getString("duel.notice", player.getName(), bounty));
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
