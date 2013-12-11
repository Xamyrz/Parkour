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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author maciekmm
 */
public class SimpleAchievement {

    AchievementCriteria criterium;
    List<Integer> options;

    public SimpleAchievement(AchievementCriteria criteria, Integer... options) {
        this.criterium = criteria;
        this.options = Arrays.asList(options);
    }

    public AchievementCriteria getCriterium() {
        return criterium;
    }

    public List<Integer> getOptions() {
        return options;
    }

    public boolean isSimiliar(SimpleAchievement achievement) {
        if (achievement.getCriterium().progressing == true && this.getCriterium().progressing == true) {
            return this.getCriterium() == achievement.getCriterium() && Arrays.asList(achievement.getOptions()).containsAll(Arrays.asList(this.getOptions()));
        } else {
            if (this.getCriterium() == achievement.getCriterium() && this.getCriterium().atLeast) {
                try {
                    ArrayList<Integer> optsBase = new ArrayList<>(this.getOptions());
                    ArrayList<Integer> optsChecked = new ArrayList<>(achievement.getOptions());
                    for (int option : this.getCriterium().optionsAffected) {
                        if (optsChecked.get(option) < optsBase.get(option)) {
                            return false;
                        } else {
                            optsBase.remove(option);
                            optsChecked.remove(option);
                        }
                    }
                    if (!optsBase.isEmpty() && !optsChecked.isEmpty()) {
                        return optsBase.equals(optsChecked);
                    } else {
                        return true;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return false;
                }
            } else {
                return this.getCriterium() == achievement.getCriterium() && this.getOptions().equals(achievement.getOptions());
            }
        }

    }

    public enum AchievementCriteria {

        PARKOUR_COMPLETE(false, false), //Completed certain parkour
        PARKOURS_COMPLETED(true, false), //Total parkours completed, it's definetely faster doing that this way.
        TOTAL_PLAYTIME(false, true, 1), //Total server playtime
        DUELS_PLAYED(false, true, 1), //TODO
        PLAYS_ON_CERTAIN_PARKOUR(false, true, 1), //Total plays on certain parkour no. of runs
        TOTAL_PLAYS_ON_PARKOURS(false, true, 1), //Total plays on all parkours
        LEVEL_ACQUIRE(false, true, 0), 
        FAVORITES_NUMBER(false, true, 0),
        GUILD_CREATE(false, false), //TODO
        GUILD_MEMBERSHIP(false, false), //TODO
        BEST_HIGHSCORE(false, false), 
        TOP_10(false, false), 
        BEAT_PREVIOUS_SCORE(false, false);

        public final boolean progressing;
        public final boolean atLeast; //SORRY, can't find better name for that
        public final int[] optionsAffected;

        private AchievementCriteria(boolean isProgressing, boolean atLeast, int... optionsAffected) {
            this.progressing = isProgressing;
            this.atLeast = atLeast;
            this.optionsAffected = optionsAffected;
        }
    }
}
