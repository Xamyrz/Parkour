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
 * It's a simple to create achievement, easy for firing.
 *
 * @author maciekmm
 */
public class SimpleAchievement {

    AchievementCriteria criterium;
    List<Integer> options;

    /**
     * 
     * @param criteria - criteria for this achievement
     * @param options - options for achievement
     */
    public SimpleAchievement(AchievementCriteria criteria, Integer... options) {
        this.criterium = criteria;
        this.options = Arrays.asList(options);
    }

    /**
     * Gets criterium
     * @return criterium of this achievement
     */
    public AchievementCriteria getCriterium() {
        return criterium;
    }

    /**
     * Gets options of achievement.
     * @return list of options.
     */
    public List<Integer> getOptions() {
        return options;
    }

    /**
     *
     * @param achievement to compare to this achievement
     * @return if they're similiar true otherwise false
     */
    public boolean isSimiliar(SimpleAchievement achievement) {
        if (this.getCriterium() != achievement.getCriterium()) {
            return false;
        }
        if (this.criterium.progressing) { // Check if achievement is progressing
            //Check if the simpleachievement contains all things from generic (this) achievement 
            return Arrays.asList(achievement.getOptions()).containsAll(options);
        } else if (this.criterium.atLeast) { //Check if achievement is atLeast 
            ArrayList<Integer> baseOpts = new ArrayList<>(this.options); //Get ArrayList from List to be able to modify
            ArrayList<Integer> checkedOpts = new ArrayList<>(achievement.getOptions());
            if(baseOpts.size()!=checkedOpts.size()) {
                return false;
            }
            for (int option : this.criterium.optionsAffected) {
                if (checkedOpts.get(option) < baseOpts.get(option)) {
                    return false; //If checkedOpt is lower than baseOpts (required) return false, because it doesn't fullfill criterium
                } else {
                    baseOpts.remove(option); //We remove that option to check other not affected by atLeast options
                    checkedOpts.remove(option);
                }
            }
            return baseOpts.equals(checkedOpts); //Check if remaining options are thesame.
        } else { //Normal check
            return this.getOptions().equals(achievement.getOptions());
        }
    }

    public enum AchievementCriteria {

        /**
         * Achievement fired when someone completes parkour. It takes the
         * parkour number as first param.
         */
        PARKOUR_COMPLETE(false, false),
        /**
         * Achievement fired when someone completes parkour. It takes parkour
         * ids in params
         */
        PARKOURS_COMPLETED(true, false), //Total parkours completed, it's definetely faster doing that this way.
        /**
         * TODO still, will be fired on quit(?) or in scheduler. As the first
         * param it takes total playtime on server.
         */
        TOTAL_PLAYTIME(false, true, 0), //Total server playtime
        /**
         * TODO still, will be fired on duel completion. As the first param it
         * takes amount of duels played.
         */
        DUELS_PLAYED(false, true, 0), //TODO
        /**
         * Achievement fired when someone completes parkour. As the first param
         * it takes number(id) of parkour. As the second param it takes amount
         * of plays on certain parkour.
         */
        PLAYS_ON_CERTAIN_PARKOUR(false, true, 1), //Total plays on certain parkour no. of runs
        /**
         * TODO still, will be fired on parkour completion. As the first param
         * it takes amount of plays on all parkours.
         */
        TOTAL_PLAYS_ON_PARKOURS(false, true, 0), //Total plays on all parkours
        /**
         * Achievement fired when someone's level changed. As the first param it
         * takes a level. * It's atLeast, it means that the params specified in
         * affected options must be higher or equal to thesame params in generic
         * achievement.
         */
        LEVEL_ACQUIRE(false, true, 0),
        /**
         * Achievement fired when someone adds parkour to favorites. As the
         * first param it takes a favorites number
         */
        FAVORITES_NUMBER(false, true, 0),
        /**
         * TODO still, will be fired on guild creation.
         */
        GUILD_CREATE(false, false),
        /**
         * TODO still, will be fired when player joins guild.
         */
        GUILD_MEMBERSHIP(false, false),
        /**
         * Achievement fired when someone's gets the best highscore on parkour.
         * It doesn't take any params (will take parkour id in the future maybe)
         */
        BEST_HIGHSCORE(false, false),
        /**
         * TODO still, will be fired when someone gets to TOP_10. It doesn't
         * take any params (will take parkour id in the future maybe)
         */
        TOP_10(false, false),
        /**
         * Achievement fired when someone beats his previous score. It doesn't
         * take any params (will take parkour id in the future maybe)
         */
        BEAT_PREVIOUS_SCORE(false, false);

        /**
         * if true it means that the orded of params is not checked and they're
         * expandable.
         */
        public final boolean progressing;
        /**
         * if true it means that the params specified in affected options must
         * be higher or equal to thesame params in generic achievement.
         */
        public final boolean atLeast;
        public final int[] optionsAffected;

        private AchievementCriteria(boolean isProgressing, boolean atLeast, int... optionsAffected) {
            this.progressing = isProgressing;
            this.atLeast = atLeast;
            this.optionsAffected = optionsAffected;
        }
    }
}
