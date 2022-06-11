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
import org.bukkit.scoreboard.Team;

public class PlaysRushParkourEvent extends ScoreableParkourEvent implements OwnEndingEvent {
    private Scoreboard sb;

    public PlaysRushParkourEvent(EventCourse course, Parkour plugin, int eventTime) {
        super(course, plugin, eventTime, true);
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("scores", "dummy", Parkour.getString("event.scoreboard.title"));
        Team team = sb.registerNewTeam("parkourRush");
    }

    @Override
    public void showScoreboard(Player player) {
        Objective obj = sb.getObjective("scores");
        obj.setDisplayName(Parkour.getString("event.scoreboard.title"));
        obj.getScore(Parkour.getString("event.scoreboard.plays.plays")).setScore(getPlayerBestScore(player) != null ? getPlayerBestScore(player).intValue() : 0);
        obj.getScore(Parkour.getString("event.scoreboard.scoredplaces")).setScore(plugin.getConfig().getInt("events." + course.getType().key + ".scoredplaces"));
        obj.getScore(Parkour.getString("event.scoreboard.position")).setScore(getPlayerPosition(player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        Team team = sb.getTeam("parkourRush");
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        team.removeEntry(player.getUniqueId().toString());
        team.addEntry(player.getName());
        player.setScoreboard(sb);
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
        this.setStatistic(player, (this.getPlayerBestScore(player) != null ? this.getPlayerBestScore(player).intValue() : 0) + 1);
    }
}
