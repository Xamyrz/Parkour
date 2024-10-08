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

import java.util.Arrays;

/**
 * Milestone is achievement that you get when completing other achievements,
 * it'll have an impact on ratio and modifier
 *
 */
public class SimpleMilestone {

    private ParkourAchievement[] criterias;
    private double modifier;

    /**
     * Creates SimpleMilestone based only on ParkourAchievements
     *
     * @param criterias - ParkourAchievements(criterias) that must be completed
     * to get this achievement
     */
    public SimpleMilestone(ParkourAchievement... criterias) {
        this.criterias = criterias;
        for(ParkourAchievement ach : criterias) {
            this.modifier += ach.getType().modifier;
        }
    }

    /**
     * Checks if this milestone is similiar to other
     *
     * @param mile - milestone to check
     * @return boolean - true if equals or false if not equals
     */
    public boolean isSimiliar(SimpleMilestone mile) {
        for(ParkourAchievement ac : mile.getCriterias()) {
                if(!Arrays.asList(criterias).contains(ac)) {
                    return false;
                }
            }
            return true;
    }

    /**
     * Checks if achievement is completed
     *
     * @param completed - achievements that player has
     * @return true - if is completed
     */
    public boolean isCompleted(ParkourAchievement... completed) {
        if (completed.length < criterias.length) {
            return false;
        }
        int size = criterias.length; //Temporary thing, will be improved in future
        //Iterates through needed achievement
        for (ParkourAchievement criterium : criterias) {
            //Iterates through player achievements to check if it has certain achievement
            for (ParkourAchievement ach : completed) {
                if (criterium==ach) {
                    size--;
                }
            }
        }
        return size <= 0;
    }

    /**
     * Gets ParkourAchievement that must be completed
     *
     * @return ParkourAchievements needed for this milestone to award
     */
    public ParkourAchievement[] getCriterias() {
        return criterias;
    }

    /**
     * Get ratio modifier for ratio based on achievements that it contains and
     * their AchievementType
     *
     * @return modifier
     */
    public double getRatioModifier() {
        return modifier;
    }
}
