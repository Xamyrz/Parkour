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

/**
 *
 * @author maciekmm
 */
public class AchievementMilestone extends SimpleMilestone {

    private int id;
    private String name, desc;

    /**
     * Creates AchievementMilestone from params
     * @param id - id of this milestone.
     * @param name - name of this milestone
     * @param desc - description of this milestone
     * @param conds - ParkourAchievements - conditions to make this milestone
     */
    public AchievementMilestone(int id, String name, String desc, ParkourAchievement... conds) {
        this.criterias = conds;
        this.id = id;
        this.name = name;
        this.desc = desc;
    }
    
    /**
     * Creates AchievementMilestone from SimpleMilestone and additional params
     * @param mile - SimpleMilestone
     * @param id - id of this Milestone
     * @param name - name of this Milestone
     * @param desc - description of this Milestone
     */
    public AchievementMilestone(SimpleMilestone mile, int id, String name, String desc) {
        super(mile.getCriterias());
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Get ratio modifier for ratio based on achievements that it contains and their AchievementType
     *
     * @return modifier
     */
    public double getRatioModifier() {
        //TODO
        return 0;
    }

    /**
     * Gets name of this milestone
     * @return name of milestone
     */
    public String getName() {
        return name;
    }

    /**
     * Gets description of this milestone
     * @return description of milestone
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Gets id of this milestone
     * @return id
     */
    public int getId() {
        return id;
    }

}
