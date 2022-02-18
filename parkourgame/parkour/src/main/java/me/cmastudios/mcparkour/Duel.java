/*
 * Copyright (C) 2014 Connor Monahan
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

package me.cmastudios.mcparkour;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.Runnable;

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.data.GameResult;
import me.cmastudios.mcparkour.data.ParkourCourse;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Duel {
    private final Player initiator;
    private final Player competitor;
    private final ParkourCourse course;
    private boolean accepted;
    private boolean started;
    private long acceptTime;
    private final int bounty;
    private BukkitTask xpbar;

    public Duel(Player initiator, Player competitor, ParkourCourse course,
            int bounty) {
        this.initiator = initiator;
        this.competitor = competitor;
        this.course = course;
        this.accepted = false;
        this.started = false;
        this.bounty = bounty;
    }

    public Player getInitiator() {
        return initiator;
    }

    public Player getCompetitor() {
        return competitor;
    }

    public ParkourCourse getCourse() {
        return course;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
        if (accepted) {
            this.acceptTime = System.currentTimeMillis();
        }
    }

    public int getBounty() {
        return bounty;
    }

    public boolean hasStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public long getAcceptTime() {
        return acceptTime;
    }

	public void initiateDuel(Parkour plugin) {
        if (plugin.playerCourseTracker.containsKey(initiator)) {
            plugin.playerCourseTracker.remove(initiator).leave(initiator);
        }
        if (plugin.playerCourseTracker.containsKey(competitor)) {
            plugin.playerCourseTracker.remove(competitor).leave(competitor);
        }
        plugin.playerCheckpoints.remove(initiator);
        plugin.playerCheckpoints.remove(competitor);
        initiator.teleport(course.getTeleport(), TeleportCause.COMMAND);
        competitor.teleport(course.getTeleport(), TeleportCause.COMMAND);
        initiator.sendMessage(Parkour.getString("duel.init"));
        competitor.sendMessage(Parkour.getString("duel.init"));
        plugin.getServer().getScheduler()
                .runTaskLater(plugin, new DuelStartTimer(this), 200L);
        xpbar = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, new DuelCountdownXpTimer(this), 1L, 1L);
    }

    public void cancel(Parkour plugin, Player logoutee) throws SQLException {
        if (started) {
            this.win(logoutee == initiator ? competitor : initiator, plugin);
        } else if (accepted) {
            initiator.sendMessage(Parkour.getString("duel.cancelled"));
            competitor.sendMessage(Parkour.getString("duel.cancelled"));
            initiator.teleport(plugin.getSpawn(), TeleportCause.PLUGIN);
            competitor.teleport(plugin.getSpawn(), TeleportCause.PLUGIN);
        }
    }

    public void win(Player winner, Parkour plugin) throws SQLException {
		Connection db = plugin.getCourseDatabase();
        IPlayerExperience initXp = Parkour.experience.getPlayerExperience(initiator);
        IPlayerExperience compXp = Parkour.experience.getPlayerExperience(competitor);
        if (winner == initiator) {
            initXp.setExperience(initXp.getExperience() + bounty,false);
            compXp.setExperience(compXp.getExperience() - bounty,false);
        } else {
            initXp.setExperience(initXp.getExperience() - bounty,false);
            compXp.setExperience(compXp.getExperience() + bounty,false);
        }
        if (plugin.playerCourseTracker.containsKey(initiator)) {
            plugin.playerCourseTracker.remove(initiator).leave(initiator);
        }
        if (plugin.playerCourseTracker.containsKey(competitor)) {
            plugin.playerCourseTracker.remove(competitor).leave(competitor);
        }
        initiator.sendMessage(Parkour.getString("duel.win", winner.getName(), bounty));
        competitor.sendMessage(Parkour.getString("duel.win", winner.getName(), bounty));
		GameResult result = new GameResult(winner, winner == initiator ? competitor : initiator);
		result.save(db);
    }

    public void startTimeoutTimer(Parkour plugin) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new DuelTimeoutTimer(this, plugin), 1200L);
    }

    private class DuelStartTimer implements Runnable {

        private Duel duel;

        public DuelStartTimer(Duel duel) {
            this.duel = duel;
        }

        @Override
        public void run() {
            if (!duel.getInitiator().isOnline()
                    || !duel.getCompetitor().isOnline()) {
                return;
            }
            duel.setStarted(true);
            duel.xpbar.cancel();
            duel.getInitiator().sendMessage(Parkour.getString("duel.start"));
            duel.getCompetitor().sendMessage(Parkour.getString("duel.start"));
        }
    }

    private class DuelTimeoutTimer implements Runnable {

        private Duel duel;
        private Parkour plugin;

        public DuelTimeoutTimer(Duel duel, Parkour plugin) {
            this.duel = duel;
            this.plugin = plugin;
        }

        @Override
        public void run() {
            if (duel.isAccepted()) {
                return;
            }
            plugin.activeDuels.remove(duel);
            duel.getInitiator().sendMessage(Parkour.getString("duel.timeout"));
        }
    }

    private class DuelCountdownXpTimer implements Runnable {

        private Duel duel;

        public DuelCountdownXpTimer(Duel duel) {
            this.duel = duel;
        }
        @Override
        public void run() {
            for (Player player : new Player[] { duel.getInitiator(), duel.getCompetitor()}) {
                int secondsPassed = (int) ((System.currentTimeMillis() - duel.getAcceptTime()) / 1000);
                float remainder = (int) ((System.currentTimeMillis() - duel.getAcceptTime()) % 1000);
                float tenthsPassed = remainder / 1000F;
                player.setLevel(Math.max(10 - secondsPassed, 1));
                player.setExp(Math.max(1.0F - tenthsPassed, 0.0F));
            }
        }
    }
}
