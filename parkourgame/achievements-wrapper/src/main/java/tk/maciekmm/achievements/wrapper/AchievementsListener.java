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

package tk.maciekmm.achievements.wrapper;

import me.cmastudios.experience.events.ChangeExperienceEvent;
import me.cmastudios.mcparkour.events.PlayerCompleteParkourEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tk.maciekmm.achievements.data.PlayerAchievements;
import tk.maciekmm.achievements.data.SimpleAchievement;
import tk.maciekmm.favorites.FavoritesAddParkourEvent;

public class AchievementsListener implements Listener {

    @EventHandler
    public void onPlayerCompleteParkour(PlayerCompleteParkourEvent event) {
        if (!event.getHighScore().getPlayer().isOnline()) {
            return;
        }
        Player player = event.getHighScore().getPlayer().getPlayer();
        AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("PLAYS_ON_CERTAIN_PARKOUR"), (double) event.getHighScore().getCourse(), (double) event.getHighScore().getPlays()));
        AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("PARKOUR_COMPLETE"), (double) event.getHighScore().getCourse()));
        AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("PARKOURS_COMPLETED"), (double) event.getHighScore().getCourse()));
        AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("PARKOUR_COMPLETED_IN_TIME"), (double) event.getHighScore().getCourse(), event.getHighScore().getTime()));

        if (event.isPersonalBest()) {
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("BEAT_PREVIOUS_SCORE")));
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("BEAT_PREVIOUS_SCORE_ON_CERTAIN_PARKOUR"), (double) event.getHighScore().getCourse()));
        }

        if (event.isBest()) {
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("BEST_HIGHSCORE")));
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("BEST_HIGHSCORE_ON_CERTAIN_PARKOUR"), (double) event.getHighScore().getCourse()));
        }

        if (event.isTopTen()) {
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("TOP_10")));
            AchievementsWrapper.achievements.getPlayerAchievements(player).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("TOP_10_ON_CERTAIN_PARKOUR"), (double) event.getHighScore().getCourse()));
        }
    }

    @EventHandler
    public void onExperienceChange(ChangeExperienceEvent event) {
        if (event.getDifference() > 0) {
            event.setXp(event.getXpBefore()+(int)(event.getDifference() * (1 + AchievementsWrapper.achievements.getOfflinePlayerAchievements(event.getPlayerExperience().getPlayer()).getModifier())));
            if (event.getPlayerExperience().getPlayer().isOnline()) {
                AchievementsWrapper.achievements.getPlayerAchievements(event.getPlayerExperience().getPlayer().getPlayer()).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("LEVEL_ACQUIRE"), (double) AchievementsWrapper.experience.getLevel(event.getXp())));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFavoritesAddItem(FavoritesAddParkourEvent event) {
        AchievementsWrapper.achievements.getPlayerAchievements(event.getPlayer()).awardAchievement(new SimpleAchievement(PlayerAchievements.getCriteriumByName("FAVORITES_NUMBER"),(double) event.getFavorites().size()));
    }

}

