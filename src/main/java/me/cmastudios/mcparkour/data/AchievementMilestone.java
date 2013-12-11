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
    private String name,desc;
    
    public AchievementMilestone(int id, String name, String desc, ParkourAchievement... conds) {
        this.criterias = conds;
        this.id = id;
        this.name = name;
        this.desc = desc;
    }
    
    public AchievementMilestone(SimpleMilestone mile, int id, String name, String desc) {
        super(mile.getCriterias());
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public double getRatioModifier() {
        //TODO
        return 0;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return desc;
    }
    
    public int getId() {
        return id;
    }
    
    
}
