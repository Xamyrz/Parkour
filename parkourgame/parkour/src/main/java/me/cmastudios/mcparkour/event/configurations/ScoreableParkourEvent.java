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

package me.cmastudios.mcparkour.event.configurations;


import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.event.EventCourse;
import me.cmastudios.mcparkour.event.ParkourEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.SQLException;
import java.util.*;

public abstract class ScoreableParkourEvent<N extends Number> extends ParkourEvent {
    protected final TreeMap<N, ArrayList<OfflinePlayer>> bestResults;

    public ScoreableParkourEvent(EventCourse course, Parkour plugin, int eventTime, boolean descending) {
        super(course, plugin, eventTime);
        if (descending) {
            bestResults = new TreeMap<>();
        } else {
            bestResults = new TreeMap<>(Collections.reverseOrder());
        }
    }

    public abstract void showScoreboard(Player player);

    public abstract String getFormatted(N rank);

    @Override
    public void end() {
        super.end();
        int ctrl = 0;
        boolean stop = false;
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective ob = sb.registerNewObjective("scores", "dummy");
        ob.setDisplayName(Parkour.getString("event.scoreboard.scores.title"));
        ob.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (N key : bestResults.descendingMap().keySet()) {
            if (ctrl >= plugin.getConfig().getInt("events." + getKey() + ".scoredplaces")) {
                stop = true;
            }
            Team team = null;
            int reward = 0;
            if (!stop) {
                ctrl++;
                team = sb.registerNewTeam("pos" + ctrl);
                team.setPrefix(Parkour.getString("event.scoreboard.team.prefix", getFormatted(key)));
                reward = (plugin.getConfig().contains("events." + getKey() + ".rewards." + ctrl) ? plugin.getConfig().getInt("events." + getKey() + ".rewards." + ctrl) : 0);
            }
            for (OfflinePlayer player : bestResults.get(key)) {
                if (!stop) {
                    try {
                        Parkour.experience.getPlayerExperience(player).setExperience(Parkour.experience.getPlayerExperience(player).getExperience() + reward, false);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    team.addPlayer(player);
                    ob.getScore(player).setScore(reward);
                }
                if (player.isOnline()) {
                    player.getPlayer().setScoreboard(sb);
                }
            }

        }
        bestResults.clear();
    }

    public abstract String getKey();

    public void setStatistic(OfflinePlayer player, N stat) {
        N best = getPlayerBestScore(player);
        if (best != null) {
            bestResults.get(best).remove(player);
            if (bestResults.get(best).isEmpty()) {
                bestResults.remove(best);
            }
        }
        if (bestResults.containsKey(stat)) {
            bestResults.get(stat).add(player);
            if (player.isOnline()) {
                showScoreboard(player.getPlayer());
            }
            return;
        }
        if (bestResults.containsKey(stat)) {
            bestResults.get(stat).add(player);
        } else {
            ArrayList<OfflinePlayer> players = new ArrayList<>();
            players.add(player);
            bestResults.put(stat, players);
            for (Player pl : Bukkit.getOnlinePlayers()) {
                this.showScoreboard(pl);
            }
        }
    }

    public N getPlayerBestScore(OfflinePlayer player) {
        for (Map.Entry<N, ArrayList<OfflinePlayer>> entry : bestResults.entrySet()) {
            for (OfflinePlayer p : entry.getValue()) {
                if (p == player) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public int getPlayerPosition(OfflinePlayer player) {
        int ctrl = 0;
        for (Map.Entry<N, ArrayList<OfflinePlayer>> entry : bestResults.descendingMap().entrySet()) {
            ctrl++;
            for (OfflinePlayer p : entry.getValue()) {
                if (p == player) {
                    return ctrl;
                }
            }
        }
        return 0;
    }
}
