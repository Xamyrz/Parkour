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

package me.cmastudios.mcparkour.events;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.PlayerHighScore;

public class PlayerCompleteParkourEventBuilder {
    private Parkour.PlayerCourseData endData;
    private PlayerHighScore highScore;
    private IPlayerExperience experience;
    private double completionTime;
    private boolean isPersonalBest;
    private boolean isBest;
    private double xp;
    private boolean isTopTen;


    public PlayerCompleteParkourEventBuilder(Parkour.PlayerCourseData endData,IPlayerExperience experience, PlayerHighScore highScore,double completionTime) {
        this.endData = endData;
        this.experience = experience;
        this.highScore = highScore;
        this.completionTime = completionTime;
    }

    public void setPersonalBest(boolean isPersonalBest) {
        this.isPersonalBest = isPersonalBest;
    }

    public void setBest (boolean isBest) {
        this.isBest = isBest;
    }

    public PlayerCompleteParkourEvent getEvent() {
        return new PlayerCompleteParkourEvent(endData,experience,highScore,completionTime,isPersonalBest,isBest,xp,isTopTen);
    }

    public void setReducedXp(double xp) {
        this.xp = xp;
    }

    public void setTopTen(boolean isTopTen) {
        this.isTopTen = isTopTen;
    }
}
