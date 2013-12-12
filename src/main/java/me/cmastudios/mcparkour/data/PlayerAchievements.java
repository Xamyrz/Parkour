/*
 * Copyright (C) 2013 maciekmm
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
package me.cmastudios.mcparkour.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.cmastudios.mcparkour.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author maciekmm
 */
public class PlayerAchievements implements ItemMenu {

    ArrayList<ParkourAchievement> completedAchievements = new ArrayList<>();
    HashMap<ParkourAchievement, List<Integer>> achievementProgress = new HashMap<>();
    ArrayList<AchievementMilestone> completedMilestones = new ArrayList<>();
    private Parkour plugin;
    private Player player;
    private int page = 1;

    public PlayerAchievements(Player p, Parkour plugin) {
        this.plugin = plugin;
        this.player = p;
        try {
            PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("SELECT * FROM playerachievements WHERE player=?");
            stmt.setString(1, p.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String[] achievements = rs.getString("completed").split(",");

                for (String s : achievements) {
                    if ("".equals(s)) {
                        continue;
                    }
                    ParkourAchievement ach = plugin.getAchievementById(Integer.parseInt(s));
                    if (ach != null) {
                        completedAchievements.add(ach);
                    }
                }
                String[] progress = rs.getString("progress").split(";");
                System.out.println(rs.getString("progress"));
                for (String s : progress) {
                    System.out.println(s);
                    if ("".equals(s)) {
                        continue;
                    }
                    String[] parent = s.split("|");
                    if (parent.length > 1) {

                        String[] options = parent[1].split(",");

                        ParkourAchievement ach = plugin.getAchievementById(Integer.parseInt(parent[0]));
                        if (ach != null) {
                            List<Integer> optionList = new ArrayList<>();
                            for (String opt : options) {
                                optionList.add(Integer.parseInt(opt));
                            }
                            achievementProgress.put(ach, optionList);
                        }
                    }
                }
                String[] milestones = rs.getString("milestones").split(",");

                for (String s : milestones) {
                    if ("".equals(s)) {

                        continue;
                    }
                    AchievementMilestone milestone = plugin.getMilestoneById(Integer.parseInt(s));
                    if (milestone != null) {
                        completedMilestones.add(milestone);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(PlayerAchievements.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Check if criterias are fullfilled and awards player with achievement.
     *
     * @param achievement gets SimpleAchievement for comparison
     */
    public void awardAchievement(SimpleAchievement achievement) {
        //Get similiar enchantments 
        List<ParkourAchievement> genericAchievements = plugin.getSimiliarAchievements(achievement);
        for (ParkourAchievement genericAchievement : genericAchievements) {
            if (genericAchievement == null) {
                return;
            }
            //Check if achievement is in progress
            if (achievementProgress.containsKey(genericAchievement) && !completedAchievements.contains(genericAchievement)) {
                List<Integer> objects = achievementProgress.get(genericAchievement);
                for (Integer o : achievement.getOptions()) {
                    if (!objects.contains(o)) {
                        objects.add(o);
                    }
                }
                //Check if achievement contains all keys
                if (Arrays.asList(achievement.getOptions()).containsAll(objects)) {
                    achievementProgress.remove(genericAchievement);
                    completedAchievements.add(genericAchievement);
                    //If successfully awarded check milestones
                    checkMilestones();
                } else {
                    achievementProgress.put(genericAchievement, objects);
                }
                //Check if not already awarded
            } else if (!completedAchievements.contains(genericAchievement)) {
                completedAchievements.add(genericAchievement);
                //If successfully awarded check milestones
                checkMilestones();
            }
        }
    }

    /**
     * Awards player with milestone
     *
     * @param milestone
     */
    public void awardMilestone(SimpleMilestone milestone) {
        for (AchievementMilestone mile : plugin.getSimiliarMilestones(milestone)) {
            //Check if not already completed
            if (!completedMilestones.contains(mile)) {
                completedMilestones.add(mile);
            }
        }
    }

    /**
     * Check if criterias are fullfilled and awards player with milestone.
     */
    public void checkMilestones() {
        for (AchievementMilestone mile : plugin.milestones) {
            if (!completedMilestones.contains(mile) && mile.isCompleted(completedAchievements.toArray(new ParkourAchievement[completedAchievements.size()]))) {
                awardMilestone(mile);
            }
        }
    }

    @Override
    public void openMenu() {
        Inventory inv = Bukkit.createInventory(player, 54, Parkour.getString("achievement.inventory.name"));
        if (plugin.achievements.size() + plugin.milestones.size() > 45 * page) {
            if (page > 1) {
                ItemStack prev = plugin.PREV_PAGE;
                prev.setAmount(page - 1);
                inv.setItem(46, prev);
            }
            if (plugin.achievements.size() + plugin.milestones.size() / 45 > 1) {
                ItemStack next = plugin.NEXT_PAGE;
                next.setAmount(page + 1);
                inv.setItem(53, next);
            }
        }

        int target = plugin.achievements.size();
        if (plugin.achievements.size() > 45) {
            target = 45;
        }

        for (int i = 0; i < target; i++) {
            ItemStack item;
            ItemMeta meta;
            ParkourAchievement current = plugin.achievements.get(45 * (page - 1) + i);
            if (completedAchievements.contains(current)) {
                item = plugin.ACHIEVEMENT_ACHIEVED;
                meta = item.getItemMeta();
                meta.setDisplayName(Parkour.getString("achievement.inventory.achievement.achieved", current.getName()));
            } else {
                item = plugin.ACHIEVEMENT;
                meta = item.getItemMeta();
                meta.setDisplayName(Parkour.getString("achievement.inventory.achievement.not_achieved", current.getName()));
            }
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        int startingPos = target;
        target = plugin.milestones.size() + plugin.achievements.size();
        if (plugin.milestones.size() + plugin.achievements.size() > 45) {
            target = 45;
        }

        for (int i = startingPos; i < target; i++) {
            ItemStack item;
            ItemMeta meta;
            AchievementMilestone current = plugin.milestones.get(45 * (page - 1) + i);
            if (completedMilestones.contains(current)) {
                item = plugin.ACHIEVEMENT_ACHIEVED;
                meta = item.getItemMeta();
                meta.setDisplayName(Parkour.getString("achievement.inventory.milestone.achieved", current.getName()));
            } else {
                item = plugin.ACHIEVEMENT;
                meta = item.getItemMeta();
                meta.setDisplayName(Parkour.getString("achievement.inventory.milestone.not_achieved", current.getName()));
            }
            item.setItemMeta(meta);
            inv.setItem(i - startingPos, item);
        }

        player.openInventory(inv);
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public void destroyMenu() {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        }, 1L);
    }

    @Override
    public void save() {
        try {
            StringBuilder sbach = new StringBuilder();
            for (ParkourAchievement ach : completedAchievements) {
                sbach.append(ach.getId()).append(",");
            }
            StringBuilder sbprogress = new StringBuilder();
            for (ParkourAchievement ach : achievementProgress.keySet()) {
                sbprogress.append(ach.getId()).append("|");
                for (int i : achievementProgress.get(ach)) {
                    sbprogress.append(i).append(",");
                }
                sbprogress.append(";");
            }
            StringBuilder sbmilestones = new StringBuilder();
            for (AchievementMilestone mile : completedMilestones) {
                sbmilestones.append(mile.getId()).append(",");
            }
            PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("INSERT INTO playerachievements (`player`,`completed`,`progress`,`milestones`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `completed`=VALUES(completed), `progress`=VALUES(progress), `milestones`=VALUES(milestones)");
            stmt.setString(1, player.getName());
            stmt.setString(2, sbach.toString());
            stmt.setString(3, sbprogress.toString());
            stmt.setString(4, sbmilestones.toString());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(PlayerAchievements.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
