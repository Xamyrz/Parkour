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
import me.cmastudios.mcparkour.event.EventCourse;
import me.cmastudios.mcparkour.event.configurations.OwnEndingEvent;
import me.cmastudios.mcparkour.event.configurations.ScoreableParkourEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.DecimalFormat;

public class TimeRushParkourEvent extends ScoreableParkourEvent implements OwnEndingEvent {

    public TimeRushParkourEvent(EventCourse course, Parkour plugin, int eventTime) {
        super(course, plugin, eventTime, false);
    }

    @Override
    public void showScoreboard(Player player) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("scores", "dummy");
        obj.setDisplayName(Parkour.getString("event.scoreboard.title"));
        obj.getScore(Bukkit.getOfflinePlayer(Parkour.getString("event.scoreboard.hr"))).setScore(-1);
        obj.getScore(Bukkit.getOfflinePlayer(Parkour.getString("event.scoreboard.time.title"))).setScore(-2);
        obj.getScore(Bukkit.getOfflinePlayer(getFormatted(getPlayerBestScore(player)))).setScore(-3);
        obj.getScore(Bukkit.getOfflinePlayer(Parkour.getString("event.scoreboard.scoredplaces"))).setScore(plugin.getConfig().getInt("events." + course.getType().key + ".scoredplaces"));
        obj.getScore(Bukkit.getOfflinePlayer(Parkour.getString("event.scoreboard.position"))).setScore(getPlayerPosition(player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(sb);
    }

    @Override
    public String getFormatted(Number rank) {
        if (rank != null) {
            DecimalFormat df = new DecimalFormat("#.###");
            return df.format(rank.longValue() / 1000.0D);
        } else {
            return Parkour.getString("event.formatted.notfinished");
        }
    }


    @Override
    public void handleEnding(Player player, long time, Parkour.PlayerCourseData endData) {
        long oldTime = this.getPlayerBestScore(player) != null ? this.getPlayerBestScore(player).longValue() : 0;
        if (oldTime > time || oldTime == 0) {
            this.setStatistic(player, time);
        }
        player.teleport(this.getCourse().getCourse().getTeleport());
    }
}
