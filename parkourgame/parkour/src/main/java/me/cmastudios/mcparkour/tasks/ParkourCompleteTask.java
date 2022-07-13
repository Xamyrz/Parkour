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
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;
import me.cmastudios.mcparkour.events.PlayerCompleteParkourEventBuilder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.swing.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.List;
import java.lang.Runnable;
import java.util.Objects;
import java.util.Properties;

public class ParkourCompleteTask implements Runnable {
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
            Hashtable<String, Boolean> highscores = new Hashtable<>();
            highscores.put("personal", false);
            highscores.put("best", false);

            final PlayerCompleteParkourEventBuilder eventBuilder = new PlayerCompleteParkourEventBuilder(endData, playerXp, highScore, completionTime);
            if (player.hasPermission("parkour.highscore")&&(!player.isFlying()||(player.isFlying()&&player.hasPermission("parkour.fly.bypass")))) {
                if (highScore.getTime() > completionTime && highScore.getPlays() > 0) {
                    eventBuilder.setPersonalBest(true);
                    highscores.put("personal", true);
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
            List<PlayerHighScore> scores = plugin.courses.get(endData.course.getId()).getHighScores();

            if (player.hasPermission("parkour.highscore")) {
                PlayerHighScore bestScore;
                if(scores.size() != 0) {
                    bestScore = scores.get(0);
                    if (scores.size() == 10 && completionTime < scores.get(scores.size()-1).getTime()) {
                        eventBuilder.setTopTen(true);
                        ParkourCourse course = plugin.courses.get(endData.course.getId());
                        course.setHighScores(plugin.getCourseDatabase());
                        course.updateScoreBoard();
                    } else if(completionTime < scores.get(scores.size()-1).getTime()) {
                        eventBuilder.setTopTen(true);
                        ParkourCourse course = plugin.courses.get(endData.course.getId());
                        course.setHighScores(plugin.getCourseDatabase());
                        course.updateScoreBoard();
                    }
                } else {
                    eventBuilder.setTopTen(true);
                    ParkourCourse course = plugin.courses.get(endData.course.getId());
                    course.setHighScores(plugin.getCourseDatabase());
                    course.updateScoreBoard();
                    bestScore = course.getHighScores().get(0);
                }
                if (bestScore.getTime() > completionTime) {
                    eventBuilder.setBest(true);
                    highscores.put("best", true);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getServer().broadcastMessage(Parkour.getString("course.end.best", player.getDisplayName() + ChatColor.RESET, endData.course.getName(), endData.course.getId(), df.format(completionTimeSeconds)));
                        }
                    });
                }
                if(highscores.get("best") || highscores.get("personal")){
                    if(highscores.get("best")){
                        player.sendTitle(Parkour.getString("course.end.bestsimple"),Parkour.getString("course.end.time",df.format(completionTimeSeconds)), 10, 70, 20);
                    }else if(highscores.get("personal")){
                        player.sendTitle(Parkour.getString("course.end.personalbest"),Parkour.getString("course.end.time",df.format(completionTimeSeconds)), 10, 70, 20);
                    }
                }else{
                    player.sendTitle(Parkour.getString("course.end") ,Parkour.getString("course.end.time",df.format(completionTimeSeconds)) + " " + Parkour.getString("course.end.seconds"), 10, 70, 20);
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
                        player.setScoreboard(endData.course.getScoreboard());
                    }
                }
            });

        } catch (NumberFormatException | IndexOutOfBoundsException | SQLException e) { // No XP gain for this course
        }
    }

}
