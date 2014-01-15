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

package me.cmastudios.mcparkour.data;

import java.util.ArrayList;
import java.util.List;

public class EventCourse {
    private final ParkourCourse course;
    private EventType eventType;
    private final String name;
    private final long startingTime;

    public EventCourse(ParkourCourse course,String name, long startingTime) {
        this.course = course;
        this.name = name;
        this.startingTime = startingTime;
    }

    public ParkourCourse getCourse() {
        return course;
    }

    public EventType getEventType() {
        return eventType;
    }

    public enum EventType {
        TIME_RUSH, POSITION_RUSH, PLAYS_RUSH, FALL_RUSH
    }
}
