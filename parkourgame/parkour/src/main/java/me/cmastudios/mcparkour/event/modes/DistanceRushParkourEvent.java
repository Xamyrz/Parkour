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

package me.cmastudios.mcparkour.event.modes;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.event.*;
import me.cmastudios.mcparkour.event.configurations.OwnEndingEvent;
import me.cmastudios.mcparkour.event.configurations.ScoreableParkourEvent;
import me.cmastudios.mcparkour.event.configurations.SignConfigurableEvent;
import me.cmastudios.mcparkour.event.configurations.TimerableEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.lang.Runnable;

public class DistanceRushParkourEvent extends ScoreableParkourEvent implements OwnEndingEvent, TimerableEvent, SignConfigurableEvent {

    public DistanceRushParkourEvent(EventCourse course, Parkour plugin, int eventTime) {
        super(course, plugin, eventTime, true);
    }

    @Override
    protected void start() {
        super.start();
        this.startTimerTask();
    }

    @Override
    public void handleEventSign(Player player, Sign sign) {
        if (!plugin.playerCourseTracker.containsKey(player) || !(plugin.playerCourseTracker.get(player) instanceof PlayerEventRushData.PlayerDistanceRushData) || plugin.playerCourseTracker.get(player).course.getId() != this.getCourse().getCourse().getId()) {
            return;
        }
        PlayerEventRushData.PlayerDistanceRushData data = (PlayerEventRushData.PlayerDistanceRushData) plugin.playerCourseTracker.get(player);
        switch (sign.getLine(1)) {
            case "GATE":
                try {
                    if (data.getGate() + 1 != Integer.parseInt(sign.getLine(3))) {
                        return;
                    }
                    data.setGate(Integer.parseInt(sign.getLine(3)));
                    data.addSecs(Integer.parseInt(sign.getLine(2)));
                    int best = getPlayerBestScore(player) != null ? getPlayerBestScore(player).intValue() : 0;
                    if (best <= data.getGate()) {
                        this.setStatistic(player, data.getGate());
                    }
                    this.showScoreboard(player);
                    break;
                } catch (NumberFormatException e) {
                    //Do nothing, sign is configured badly
                }
        }

    }

    @Override
    public void startTimerTask() {
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, new TimerTask(), 1, 1));
    }

    @Override
    public void showScoreboard(Player player) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("scores", "dummy");
        obj.setDisplayName(Parkour.getString("event.scoreboard.title"));
        obj.getScore(Parkour.getString("event.scoreboard.distance.bestgate")).setScore(getPlayerBestScore(player) != null ? getPlayerBestScore(player).intValue() : 0);
        obj.getScore(Parkour.getString("event.scoreboard.position")).setScore(getPlayerPosition(player));
        obj.getScore(Parkour.getString("event.scoreboard.scoredplaces")).setScore(plugin.getConfig().getInt("events." + course.getType().key + ".scoredplaces"));
        obj.getScore(Parkour.getString("event.scoreboard.distance.gate")).setScore((plugin.playerCourseTracker.containsKey(player) && (plugin.playerCourseTracker.get(player) instanceof PlayerEventRushData.PlayerDistanceRushData)) ? ((PlayerEventRushData.PlayerDistanceRushData) plugin.playerCourseTracker.get(player)).getGate() : 0);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(sb);
        Bukkit.getLogger().info("scoreboard.....");
    }

    @Override
    public String getFormatted(Number rank) {
        if (rank != null) {
            return String.valueOf(rank.intValue());
        } else {
            return Parkour.getString("event.formatted.notfinished");
        }
    }

    @Override
    public void handleEnding(Player player, long time, Parkour.PlayerCourseData endData) {
        player.teleport(this.getCourse().getCourse().getTeleport());
        this.showScoreboard(player);
    }

    private class TimerTask implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<Player, Parkour.PlayerCourseData> entry : plugin.playerCourseTracker.entrySet()) {
                if (!(entry.getValue() instanceof PlayerEventRushData.PlayerDistanceRushData)) {
                    continue;
                }
                int secondsPassed = (int) ((System.currentTimeMillis() - entry.getValue().startTime) / 1000);
                if ((((PlayerEventRushData.PlayerDistanceRushData) entry.getValue()).getSecs() - secondsPassed) <= 0) {
                    plugin.playerCourseTracker.remove(entry.getKey());
                    entry.getKey().teleport(course.getCourse().getTeleport());
                    return;
                }
                float remainder = (int) ((System.currentTimeMillis() - entry.getValue().startTime) % 1000);
                float tenthsPassed = remainder / 1000F;
                entry.getKey().setLevel(Math.max(((PlayerEventRushData.PlayerDistanceRushData) entry.getValue()).getSecs() - secondsPassed, 1));
                entry.getKey().setExp(Math.max(1.0F - tenthsPassed, 0.0F));
            }
        }
    }
}


