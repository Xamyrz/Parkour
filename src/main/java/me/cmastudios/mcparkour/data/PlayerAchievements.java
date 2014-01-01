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
package me.cmastudios.mcparkour.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.cmastudios.mcparkour.Item;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.SimpleAchievement.AchievementCriteria;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerAchievements implements ItemMenu {

    private static List<ParkourAchievement> achievements = new ArrayList<>();
    private static List<AchievementMilestone> milestones = new ArrayList<>();
    ArrayList<ParkourAchievement> completedAchievements;
    HashMap<ParkourAchievement, ArrayList<Long>> achievementProgress;
    ArrayList<AchievementMilestone> completedMilestones;
    private Player player;
    private final Parkour plugin;
    private int page = 1;

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
            if (mile==milestone) {
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

    public static void setupAchievements(FileConfiguration config) {

        Set<String> sects = config.getConfigurationSection("achievements").getKeys(false);

        for (String sctn : sects) {
            ConfigurationSection section = config.getConfigurationSection("achievements." + sctn);
            if (!section.contains("criteria") || !section.contains("type") || !section.contains("name")) {
                Bukkit.getLogger().log(Level.WARNING, Parkour.getString("achievement.error.loading.missing", section.getCurrentPath()));
                continue;
            }
            List<Long> opts = new ArrayList<>();
            ArrayList<String> desc = new ArrayList<>();
            if (section.contains("description")) {
                desc = new ArrayList(section.getStringList("description"));
            }
            try {
                AchievementCriteria criteria = AchievementCriteria.valueOf(section.getString("criteria"));

                switch (criteria) {
                    case PARKOUR_COMPLETE:
                    case TOP_10_ON_CERTAIN_PARKOUR:
                    case PARKOUR_COMPLETED_WITHOUT_CHECKPOINTS:
                    case BEAT_PREVIOUS_SCORE_ON_CERTAIN_PARKOUR:
                    case BEST_HIGHSCORE_ON_CERTAIN_PARKOUR:
                        if (!section.contains("options.parkour")) {
                            continue;
                        }
                        opts.add(section.getLong("options.parkour"));
                        break;
                    case PARKOURS_COMPLETED:
                        if (!section.contains("options.parkours")) {
                            continue;
                        }
                        for (long pk : section.getLongList("options.parkours")) {
                            opts.add(pk);
                        }
                        break;
                    case TOTAL_PLAYTIME:
                    case DUELS_PLAYED:
                    case TOTAL_PLAYS_ON_PARKOURS:
                    case LEVEL_ACQUIRE:
                    case FAVORITES_NUMBER:
                        if (!section.contains("options.required_amount")) {
                            continue;
                        }
                        opts.add(section.getLong("options.required_amount"));
                        break;
                    case PLAYS_ON_CERTAIN_PARKOUR:
                    case PARKOUR_COMPLETED_IN_TIME:
                        if (!section.contains("options.parkour")) {
                            continue;
                        }
                        if (!section.contains("options.required_amount")) {
                            continue;
                        }
                        opts.add(section.getLong("options.parkour"));
                        opts.add(section.getLong("options.required_amount"));
                        break;
                    /* These achievements don't get any params
                     case GUILD_CREATE:
                     case GUILD_MEMBERSHIP:
                     case BEST_HIGHSCORE:
                     case TOP_10:
                     case BEAT_PREVIOUS_SCORE:
                     break;*/
                }
                achievements.add(new ParkourAchievement(Integer.valueOf(sctn), section.getString("name"), desc, criteria, ParkourAchievement.AchievementType.valueOf(section.getString("type")), opts.toArray(new Long[opts.size()])));

            } catch (Exception e) { //CONFUSED AchievementCriteria.valueOf can throw IllegalArgumentException i believe but netbeans doesn't let me put it here
                Bukkit.getLogger().log(Level.WARNING, Parkour.getString("achievement.error.loading.syntax", section.getCurrentPath()));
            }
        }

        sects = config.getConfigurationSection("milestones").getKeys(false);

        for (String mile : sects) {
            List<ParkourAchievement> achs = new ArrayList<>();
            ConfigurationSection section = config.getConfigurationSection("milestones." + mile);
            if (!section.contains("achievements") || !section.contains("name")) {
                Bukkit.getLogger().log(Level.WARNING, Parkour.getString("achievement.error.loading.missing", section.getCurrentPath()));
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
                Bukkit.getLogger().log(Level.WARNING, Parkour.getString("achievement.error.loading.syntax", section.getCurrentPath()));
            }
        }
    }

    public static PlayerAchievements loadPlayerAchievements(Player p, Parkour plugin) {
        ArrayList<ParkourAchievement> completedAchievements = new ArrayList<>();
        ArrayList<AchievementMilestone> completedMilestones = new ArrayList<>();
        HashMap<ParkourAchievement, ArrayList<Long>> progressAchievements = new HashMap<>();
        try {
            PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("SELECT * FROM playerachievements WHERE player=?");
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
        return new PlayerAchievements(plugin, p, completedMilestones, completedAchievements, progressAchievements);
    }

    public PlayerAchievements(Parkour plugin, Player p, ArrayList<AchievementMilestone> milestones, ArrayList<ParkourAchievement> achievements, HashMap<ParkourAchievement, ArrayList<Long>> progress) {
        this.player = p;
        this.completedAchievements = achievements;
        this.completedMilestones = milestones;
        this.achievementProgress = progress;
        this.plugin = plugin;
    }

    /**
     * Check if criterias are fullfilled and awards player with achievement.
     *
     * @param achievement gets SimpleAchievement for comparison
     */
    public void awardAchievement(SimpleAchievement achievement) {
        //Get similiar enchantments 
        List<ParkourAchievement> genericAchievements = getSimiliarAchievements(achievement);
        for (ParkourAchievement genericAchievement : genericAchievements) {
            if (genericAchievement == null) {
                return;
            }
            //Check if achievement is in progress
            if (achievement.getCriterium().progressing && !completedAchievements.contains(genericAchievement)) {
                if (achievementProgress.containsKey(genericAchievement)) {
                    ArrayList<Long> objects = (ArrayList<Long>) achievementProgress.get(genericAchievement);
                    for (Long o : achievement.getOptions()) {
                        if (!objects.contains(o)) {
                            objects.add(o);
                        }
                    }
                    //Check if achievement contains all keys, should work
                    if (objects.size() == genericAchievement.getOptions().size()) {
                        achievementProgress.remove(genericAchievement);
                        completedAchievements.add(genericAchievement);
                        player.sendMessage(Parkour.getString("achievement.achievement.achieved", genericAchievement.getName(), genericAchievement.getType().color.getChar()));
                        //If successfully awarded check milestones
                        checkMilestones();
                    }
                } else {
                    achievementProgress.put(genericAchievement, achievement.getOptions());
                }
                //Check if not already awarded
            } else if (!completedAchievements.contains(genericAchievement)) {
                completedAchievements.add(genericAchievement);
                player.sendMessage(Parkour.getString("achievement.achievement.achieved", genericAchievement.getName(), genericAchievement.getType().color.getChar()));
                //If successfully awarded check milestones
                checkMilestones();
            }
        }
    }

    /**
     * Awards player with milestone
     */
    public void awardMilestone(SimpleMilestone milestone) {
        for (AchievementMilestone mile : getSimiliarMilestones(milestone)) {
            //Check if not already completed
            if (!completedMilestones.contains(mile)) {
                completedMilestones.add(mile);
                player.sendMessage(Parkour.getString("achievement.milestone.achieved", mile.getName(), mile.getRatioModifier(), getModifier()));
                Bukkit.broadcastMessage(Parkour.getString("achievement.milestone.broadcast", player.getName(), mile.getName(), getModifier()));
            }
        }
    }

    /**
     * Check if criterias are fullfilled and awards player with milestone.
     */
    public void checkMilestones() {
        for (AchievementMilestone mile : milestones) {
            if (!completedMilestones.contains(mile) && mile.isCompleted(completedAchievements.toArray(new ParkourAchievement[completedAchievements.size()]))) {
                awardMilestone(mile);
            }
        }
    }

    @Override
    public void openMenu() {
        openMenu(page);
    }

    public void openMenu(int page) {
        Inventory inv = Bukkit.createInventory(player, 54, Parkour.getString("achievement.inventory.name"));
        if (achievements.size() + milestones.size() > 45 * page) {
            ItemStack next = Item.NEXT_PAGE.getItem();
            next.setAmount(page + 1);
            inv.setItem(53, next);
        }

        if (page > 1) {
            ItemStack prev = Item.PREV_PAGE.getItem();
            prev.setAmount(page - 1);
            inv.setItem(45, prev);
        }

        int target = achievements.size();
        if (achievements.size() > 45) {
            target = 45;
        }

        for (int i = 0; i < target; i++) {
            ItemStack item;
            ItemMeta meta;
            ParkourAchievement current = achievements.get(45 * (page - 1) + i);
            if (completedAchievements.contains(current)) {
                item = Item.ACHIEVEMENT_ACHIEVED.getItem();
                meta = item.getItemMeta();
                meta.setLore(current.getDescription());
                meta.setDisplayName(Parkour.getString("achievement.inventory.achievement.achieved", current.getName(), current.getType().color.getChar()));
            } else {
                item = Item.ACHIEVEMENT.getItem();
                meta = item.getItemMeta();
                meta.setDisplayName(Parkour.getString("achievement.inventory.achievement.not_achieved", meta.getDisplayName(), current.getType().color.getChar()));
                if (current.getType() != ParkourAchievement.AchievementType.HIDDEN) {
                    meta.setLore(current.getDescription());
                    meta.setDisplayName(Parkour.getString("achievement.inventory.achievement.not_achieved", current.getName(), current.getType().color.getChar()));
                }
            }

            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        int startingPos = target;
        target = milestones.size() + achievements.size();
        if (milestones.size() + achievements.size() > 45) {
            target = 45;
        }

        for (int i = startingPos; i < target; i++) {
            ItemStack item;
            ItemMeta meta;
            AchievementMilestone current = milestones.get(45 * (page - 1) + i - startingPos);
            if (completedMilestones.contains(current)) {
                item = Item.MILESTONE_ACHIEVED.getItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Parkour.getString("achievement.inventory.milestone.achieved", current.getName())));
            } else {
                item = Item.MILESTONE.getItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Parkour.getString("achievement.inventory.milestone.not_achieved", current.getName())));
            }
            ArrayList<String> desc = (ArrayList<String>) current.getDescription().clone();
            desc.add(Parkour.getString("achievement.inventory.milestone.modifier", current.getRatioModifier()));
            meta.setLore(desc);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public void handleSelection(int page, int slot, ClickType click, Inventory inv) {
        if (slot == 45 && inv.getItem(45) != null) {
            this.page--;
            destroyMenu();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    openMenu();
                }
            }, 2L);
            return;
        } else if (slot == 53 && inv.getItem(53) != null) {
            this.page++;
            destroyMenu();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    openMenu();
                }
            }, 2L);
            return;
        }
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
                sbprogress.append(ach.getId()).append("/");
                for (long i : achievementProgress.get(ach)) {
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
            Logger.getLogger(PlayerAchievements.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public double getModifier() {
        double mod = 0;
        for (AchievementMilestone mile : completedMilestones) {
            mod += mile.getRatioModifier();
        }
        return mod;
    }
}
