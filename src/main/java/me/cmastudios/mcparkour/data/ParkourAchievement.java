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
public class ParkourAchievement extends SimpleAchievement {

    private int id; // Just for saving purposes
    private String name;
    private AchievementType type;

    public ParkourAchievement(int id, String name, AchievementCriteria criteria, AchievementType type, Integer... options) {
        super(criteria, options);
        this.id = id;
        this.name = name;
        this.type = type;
        this.criterium = criteria;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public AchievementType getType() {
        return type;
    }

    public enum AchievementType {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        HIDDEN
    }

}
