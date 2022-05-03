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

package tk.maciekmm.achievements;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import java.lang.Runnable;

import org.bukkit.entity.Player;
import tk.maciekmm.achievements.data.AchievementMilestone;
import tk.maciekmm.achievements.data.ParkourAchievement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class SaveTask implements Runnable {

    private final ImmutableList<ParkourAchievement> achievements;
    private final ImmutableList<AchievementMilestone> milestones;
    private final HashMap<ParkourAchievement, ArrayList<Long>> progress;
    private final UUID uuid;
    private final String player;

    private final Connection conn;

    public SaveTask(Player player, ImmutableList<ParkourAchievement> achievements, ImmutableList<AchievementMilestone> milestones, HashMap<ParkourAchievement, ArrayList<Long>> progress, Connection connection) {
        this.uuid = player.getUniqueId();
        this.player = player.getName();
        this.achievements = achievements;
        this.milestones = milestones;
        this.progress = (HashMap<ParkourAchievement, ArrayList<Long>>) progress.clone();
        this.conn = connection;
    }

    @Override
    public void run() {
        try {
            StringBuilder sbach = new StringBuilder();
            for (ParkourAchievement ach : achievements) {
                sbach.append(ach.getId()).append(",");
            }
            StringBuilder sbprogress = new StringBuilder();
            for (ParkourAchievement ach : progress.keySet()) {
                sbprogress.append(ach.getId()).append("/");
                for (long i : progress.get(ach)) {
                    sbprogress.append(i).append(",");
                }
                sbprogress.append(";");
            }
            StringBuilder sbmilestones = new StringBuilder();
            for (AchievementMilestone mile : milestones) {
                sbmilestones.append(mile.getId()).append(",");
            }
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO playerachievements (`uuid`,`completed`,`progress`,`milestones`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `completed`=VALUES(completed), `progress`=VALUES(progress), `milestones`=VALUES(milestones)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, sbach.toString());
            stmt.setString(3, sbprogress.toString());
            stmt.setString(4, sbmilestones.toString());
            stmt.executeUpdate();

        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, String.format("Couldn't save playerachievements for %s %s", player, uuid.toString()));
        }
    }
}
