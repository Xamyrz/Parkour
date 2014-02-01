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

package me.cmastudios.mcparkour.event;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.entity.Player;

public class PlayerEventRushData extends Parkour.PlayerCourseData {
    public PlayerEventRushData(ParkourCourse course, Player player, long time) {
        super(course, player, time);
    }

    public static class PlayerDistanceRushData extends PlayerEventRushData {
        private int sec;
        private int gate = 0;

        public PlayerDistanceRushData(ParkourCourse course, Player player, long time, int sec) {
            super(course, player, time);
            this.sec = sec;
        }

        public void addSecs(int secs) {
            sec += secs;
        }

        public void setGate(int gate) {
            this.gate = gate;
        }

        public int getGate() {
            return gate;
        }

        public int getSecs() {
            return sec;
        }
    }
}

