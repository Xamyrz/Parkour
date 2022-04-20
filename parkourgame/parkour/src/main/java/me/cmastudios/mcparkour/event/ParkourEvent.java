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
import net.kyori.adventure.text.TextComponent;
import org.bukkit.boss.*;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import java.lang.Runnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;

public abstract class ParkourEvent {
    protected final EventCourse course;
    protected final Parkour plugin;
    protected ArrayList<BukkitTask> tasks = new ArrayList<>();
    private boolean started;
    private long startingTime = System.currentTimeMillis();
    protected int eventTime;
    public BossBar bar;

    public ParkourEvent(EventCourse course, Parkour plugin, int eventTime) {
        this.course = course;
        this.eventTime = eventTime;
        this.plugin = plugin;
    }

    public EventCourse getCourse() {
        return course;
    }

    public void end() {
        this.started = false;
        plugin.setEvent(null);
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();

        bar.setColor(BarColor.RED);
        bar.setTitle(Parkour.getString("event.ended"));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                bar.removeAll();
            }
        }, 100L);
        for (Map.Entry<Player, Parkour.PlayerCourseData> data : plugin.playerCourseTracker.entrySet()) {
            if (data.getValue() instanceof PlayerEventRushData) {
                data.getKey().teleport(getCourse().getCourse().getTeleport());
                data.getValue().restoreState(data.getKey());
                plugin.playerCourseTracker.remove(data.getKey());
            }
        }
    }

    protected void start() {
        this.started = true;
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
        startingTime = System.currentTimeMillis();
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, new GameEndTask(), 1, 20));
    }

    public void prepare() {
        bar = Bukkit.createBossBar(Parkour.getString("event.starting"), BarColor.YELLOW, BarStyle.SOLID);
        bar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(player);
        }
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, new StartCountDown(), 1, 20));
    }

    public boolean hasStarted() {
        return started;
    }

    public abstract void showScoreboard(Player player);

    protected class StartCountDown implements Runnable {
        @Override
        public void run() {
            int secondsPassed = (int) ((System.currentTimeMillis() - startingTime) / 1000);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(player);
                if (5 - secondsPassed <= 0) {
                    player.playNote(player.getLocation(), Instrument.PIANO, Note.natural(0, Note.Tone.F));
                    start();
                } else {
                    player.playNote(player.getLocation(), Instrument.STICKS, Note.natural(0, Note.Tone.D));
                }

            }
            bar.setTitle(Parkour.getString("event.starting", Parkour.getString(course.getType().getNameKey()), 5 - secondsPassed));
        }
    }

    protected class GameEndTask implements Runnable {
        @Override
        public void run() {
            int secondsPassed = (int) ((System.currentTimeMillis() - startingTime) / 1000);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(player);
            }
            if (secondsPassed < eventTime*60) {
                bar.setColor(BarColor.GREEN);
                bar.setTitle(Parkour.getString("event.started", Parkour.getString(getCourse().getType().getNameKey()), this.convertSecondsToUserFriendlyTime(eventTime*60-secondsPassed)));
            } else {
                end();
            }
        }

        private String convertSecondsToUserFriendlyTime(int secs) {
            int minutes = secs/60, seconds = secs%60;
            return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }
}
