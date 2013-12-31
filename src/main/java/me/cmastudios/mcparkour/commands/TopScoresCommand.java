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
import java.util.List;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;
import org.bukkit.util.ChatPaginator.ChatPage;

public class TopScoresCommand implements CommandExecutor {

    private final Parkour plugin;

    public TopScoresCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        try {
            int id = Integer.parseInt(args[0]);
            ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), id);
            if (course == null) {
                sender.sendMessage(Parkour.getString("error.course404", new Object[]{}));
                return true;
            }
            if (args.length == 2) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                if (player.hasPlayedBefore()) {
                    PlayerHighScore highScores = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), player, id);
                    if (highScores.getPlays() > 0) {
                        sender.sendMessage(Parkour.getString("topscores.player", player.getName(), id, highScores.getTime()));
                    }
                    return true;
                }
            }
            List<PlayerHighScore> highScores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), id);
            StringBuilder scores = new StringBuilder();
            for (PlayerHighScore highScore : highScores) {
                double completionTimeSeconds = ((double) highScore.getTime()) / 1000;
                int index = highScores.indexOf(highScore) + 1;
                scores.append(Parkour.getString("topscores.format", new Object[]{index, completionTimeSeconds, highScore.getPlayer().getName()})).append('\n');
            }
            int pageId = 1;
            if (args.length == 2) {
                pageId = Integer.parseInt(args[1]);
                if (pageId <= 0) {
                    pageId = 1;
                }
            }
            ChatPage page = ChatPaginator.paginate(scores.toString(), pageId, ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH, ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 2);
            sender.sendMessage(Parkour.getString("topscores.start", new Object[]{id}));
            sender.sendMessage(page.getLines());
            sender.sendMessage(Parkour.getString("topscores.end", new Object[]{page.getPageNumber(), page.getTotalPages()}));

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint", new Object[]{}));
        }

        return true;
    }
}
