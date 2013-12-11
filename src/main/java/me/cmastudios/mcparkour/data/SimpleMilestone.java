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

import java.util.Arrays;

/**
 *
 * @author maciekmm
 */
public class SimpleMilestone {

    ParkourAchievement[] criterias;

    public SimpleMilestone(ParkourAchievement... criterias) {
        this.criterias = criterias;
    }

    public boolean isSimiliar(SimpleMilestone mile) {
        return Arrays.asList(criterias).equals(Arrays.asList(mile.getCriterias()));
    }

    public boolean isCompleted(ParkourAchievement... completed) {
        if (completed.length < criterias.length) {
            return false;
        }
        int size = criterias.length; //Temporary thing, will be improved in future
        for (ParkourAchievement criterium : criterias) {
            for (ParkourAchievement ach : completed) {
                if (criterium.isSimiliar(ach)) {
                    size--;
                }
            }
        }
        return size == 0;
    }

    public ParkourAchievement[] getCriterias() {
        return criterias;
    }
}
