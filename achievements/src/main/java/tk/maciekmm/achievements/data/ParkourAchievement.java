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

import java.util.ArrayList;
import org.bukkit.ChatColor;

/**
 * Represents full achievement, with name, type and id
 * 
 */
public class ParkourAchievement extends SimpleAchievement {

    private int id; // Just for saving purposes
    private String name;
    private ArrayList<String> description = new ArrayList<>();
    private AchievementType type;

    /**
     * 
     * @param id - id for this achievement
     * @param name - name for this achievement
     * @param description - achievement description
     * @param criteria - AchievementCriteria for this achievement
     * @param type - AchievementType for this achievement
     * @param options - Options that must be fullfilled to complete this achievement
     */
    public ParkourAchievement(int id, String name, ArrayList<String> description, AchievementCriterium criteria, AchievementType type, Double... options) {
        super(criteria, options);
        this.id = id;
        this.name = name;
        for(String s : description) {
            this.description.add(ChatColor.translateAlternateColorCodes('&',s));
        }
        this.type = type;
    }

    /**
     * Gets id
     * @return id of this achievement 
     */
    public int getId() {
        return id;
    }

    /**
     * Gets name
     * @return name of this achievement
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets AchievementType
     * @return AchievementType for this achievement
     */
    public AchievementType getType() {
        return type;
    }

    public ArrayList<String> getDescription() {
        return description;
    }
    
    /**
     * Milestones will calculate ratio modifier based on that.
     */
    public enum AchievementType {
        BRONZE(0.01,ChatColor.GOLD),
        SILVER(0.02,ChatColor.GRAY),
        GOLD(0.04,ChatColor.YELLOW),
        PLATINUM(0.07,ChatColor.DARK_GRAY),
        HIDDEN(0.04,ChatColor.BLACK);
        
        public final double modifier;
        public final ChatColor color;
        
        private AchievementType(double modifier,ChatColor color) {
            this.modifier = modifier;
            this.color = color;
        }
        
    }

}
