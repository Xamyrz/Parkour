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

package me.cmastudios.mcparkour.tasks;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Checkpoint;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.PlayerHighScore;
import me.cmastudios.mcparkour.events.PlayerCompleteParkourEventBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class ParkourCompleteTask extends BukkitRunnable {
    private final Player player;
    private final Parkour plugin;
    private final Parkour.PlayerCourseData endData;
    private final long completionTime;
    private final boolean isDuel;
    private final String signExp;

    public ParkourCompleteTask(Player player, Parkour plugin, Parkour.PlayerCourseData data, long time, boolean isDuel, String signExp) {
        this.player = player;
        this.completionTime = time;
        this.signExp = signExp;
        this.endData = data;
        this.isDuel = isDuel;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            PlayerHighScore highScore = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), player, endData.course.getId());
            IPlayerExperience playerXp = Parkour.experience.getPlayerExperience(player);

            final PlayerCompleteParkourEventBuilder eventBuilder = new PlayerCompleteParkourEventBuilder(endData, playerXp, highScore, completionTime);
            if (player.hasPermission("parkour.highscore")&&(!player.isFlying()||(player.isFlying()&&player.hasPermission("parkour.fly.bypass")))) {
                if (highScore.getTime() > completionTime && highScore.getPlays() > 0) {
                    eventBuilder.setPersonalBest(true);
                    player.sendMessage(Parkour.getString("course.end.personalbest", endData.course.getName(), endData.course.getId()));
                }
                if (highScore.getTime() > completionTime || highScore.getTime() == -1) {
                    highScore.setTime(completionTime);
                }
            } else if (highScore.getTime()==Long.MAX_VALUE) {
                highScore.setTime(-1);
            }
            highScore.setPlays(highScore.getPlays() + 1);
            highScore.save(plugin.getCourseDatabase());
            final DecimalFormat df = new DecimalFormat("#.###");
            final double completionTimeSeconds = ((double) completionTime) / 1000;
            player.sendMessage(Parkour.getString("course.end", df.format(completionTimeSeconds)));
            final List<PlayerHighScore> scores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), endData.course.getId(), 10);

            if (player.hasPermission("parkour.highscore")) {
                PlayerHighScore bestScore = scores.get(0);
                for (PlayerHighScore hs : scores) {
                    if (hs.getPlayer().getName().equals(player.getName())) {
                        eventBuilder.setTopTen(true);
                        break;
                    }
                }
                if (highScore.equals(bestScore) && highScore.getTime() == completionTime) {
                    eventBuilder.setBest(true);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getServer().broadcastMessage(Parkour.getString("course.end.best", player.getDisplayName() + ChatColor.RESET, endData.course.getName(), endData.course.getId(), df.format(completionTimeSeconds)));
                        }
                    });
                }
            }
            int courseXp = Integer.parseInt(signExp);
            Checkpoint cp = plugin.playerCheckpoints.get(player);
            if (cp != null && cp.getCourse().getId() == endData.course.getId()) {
                courseXp = cp.getReducedExp(courseXp);
            }
            courseXp = highScore.getReducedXp(courseXp);
            if (isDuel) {
                return;
            }
            courseXp *= (player.hasPermission("parkour.vip") ? plugin.getRatio() > 2 ? plugin.getRatio() : 2 : plugin.getRatio());
            eventBuilder.setReducedXp(courseXp);
            playerXp.setExperience(playerXp.getExperience() + courseXp, true);

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(eventBuilder.getEvent());
                    if (!player.hasMetadata("disableScoreboard")) {
                        player.setScoreboard(endData.course.getScoreboard(scores));
                    }
                }
            });

        } catch (NumberFormatException | IndexOutOfBoundsException | SQLException e) { // No XP gain for this course
        }
    }

}
