package me.cmastudios.mcparkour.commands;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.Guild;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildRank;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildCommand implements CommandExecutor {

    private final Parkour plugin;
    private static final Map<Player, Guild> invites = new HashMap<Player, Guild>();

    public GuildCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args.length < 1)
            return false;
        if (!(sender instanceof Player)) {
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
            String createName = args[2];
            if (!StringUtils.isAlphanumeric(createTag)
                    || createTag.length() > 5) {
                sender.sendMessage(Parkour.getString("guild.create.invalid"));
                return true;
            }
            try {
                GuildPlayer createPlayer = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (createPlayer != null && createPlayer.inGuild()) {
                    sender.sendMessage(Parkour
                            .getString("guild.create.inguild"));
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
            OfflinePlayer invitedPlayer = Bukkit.getPlayer(args[1]);
            if (invitedPlayer == null) {
                sender.sendMessage(Parkour.getString("error.player404"));
                return true;
            }
            if (invitedPlayer.getName() == sender.getName())
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
            Guild acreq = invites.get(Bukkit.getPlayerExact(sender.getName()));
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
                invites.remove(acreq);
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
            Guild dcreq = invites.get(Bukkit.getPlayerExact(sender.getName()));
            if (dcreq == null) {
                sender.sendMessage(Parkour.getString("guild.invite.404"));
                return true;
            }
            try {
                if (!dcreq.exists(plugin.getCourseDatabase())) {
                    sender.sendMessage(Parkour.getString("guild.invite.404"));
                    return true;
                }
                invites.remove(dcreq);
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
            break;
        case "kick":
            if (args.length < 2) {
                return false;
            }
            OfflinePlayer kickPlayer = Bukkit.getOfflinePlayer(args[1]);
            if (kickPlayer.getName() == sender.getName()) {
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
                kickGP.getGuild().broadcast(
                        Parkour.getString("guild.part", kickGP.getPlayer()
                                .getName(), player.getGuild().getTag()),
                        plugin.getCourseDatabase());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "leave":
            try {
                GuildPlayer player = GuildPlayer.loadGuildPlayer(
                        plugin.getCourseDatabase(),
                        Bukkit.getOfflinePlayer(sender.getName()));
                if (player == null || !player.inGuild()) {
                    sender.sendMessage(Parkour.getString("guild.notin"));
                    return true;
                }
                Guild oldGuild = player.getGuild();
                GuildRank oldRank = player.getRank();
                player.delete(plugin.getCourseDatabase());
                List<GuildPlayer> oldPlayers = oldGuild.getPlayers(plugin
                        .getCourseDatabase());
                Parkour.broadcast(GuildPlayer.getPlayers(oldPlayers), Parkour
                        .getString("guild.part", sender.getName(),
                                oldGuild.getTag()));
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
                        Parkour.broadcast(GuildPlayer.getPlayers(oldPlayers),
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
                if (plugin.guildChat.containsKey((Player) sender)) {
                    plugin.guildChat.remove((Player) sender);
                } else {
                    plugin.guildChat.put((Player) sender, player);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            break;
        case "war":
            break;
        default:
            return false;
        }
        return true;
    }
}
