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

import me.cmastudios.experience.ExperienceManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tk.maciekmm.achievements.AchievementsManager;

public class AchievementsWrapper extends JavaPlugin {
    public static AchievementsManager achievements;
    public static ExperienceManager experience;

    @Override
    public void onEnable() {
        setupAchievements();
        setupExperience();
        Bukkit.getPluginManager().registerEvents(new AchievementsListener(),this);
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
    }

    private void setupAchievements() {
        RegisteredServiceProvider<AchievementsManager> acProvider = getServer().getServicesManager().getRegistration(tk.maciekmm.achievements.AchievementsManager.class);
        if (acProvider != null) {
            achievements = acProvider.getProvider();
        }
    }

    private boolean setupExperience() {
        RegisteredServiceProvider<ExperienceManager> xpProvider = getServer().getServicesManager().getRegistration(me.cmastudios.experience.ExperienceManager.class);
        if (xpProvider != null) {
            experience = xpProvider.getProvider();
        }
        return (experience != null);
    }

}
