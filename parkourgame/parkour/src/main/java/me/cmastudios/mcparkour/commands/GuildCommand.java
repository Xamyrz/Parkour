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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import me.cmastudios.mcparkour.data.Guild;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildRank;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildCommand implements CommandExecutor {

    private final Parkour plugin;
    private static final Map<Player, Guild> invites = new HashMap<>();
    private static final Map<CommandSender, String> confirm = new HashMap<>();
    public GuildCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args.length < 1)
            return false;
        if (!(sender instanceof Player)) {
            if (args.length >= 4 && args[0].equalsIgnoreCase("create")) {
                String createTag = args[1];
                String createName = args[2];
                String leader = args[3];
                if (!StringUtils.isAlphanumeric(createTag) || createTag.length() > 5) {
                    sender.sendMessage(Parkour.getString("guild.create.invalid"));
                    return true;
                }
                try {
                    GuildPlayer createPlayer = GuildPlayer.loadGuildPlayer(
                            plugin.getCourseDatabase(),
                            Bukkit.getOfflinePlayer(leader));
                    if (createPlayer.inGuild()) {
                        sender.sendMessage(Parkour.getString("guild.create.inguild"));
                        return true;
                    }
                    if (Guild.loadGuild(plugin.getCourseDatabase(), createTag) != null) {
                        sender.sendMessage(Parkour.getString("guild.create.exists"));
                        return true;
                    }
                    Guild createGuild = new Guild(createTag, createName);
                    createGuild.save(plugin.getCourseDatabase());
                    createPlayer.setGuild(createGuild);
                    createPlayer.setRank(GuildRank.LEADER);
                    createPlayer.save(plugin.getCourseDatabase());
                    return true;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            sender.sendMessage(Parkour.getString("error.playerreq"));
            return true;
        }
        switch (args[0]) {
        case "create":
            if (!sender.hasPermission("parkour.guild.create")) {
                sender.sendMessage(Parkour.getString("guild.create.noperms"));
                return true;
            }
            if (args.length < 3) {
                return false;
            }
            String createTag = args[1];
            String createName = args[2]; // TODO name with spaces
            if (!StringUtils.isAlphanumeric(createTag)
                    || createTag.length() > 5) {
                sender.sendMessage(Parkour.getString("guild.create.invalid"));
                return true;
            }
            try {
                GuildPlayer createPlayer = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (createPlayer.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.create.inguild"));
                    return true;
                }
                if (Guild.loadGuild(plugin.getCourseDatabase(), createTag) != null) {
                    sender.sendMessage(Parkour.getString("guild.create.exists"));
                    return true;
                }
                Guild createGuild = new Guild(createTag, createName);
                createGuild.save(plugin.getCourseDatabase());
                createPlayer.setGuild(createGuild);
                createPlayer.setRank(GuildRank.LEADER);
                createPlayer.save(plugin.getCourseDatabase());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "invite":
            if (args.length < 2) {
                return false;
            }
            Player invitedPlayer = Bukkit.getPlayer(args[1]);
            if (invitedPlayer == null) {
                sender.sendMessage(Parkour.getString("error.player404"));
                return true;
            }
            if (!invitedPlayer.hasPermission("parkour.guild.join")) {
                sender.sendMessage(Parkour.getString("guild.invite.invtnoperm"));
                return true;
            }
            if (invitedPlayer.getName().equals(sender.getName()))
                return true;
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (!player.getRank().canInvite()) {
                    sender.sendMessage(Parkour
                            .getString("guild.invite.noperms"));
                    return true;
                }
                GuildPlayer inviteGP = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(), invitedPlayer);
                if (inviteGP == null || !invitedPlayer.isOnline()) {
                    sender.sendMessage(Parkour.getString("error.player404"));
                    return true;
                }
                if (inviteGP.inGuild()) {
                    sender.sendMessage(Parkour
                            .getString("guild.invite.inguild"));
                    return true;
                }
                invites.put(invitedPlayer.getPlayer(), player.getGuild());
                invitedPlayer.getPlayer().sendMessage(
                        Parkour.getString("guild.invite.notice", player
                                .getGuild().getTag(), player.getGuild()
                                .getName()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "accept":
            Player acplr = Bukkit.getPlayerExact(sender.getName());
            if(!acplr.hasPermission("guild.join")) {
                sender.sendMessage(Parkour.getString("guild.join.noperms"));
                return true;
            }
            Guild acreq = invites.get(acplr);
            if(args.length<2) {
                sender.sendMessage(Parkour.getString("guild.putname"));
            }
            if (acreq == null) {
                sender.sendMessage(Parkour.getString("guild.invite.404"));
                return true;
            }
            if (!acreq.getTag().equalsIgnoreCase(args[1])) {
                return false;
            }
            try {
                if (!acreq.exists(plugin.getCourseDatabase())) {
                    sender.sendMessage(Parkour.getString("guild.invite.404"));
                    return true;
                }
                GuildPlayer gp = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (gp.inGuild()) {
                    sender.sendMessage(Parkour
                            .getString("guild.create.inguild"));
                    return true;
                }
                invites.remove(acplr);
                gp.setGuild(acreq);
                gp.setRank(GuildRank.DEFAULT);
                gp.save(plugin.getCourseDatabase());
                acreq.broadcast(Parkour.getString("guild.invite.join",
                        sender.getName(), acreq.getTag()), plugin
                        .getCourseDatabase());
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
            break;
        case "decline":
            Player dcplr = Bukkit.getPlayerExact(sender.getName());
            Guild dcreq = invites.get(dcplr);
            if (dcreq == null) {
                sender.sendMessage(Parkour.getString("guild.invite.404"));
                return true;
            }
            try {
                if (!dcreq.exists(plugin.getCourseDatabase())) {
                    sender.sendMessage(Parkour.getString("guild.invite.404"));
                    return true;
                }
                invites.remove(dcplr);
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
            break;
        case "kick":
            if (args.length < 2) {
                return false;
            }
            OfflinePlayer kickPlayer = Bukkit.getOfflinePlayer(args[1]);
            if (kickPlayer.getName().equals(sender.getName())) {
                sender.getServer().dispatchCommand(sender, "guild leave");
                return true;
            }
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (!player.getRank().canKick()) {
                    sender.sendMessage(Parkour.getString("guild.kick.noperms"));
                    return true;
                }
                if (plugin.getWar(player.getGuild()) != null) {
                    sender.sendMessage(Parkour.getString("guild.war.nocmds"));
                    return true;
                }
                GuildPlayer kickGP = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(), kickPlayer);
                if (kickGP == null || !kickGP.inGuild()
                        || !kickGP.getGuild().equals(player.getGuild())) {
                    sender.sendMessage(Parkour.getString("guild.rank.notin"));
                    return true;
                }
                if (kickGP.getRank() == GuildRank.LEADER) {
                    sender.sendMessage(Parkour.getString("guild.kick.noperms"));
                    return true;
                }
                kickGP.delete(plugin.getCourseDatabase());
                if (kickGP.getPlayer().isOnline()) {
                    plugin.guildChat.remove(kickGP.getPlayer().getPlayer());
                }
                kickGP.getGuild().broadcast(
                        Parkour.getString("guild.part", kickGP.getPlayer()
                                .getName(), player.getGuild().getTag()),
                        plugin.getCourseDatabase());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "leave":
            String leaveArgs = StringUtils.join(args);
            String leaveConfirm = confirm.remove(sender);
            if (!leaveArgs.equals(leaveConfirm)) {
                confirm.put(sender, leaveArgs);
                sender.sendMessage(Parkour.getString("guild.confirm"));
                break;
            }
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(((Player) sender).getUniqueId()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (plugin.getWar(player.getGuild()) != null) {
                    sender.sendMessage(Parkour.getString("guild.war.nocmds"));
                    return true;
                }
                Guild oldGuild = player.getGuild();
                GuildRank oldRank = player.getRank();
                List<GuildPlayer> oldPlayers = oldGuild.getPlayers(plugin
                        .getCourseDatabase());
                Utils.broadcast(GuildPlayer.getPlayers(oldPlayers), Parkour
                        .getString("guild.part", sender.getName(),
                                oldGuild.getTag()));
                player.delete(plugin.getCourseDatabase());
                plugin.guildChat.remove(player.getPlayer().getPlayer());
                oldPlayers.remove(player);
                if (oldPlayers.isEmpty()) {
                    oldGuild.delete(plugin.getCourseDatabase());
                    plugin.getLogger().info(
                            "Removing guild " + oldGuild.getTag()
                                    + " due to lack of players.");
                } else {
                    if (oldRank == GuildRank.LEADER) {
                        GuildPlayer newLeader = oldPlayers.get(new Random()
                                .nextInt(oldPlayers.size()));
                        newLeader.setRank(GuildRank.LEADER);
                        newLeader.save(plugin.getCourseDatabase());
                        Utils.broadcast(GuildPlayer.getPlayers(oldPlayers),
                                Parkour.getString("guild.chrank", newLeader
                                        .getPlayer().getName(), newLeader
                                        .getRank()));
                        plugin.getLogger().info(
                                "Auto-promote new leader to guild "
                                        + oldGuild.getTag() + ": "
                                        + newLeader.getPlayer().getName());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "rank":
            if (args.length < 3) {
                return false;
            }
            OfflinePlayer rankPlayer = Bukkit.getOfflinePlayer(args[1]);
            GuildRank rankNew = GuildRank.getRank(args[2]);
            if (rankNew == null) {
                sender.sendMessage(Parkour.getString("guild.rank.rank404"));
                return true;
            }
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (player.getRank() != GuildRank.LEADER) {
                    sender.sendMessage(Parkour.getString("guild.rank.noperms"));
                    return true;
                }
                GuildPlayer rankGP = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(), rankPlayer);
                if (rankGP == null || !rankGP.inGuild()
                        || !rankGP.getGuild().equals(player.getGuild())) {
                    sender.sendMessage(Parkour.getString("guild.rank.notin"));
                    return true;
                }
                rankGP.setRank(rankNew);
                rankGP.save(plugin.getCourseDatabase());
                if (rankGP.getPlayer().isOnline() && plugin.guildChat.containsKey(rankGP.getPlayer().getPlayer())) {
                    plugin.guildChat.put(rankGP.getPlayer().getPlayer(), rankGP);
                }
                rankGP.getGuild().broadcast(
                        Parkour.getString("guild.chrank", rankGP.getPlayer()
                                .getName(), rankGP.getRank()),
                        plugin.getCourseDatabase());
                if (rankNew == GuildRank.LEADER) {
                    player.setRank(GuildRank.OFFICER);
                    player.save(plugin.getCourseDatabase());
                    player.getGuild().broadcast(
                            Parkour.getString("guild.chrank", player
                                    .getPlayer().getName(), player.getRank()),
                            plugin.getCourseDatabase());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "chat":
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (plugin.guildChat.containsKey(sender)) {
                    plugin.guildChat.remove(sender);
                    sender.sendMessage(Parkour.getString("guild.chat.toggle.off"));
                } else {
                    plugin.guildChat.put((Player) sender, player);
                    sender.sendMessage(Parkour.getString("guild.chat.toggle.on"));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "war":
            if (args.length < 2) {
                return false;
            }
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                if (!player.getRank().canDeclareWar()) {
                    sender.sendMessage(Parkour.getString("guild.war.noperms"));
                    return true;
                }
                GuildWar war = plugin.getWar(player.getGuild());
                if (args[1].equalsIgnoreCase("decline")) {
                    if (war == null) {
                        sender.sendMessage(Parkour.getString("guild.war.notin"));
                        return true;
                    } else if (war.isAccepted()) {
                        sender.sendMessage(Parkour.getString("guild.war.nocmds"));
                        return true;
                    }
                    war.broadcast(Parkour.getString("guild.war.decline"));
                    plugin.activeWars.remove(war);
                    return true;
                } else if (args[1].equalsIgnoreCase("add") && args.length >= 3) {
                    if (war == null) {
                        sender.sendMessage(Parkour.getString("guild.war.notin"));
                        return true;
                    } else if (war.hasStarted()) {
                        sender.sendMessage(Parkour.getString("guild.war.add.started"));
                        return true;
                    } else if (!war.isAccepted()) {
                        war.setAccepted(true);
                    }
                    Player bukkitTarget = Bukkit.getPlayer(args[2]);
                    if (bukkitTarget == null) {
                        sender.sendMessage(Parkour.getString("error.player404", args[2]));
                        return true;
                    }
                    GuildPlayer target = GuildPlayer.loadGuildPlayer(plugin.getCourseDatabase(), bukkitTarget);
                    if (target == null || !target.inGuild() || !target.getGuild().equals(player.getGuild())) {
                        sender.sendMessage(Parkour.getString("error.player404", args[2]));
                        return true;
                    }
                    try {
                        war.addPlayer(target);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        sender.sendMessage(e.getMessage());
                        return true;
                    }
                    sender.sendMessage(Parkour.getString("guild.war.add.confirm", bukkitTarget.getName()));
                    bukkitTarget.sendMessage(Parkour.getString("guild.war.add.notice"));
                    if (war.hasStarted()) {
                        // Automatically start the war if there are enough players
                        war.initiateWar(plugin);
                    }
                    return true;
                }
                Guild opponent = Guild.loadGuild(plugin.getCourseDatabase(), args[1]);
                if (opponent == null) {
                    sender.sendMessage(Parkour.getString("guild.guild404"));
                    return true;
                }
                if (opponent.equals(player.getGuild())) {
                    // silently fail, no internal conflicts
                    return true;
                }
                GuildWar opponentWar = plugin.getWar(opponent);
                if (war != null && (war == opponentWar)) {
                    // accepting a declaration of war (we only allow mutual wars)
                    war.setAccepted(true);
                    war.addPlayer(player);
                    // War will be initiated when enough players are added
                    sender.sendMessage(Parkour.getString("guild.war.accept"));
                    return true;
                }
                if (war != null) { // Only one war at a time
                    sender.sendMessage(Parkour.getString("guild.war.already.self"));
                    return true;
                }
                if (opponentWar != null) { // opponent is declaring war
                    sender.sendMessage(Parkour.getString("guild.war.already.other"));
                    return true;
                }
                // declaring war on other guild
                if (args.length < 3) return false;
                int courseId = Integer.parseInt(args[2]);
                ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
                if (course == null) {
                    sender.sendMessage(Parkour.getString("error.course404"));
                    return true;
                }
                if (course.getMode() != CourseMode.GUILDWAR) {
                    sender.sendMessage(Parkour.getString("guild.war.invalidmode"));
                    return true;
                }
                if (GuildPlayer.getPlayers(opponent.getPlayers(plugin.getCourseDatabase())).isEmpty()) {
                    sender.sendMessage(Parkour.getString("guild.war.none"));
                    return true;
                }
                war = new GuildWar(player.getGuild(), opponent, course);
                war.startAcceptTimer(plugin);
                war.addPlayer(player);
                plugin.activeWars.add(war);
                opponent.broadcast(Parkour.getString("guild.war.declare",
                        player.getGuild().getTag(), player.getGuild().getName(), courseId),
                        plugin.getCourseDatabase());
                player.getGuild().broadcast(Parkour.getString("guild.war.declare",
                        player.getGuild().getTag(), player.getGuild().getName(), courseId),
                        plugin.getCourseDatabase());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(Parkour.getString("error.invalidint"));
            }
            break;
        case "chname":
            // TODO mutable guild name
            break;
        default:
            return false;
        }
        return true;
    }
}
