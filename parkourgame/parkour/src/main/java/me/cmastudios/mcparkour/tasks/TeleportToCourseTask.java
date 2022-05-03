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

package me.cmastudios.mcparkour.tasks;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import me.cmastudios.mcparkour.data.ParkourCourse;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runnable;

public class TeleportToCourseTask implements Runnable {
    private final int courseId;
    private final Player player;
    private final PlayerTeleportEvent.TeleportCause cause;
    private final Parkour plugin;

    public TeleportToCourseTask(Parkour plugin, Player player, PlayerTeleportEvent.TeleportCause cause, int courseId) {
        this.plugin = plugin;
        this.player = player;
        this.courseId = courseId;
        this.cause = cause;
    }

    @Override
    public void run() {
        this.performWithResult();
    }

    public Parkour.PlayResult performWithResult() {
        try {
            final ParkourCourse tpCourse = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
            if (tpCourse == null||((tpCourse.getMode() == ParkourCourse.CourseMode.HIDDEN || tpCourse.getMode() == ParkourCourse.CourseMode.EVENT) && cause == PlayerTeleportEvent.TeleportCause.COMMAND && !player.hasPermission("parkour.teleport"))) {
                player.sendMessage(Parkour.getString("error.course404"));
                return Parkour.PlayResult.NOT_FOUND;
            }
            IPlayerExperience pcd = Parkour.experience.getPlayerExperience(player);
            Parkour.PlayResult result = plugin.canPlay(player, pcd.getExperience(), tpCourse);
            if (result != Parkour.PlayResult.ALLOWED) {
                player.sendMessage(Parkour.getString(result.key));
            } else {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.teleport(tpCourse.getTeleport());
                        Utils.removeEffects(player);
                    }
                });
                if (tpCourse.getMode() != ParkourCourse.CourseMode.ADVENTURE) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Parkour.getString("course.name", tpCourse.getName()) + " ("+ tpCourse.getId()+")"));
                }
            }
            return result;
        } catch (SQLException ex) {
            Logger.getLogger(Parkour.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Parkour.PlayResult.ALLOWED;
    }
}
