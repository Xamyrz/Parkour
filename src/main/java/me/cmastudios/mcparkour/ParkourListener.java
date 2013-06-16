/*
 * Copyright (C) 2013 Connor Monahan
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Responds to Bukkit events for the Parkour plugin.
 *
 * @author Connor Monahan
 */
public class ParkourListener implements Listener {

    private final Parkour plugin;
    private Map<Player, PlayerCourseData> playerCourseTracker;

    public ParkourListener(Parkour instance) {
        this.plugin = instance;
        this.playerCourseTracker = new HashMap();
        plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(), 20L, 20L);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) throws SQLException {
        Player player = event.getPlayer();
        World world = player.getLocation().getWorld();
        Block below = world.getBlockAt(player.getLocation().add(0, -2, 0));
        if (below != null) {
            if (below.getType() == Material.SIGN_POST) {
                final Sign sign = (Sign) below.getState();
                final String controlLine = sign.getLine(0);
                switch (controlLine) {
                    case "[start]":
                        int startParkourId;
                        try {
                            startParkourId = Integer.parseInt(sign.getLine(1));
                        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                            return; // Prevent console spam
                        }
                        if (playerCourseTracker.containsKey(player)) {
                            if (playerCourseTracker.get(player).course.getId() == startParkourId) {
                                return;
                            } else {
                                // Remove players from a previous parkour course
                                PlayerCourseData remove = playerCourseTracker.remove(player);
                                player.setLevel(remove.previousLevel);
                            }
                        }
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), startParkourId);
                        if (course == null) {
                            return; // Prevent console spam
                        }
                        PlayerCourseData data = new PlayerCourseData();
                        data.course = course;
                        data.previousLevel = player.getLevel();
                        data.startTime = System.currentTimeMillis();
                        playerCourseTracker.put(player, data);
                        player.setLevel(0);
                        break;
                    case "[end]":
                        if (playerCourseTracker.containsKey(player)) {
                            PlayerCourseData endData = playerCourseTracker.remove(player); // They have ended their course anyhow
                            player.setLevel(endData.previousLevel);
                        }
                        break;
                }
            } else if (below.getType() == Material.BEDROCK) {
                if (playerCourseTracker.containsKey(player)) {
                    player.setFallDistance(0.0F);
                    event.setTo(playerCourseTracker.get(player).course.getTeleport());
                }
            }
        }
    }

    @EventHandler
    public void onXpLevelChange(final PlayerExpChangeEvent event) {
        if (playerCourseTracker.containsKey(event.getPlayer())) {
            event.setAmount(0);
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) throws SQLException {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getTypeId() == 63 || event.getClickedBlock().getTypeId() == 68) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                if (sign.getLine(0).equals("[tp]")) {
                    // Right clicked a parkour teleport sign
                    try {
                        int parkourNumber = Integer.parseInt(sign.getLine(1));
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), parkourNumber);
                        event.setCancelled(true);
                        event.getPlayer().teleport(course.getTeleport());
                    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException ex) {
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        this.handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getTo().distance(event.getFrom()) >= 3) {
            if (playerCourseTracker.containsKey(event.getPlayer())) {
                if (playerCourseTracker.get(event.getPlayer()).course.getTeleport() == event.getTo()) {
                    return;
                }
            }
            this.handlePlayerLeave(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (playerCourseTracker.containsKey(player)) {
                event.setCancelled(true);
            }
        }
    }

    private void handlePlayerLeave(Player player) {
        if (playerCourseTracker.containsKey(player)) {
            PlayerCourseData data = playerCourseTracker.remove(player);
            player.setLevel(data.previousLevel);
            player.teleport(player.getLocation().getWorld().getSpawnLocation()); // Get them out of the arena
        }
    }

    private class PlayerCourseData {

        public ParkourCourse course;
        public long startTime;
        public int previousLevel;
    }

    private class XpCounterTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Player player : playerCourseTracker.keySet()) {
                int secondsPassed = (int) ((System.currentTimeMillis() - playerCourseTracker.get(player).startTime) / 1000);
                player.setLevel(secondsPassed);
            }
        }
    }
}
