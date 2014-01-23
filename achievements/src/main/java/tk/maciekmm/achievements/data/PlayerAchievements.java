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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;
import tk.maciekmm.achievements.Achievements;
import tk.maciekmm.achievements.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tk.maciekmm.achievements.SaveTask;

public class PlayerAchievements extends OfflinePlayerAchievements implements ItemMenu {
    private Player player;
    private int page = 1;
    private final Achievements plugin;

    public PlayerAchievements(Achievements plugin, Player p, ArrayList<AchievementMilestone> milestones, ArrayList<ParkourAchievement> achievements, HashMap<ParkourAchievement, ArrayList<Long>> progress) {
        super(p, milestones, achievements, progress);
        this.plugin = plugin;
        this.player = p;
    }

    public PlayerAchievements(OfflinePlayerAchievements playerAchievements, Achievements plugin, Player player) {
        super(player, playerAchievements.completedMilestones, playerAchievements.completedAchievements, playerAchievements.achievementProgress);
        this.plugin = plugin;
        this.player = player;
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
                    ArrayList<Long> objects = achievementProgress.get(genericAchievement);
                    for (Long o : achievement.getOptions()) {
                        if (!objects.contains(o)) {
                            objects.add(o);
                        }
                    }
                    //Check if achievement contains all keys, should work
                    if (objects.size() == genericAchievement.getOptions().size()) {
                        achievementProgress.remove(genericAchievement);
                        completedAchievements.add(genericAchievement);
                        if (player.isOnline()) {
                            player.getPlayer().sendMessage(Achievements.getString("achievement.achievement.achieved", genericAchievement.getName(), genericAchievement.getType().color.getChar()));
                        }//If successfully awarded check milestones
                        checkMilestones();
                    }
                } else {
                    achievementProgress.put(genericAchievement, achievement.getOptions());
                }
                //Check if not already awarded
            } else if (!completedAchievements.contains(genericAchievement)) {
                completedAchievements.add(genericAchievement);
                player.sendMessage(Achievements.getString("achievement.achievement.achieved", genericAchievement.getName(), genericAchievement.getType().color.getChar()));
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
                player.getPlayer().sendMessage(Achievements.getString("achievement.milestone.achieved", mile.getName(), mile.getRatioModifier(), getModifier()));
                Bukkit.broadcastMessage(Achievements.getString("achievement.milestone.broadcast", player.getName(), mile.getName(), getModifier()));
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
        Inventory inv = Bukkit.createInventory(player, 54, Achievements.getString("achievement.inventory.name"));
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
                meta.setDisplayName(Achievements.getString("achievement.inventory.achievement.achieved", current.getName(), current.getType().color.getChar()));
            } else {
                item = Item.ACHIEVEMENT.getItem();
                meta = item.getItemMeta();
                meta.setDisplayName(Achievements.getString("achievement.inventory.achievement.not_achieved", meta.getDisplayName(), current.getType().color.getChar()));
                if (current.getType() != ParkourAchievement.AchievementType.HIDDEN) {
                    meta.setLore(current.getDescription());
                    meta.setDisplayName(Achievements.getString("achievement.inventory.achievement.not_achieved", current.getName(), current.getType().color.getChar()));
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
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Achievements.getString("achievement.inventory.milestone.achieved", current.getName())));
            } else {
                item = Item.MILESTONE.getItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Achievements.getString("achievement.inventory.milestone.not_achieved", current.getName())));
            }
            ArrayList<String> desc = (ArrayList<String>) current.getDescription().clone();
            desc.add(Achievements.getString("achievement.inventory.milestone.modifier", current.getRatioModifier()));
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

    public int getPage() {
        return page;
    }

    public void save(boolean async) {
        SaveTask task = new SaveTask(player.getName(), ImmutableList.copyOf(completedAchievements), ImmutableList.copyOf(completedMilestones), achievementProgress, plugin.getAchievementsDatabase());
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }
}
