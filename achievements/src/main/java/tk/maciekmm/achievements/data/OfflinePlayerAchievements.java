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

package tk.maciekmm.achievements.data;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import tk.maciekmm.achievements.Achievements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OfflinePlayerAchievements {
    protected static List<ParkourAchievement> achievements = new ArrayList<>();
    protected static List<AchievementMilestone> milestones = new ArrayList<>();
    protected static List<AchievementCriterium> criterias = new ArrayList<>();
    ArrayList<ParkourAchievement> completedAchievements;
    HashMap<ParkourAchievement, ArrayList<Long>> achievementProgress;
    ArrayList<AchievementMilestone> completedMilestones;

    public static List<ParkourAchievement> getSimiliarAchievements(SimpleAchievement ach) {
        List<ParkourAchievement> result = new ArrayList<>();
        for (ParkourAchievement achievement : achievements) {
            if (ach.isSimiliar(achievement)) {
                result.add(achievement);
            }
        }
        return result;
    }

    public static List<AchievementMilestone> getSimiliarMilestones(SimpleMilestone milestone) {
        List<AchievementMilestone> result = new ArrayList<>();
        for (AchievementMilestone mile : milestones) {
            if (mile == milestone) {
                result.add(mile);
            }
        }
        return result;
    }

    public static ParkourAchievement getAchievementById(int id) {
        for (ParkourAchievement achievement : achievements) {
            if (achievement.getId() == id) {
                return achievement;
            }
        }
        return null;
    }

    public static AchievementMilestone getMilestoneById(int id) {
        for (AchievementMilestone milestone : milestones) {
            if (milestone.getId() == id) {
                return milestone;
            }
        }
        return null;
    }

    public static AchievementCriterium getCriteriumByName(String name) {
        for (AchievementCriterium crit : criterias) {
            if (crit.name.equals(name)) {
                return crit;
            }
        }
        return null;
    }

    public static void setupCriterias(FileConfiguration config) {
        Set<String> criterias = config.getConfigurationSection("criterias").getKeys(false);
        for (String crit : criterias) {
            ConfigurationSection section = config.getConfigurationSection("criterias." + crit);
            if (!section.contains("progressing") || !section.contains("option") || !section.contains("affected")) {
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.missing", section.getCurrentPath()));
                continue;
            }
            try {
                List<String> options = new ArrayList<>();
                if (section.contains("options")) {
                    for (String option : section.getStringList("options")) {
                        options.add(option);
                    }
                }
                List<Integer> affectedOptions = new ArrayList<>();
                if (section.contains("affected")) {
                    for (Integer option : section.getIntegerList("affected")) {
                        affectedOptions.add(option);
                    }
                }
                OfflinePlayerAchievements.criterias.add(new AchievementCriterium(crit, section.getBoolean("progressing"), AchievementCriterium.Option.valueOf(section.getString("option")), options, affectedOptions));
            } catch (Exception e) { //CONFUSED AchievementCriteria.valueOf can throw IllegalArgumentException i believe but netbeans doesn't let me put it here
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.syntax", section.getCurrentPath()));
            }

        }
    }

    public static void setupAchievements(FileConfiguration config) {

        Set<String> sects = config.getConfigurationSection("achievements").getKeys(false);

        for (String sctn : sects) {
            ConfigurationSection section = config.getConfigurationSection("achievements." + sctn);
            if (!section.contains("criteria") || !section.contains("type") || !section.contains("name")) {
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.missing", section.getCurrentPath()));
                continue;
            }
            ArrayList<Long> opts = new ArrayList<>();
            ArrayList<String> desc = new ArrayList<>();
            if (section.contains("description")) {
                desc = new ArrayList(section.getStringList("description"));
            }
            try {
                AchievementCriterium criteria = getCriteriumByName(section.getString("criteria"));
                for (String option : criteria.options) {
                    if (!section.contains("options." + option)) {
                        continue;
                    }
                    if (!section.isList("options." + option)) {
                        opts.add(section.getLong("options." + option));
                    } else {
                        for (Long val : section.getLongList("options." + option)) {
                            opts.add(val);
                        }
                    }
                }
                achievements.add(new ParkourAchievement(Integer.valueOf(sctn), section.getString("name"), desc, criteria, ParkourAchievement.AchievementType.valueOf(section.getString("type")), opts.toArray(new Long[opts.size()])));
            } catch (Exception e) { //CONFUSED AchievementCriteria.valueOf can throw IllegalArgumentException i believe but netbeans doesn't let me put it here
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.syntax", section.getCurrentPath()));
            }
        }

        sects = config.getConfigurationSection("milestones").getKeys(false);

        for (String mile : sects) {
            List<ParkourAchievement> achs = new ArrayList<>();
            ConfigurationSection section = config.getConfigurationSection("milestones." + mile);
            if (!section.contains("achievements") || !section.contains("name")) {
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.missing", section.getCurrentPath()));
                continue;
            }
            ArrayList<String> desc = new ArrayList<>();
            if (section.contains("description")) {
                desc = new ArrayList(section.getStringList("description"));
            }
            try {
                for (Integer ach : section.getIntegerList("achievements")) {
                    if (getAchievementById(ach) != null) {
                        achs.add(getAchievementById(ach));
                    }
                }
                milestones.add(new AchievementMilestone(Integer.parseInt(mile), section.getString("name"), desc, achs.toArray(new ParkourAchievement[achs.size()])));
            } catch (Exception e) { //Same as above
                Bukkit.getLogger().log(Level.WARNING, Achievements.getString("achievement.error.loading.syntax", section.getCurrentPath()));
            }
        }
    }

    public static OfflinePlayerAchievements loadPlayerAchievements(OfflinePlayer p, Achievements plugin) {
        ArrayList<ParkourAchievement> completedAchievements = new ArrayList<>();
        ArrayList<AchievementMilestone> completedMilestones = new ArrayList<>();
        HashMap<ParkourAchievement, ArrayList<Long>> progressAchievements = new HashMap<>();
        try {

            PreparedStatement stmt = plugin.getAchievementsDatabase().prepareStatement("SELECT * FROM playerachievements WHERE player=?");
            stmt.setString(1, p.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("completed").length() > 0) {
                    String[] achievs = rs.getString("completed").split(",");
                    for (String s : achievs) {
                        ParkourAchievement ach = getAchievementById(Integer.parseInt(s));
                        if (ach != null) {
                            completedAchievements.add(ach);
                        }
                    }
                }
                if (rs.getString("progress").length() > 0) {
                    String[] progress = rs.getString("progress").split(";");
                    for (String s : progress) {
                        if (s.length() > 0) {

                            String[] parent = s.split("/");
                            if (parent.length > 1) {
                                String[] options = parent[1].split(",");

                                ParkourAchievement ach = getAchievementById(Integer.parseInt(parent[0]));
                                if (ach != null) {
                                    ArrayList<Long> optionList = new ArrayList<>();
                                    for (String opt : options) {
                                        optionList.add(Long.parseLong(opt));
                                    }
                                    progressAchievements.put(ach, optionList);
                                }
                            }
                        }
                    }
                }

                if (rs.getString("milestones").length() > 0) {
                    String[] miles = rs.getString("milestones").split(",");
                    for (String s : miles) {
                        AchievementMilestone milestone = getMilestoneById(Integer.parseInt(s));
                        if (milestone != null) {
                            completedMilestones.add(milestone);
                        }
                    }

                }
            }

            rs.close();

        } catch (SQLException ex) {
            Logger.getLogger(PlayerAchievements.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return new OfflinePlayerAchievements(completedMilestones, completedAchievements, progressAchievements);
    }

    public OfflinePlayerAchievements(ArrayList<AchievementMilestone> milestones, ArrayList<ParkourAchievement> achievements, HashMap<ParkourAchievement, ArrayList<Long>> progress) {
        this.completedAchievements = achievements;
        this.completedMilestones = milestones;
        this.achievementProgress = progress;
    }

    public double getModifier() {
        double mod = 0;
        for (AchievementMilestone mile : completedMilestones) {
            mod += mile.getRatioModifier();
        }
        return mod;
    }
}
