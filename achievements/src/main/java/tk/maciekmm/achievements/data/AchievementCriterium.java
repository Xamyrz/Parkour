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

import java.util.List;

public class AchievementCriterium {
    public final String name;
    public final boolean progressing;
    public final Option option;
    public final List<Integer> optionsAffected;
    public final List<String> options;

    public AchievementCriterium(String name, boolean progressing, Option option,List<String> options, List<Integer> optionsAffected) {
        this.name = name;
        this.options = options;
        this.progressing = progressing;
        this.option = option;
        this.optionsAffected = optionsAffected;
    }

    public enum Option {
        MORE_THAN,
        LESS_THAN,
        NONE
    }
}
