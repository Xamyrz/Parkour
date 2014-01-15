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
import java.util.Arrays;

/**
 * It's a simple to create achievement, easy for firing.
 */
public class SimpleAchievement {

    private AchievementCriterium criterium;
    private ArrayList<Long> options;

    /**
     * @param criteria - criteria for this achievement
     * @param options  - options for achievement
     */
    public SimpleAchievement(AchievementCriterium criteria, Long... options) {
        this.criterium = criteria;
        this.options = new ArrayList(Arrays.asList(options));
    }

    /**
     * Gets criterium
     *
     * @return criterium of this achievement
     */
    public AchievementCriterium getCriterium() {
        return criterium;
    }

    /**
     * Gets options of achievement.
     *
     * @return list of options.
     */
    public ArrayList<Long> getOptions() {
        return options;
    }

    /**
     * @param achievement to compare to this achievement
     * @return if they're similiar true otherwise false
     */
    public boolean isSimiliar(SimpleAchievement achievement) {
        if (this.getCriterium() != achievement.getCriterium()) {
            return false;
        }
        if (this.criterium.progressing) { // Check if achievement is progressing
            //Check if the simpleachievement contains all things from generic (this) achievement
            for (Long ac : options) {
                if (!achievement.getOptions().contains(ac)) {
                    return false;
                }
            }
            return true;
        } else if (this.criterium.option != AchievementCriterium.Option.NONE) { //Check if achievement is atLeast
            ArrayList<Long> baseOpts = new ArrayList<>(this.options); //Get ArrayList from List to be able to modify
            ArrayList<Long> checkedOpts = new ArrayList<>(achievement.getOptions());
            if (baseOpts.size() != checkedOpts.size()) {
                return false;
            }
            for (int option : this.criterium.optionsAffected) {
                switch (this.criterium.option) {
                    case LESS_THAN:
                        if (checkedOpts.get(option) < baseOpts.get(option)) {
                            return false;
                        }
                        break;
                    case MORE_THAN:
                        if (checkedOpts.get(option) > baseOpts.get(option)) {
                            return false;
                        }
                        break;
                }
                baseOpts.remove(option); //We remove that option to check other not affected by atLeast options
                checkedOpts.remove(option);
            }
            return baseOpts.equals(checkedOpts); //Check if remaining options are thesame.
        } else { //Normal check
            return this.getOptions().equals(achievement.getOptions());
        }
    }

}
