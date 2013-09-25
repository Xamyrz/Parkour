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
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerExperience;
import me.cmastudios.mcparkour.data.PlayerHighScore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
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
        this.playerCourseTracker = new HashMap<Player, PlayerCourseData>();
        plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(), 1L, 1L);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) throws SQLException {
        Player player = event.getPlayer();
        World world = player.getLocation().getWorld();
        Block below2 = world.getBlockAt(player.getLocation().add(0, -2, 0));
        Block below3 = world.getBlockAt(player.getLocation().add(0, -3, 0));
        Block below4 = world.getBlockAt(player.getLocation().add(0, -4, 0));
        Block below5 = world.getBlockAt(player.getLocation().add(0, -5, 0));
        Block[] belowArray = {below2, below3, below4, below5};
        Block below = null;
        for (Block belowBlock : belowArray) {
            if (belowBlock.getType() == Material.SIGN_POST
                    || belowBlock.getType() == Material.WALL_SIGN) {
                below = belowBlock;
            }
        }
        if (below != null) {
            if (below.getType() == Material.SIGN_POST
                    || below.getType() == Material.WALL_SIGN) {
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
                                playerCourseTracker.put(player, new PlayerCourseData(playerCourseTracker.get(player).course, player));
                                return;
                            } else {
                                // Remove players from a previous parkour course
                                PlayerCourseData remove = playerCourseTracker.remove(player);
                                player.setLevel(remove.previousLevel);
                            }
                        }
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), startParkourId);
                        if (course == null) {
                            event.setTo(player.getLocation().add(2, 0, 0)); // Prevent database spam
                            player.sendMessage(Parkour.getString("error.course404", new Object[]{}));
                            return; // Prevent console spam
                        }
                        PlayerCourseData data = new PlayerCourseData(course, player);
                        playerCourseTracker.put(player, data);
                        break;
                    case "[end]":
                        if (playerCourseTracker.containsKey(player)) {
                            PlayerCourseData endData = playerCourseTracker.remove(player); // They have ended their course anyhow
                            endData.restoreState(player);
                            PlayerHighScore highScore = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), player, endData.course.getId());
                            long completionTime = System.currentTimeMillis() - endData.startTime;
                            if (highScore.getTime() > completionTime) {
                                highScore.setTime(completionTime);
                                highScore.save(plugin.getCourseDatabase());
                                player.sendMessage(Parkour.getString("course.end.personalbest", new Object[]{endData.course.getId()}));
                            }
                            DecimalFormat df = new DecimalFormat("#.###");
                            double completionTimeSeconds = ((double) completionTime) / 1000;
                            player.sendMessage(Parkour.getString("course.end", new Object[]{df.format(completionTimeSeconds)}));
                            PlayerHighScore bestScore = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), endData.course.getId()).get(0);
                            if (highScore.equals(bestScore) && highScore.getTime() == completionTime) {
                                plugin.getServer().broadcast(Parkour.getString("course.end.best", new Object[]{player.getDisplayName() + ChatColor.RESET, endData.course.getId(), df.format(completionTimeSeconds)}), "parkour.play");
                            }
                            PlayerExperience playerXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), player);
                            try {
                                int courseXp = Integer.parseInt(sign.getLine(1));
                                playerXp.setExperience(playerXp.getExperience() + courseXp);
                                playerXp.save(plugin.getCourseDatabase());
                                player.sendMessage(Parkour.getString("xp.gain", new Object[]{courseXp, playerXp.getExperience()}));
                                event.getPlayer().setDisplayName(Parkour.getString("xp.prefix", new Object[] {
                                        plugin.getLevel(playerXp.getExperience()), event.getPlayer().getName() }));
                            } catch (NumberFormatException | IndexOutOfBoundsException e) { // No XP gain for this course
                            }
                            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                        }
                        break;
                    case "[vwall]":
                        if (!playerCourseTracker.containsKey(player)) {
                            event.setTo(player.getLocation().getBlock().getRelative(
                                    ((org.bukkit.material.Sign)sign.getData()).getFacing()).getLocation());
                        }
                        break;
                    case "[cancel]":
                        if (playerCourseTracker.containsKey(player)) {
                            final Location teleport = playerCourseTracker.get(player).course.getTeleport();
                            this.handlePlayerLeave(player, false);
                            event.setTo(teleport);
                        }
                        break;
                    case "[tp]":
                        int tpParkourId;
                        try {
                            tpParkourId = Integer.parseInt(sign.getLine(1));
                        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                            return; // Prevent console spam
                        }
                        ParkourCourse tpCourse = ParkourCourse.loadCourse(plugin.getCourseDatabase(), tpParkourId);
                        if (tpCourse == null) {
                            event.setTo(player.getLocation().add(2, 0, 0)); // Prevent database spam
                            player.sendMessage(Parkour.getString("error.course404", new Object[]{}));
                            return; // Prevent console spam
                        }
                        event.setTo(tpCourse.getTeleport());
                        player.sendMessage(Parkour.getString("course.teleport", new Object[]{tpCourse.getId()}));
                        break;
                }
            }
        }
        if (below2.getType() == Material.BEDROCK) {
            if (playerCourseTracker.containsKey(player)) {
                player.setFallDistance(0.0F);
                final Location teleport = playerCourseTracker.get(player).course.getTeleport();
                this.handlePlayerLeave(player, false);
                event.setTo(teleport);
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
            if (event.getClickedBlock().getType() == Material.SIGN_POST
                    || event.getClickedBlock().getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                if (sign.getLine(0).equals("[tp]")) {
                    // Right clicked a parkour teleport sign
                    try {
                        int parkourNumber = Integer.parseInt(sign.getLine(1));
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), parkourNumber);
                        event.setCancelled(true);
                        event.getPlayer().teleport(course.getTeleport());
                        event.getPlayer().sendMessage(Parkour.getString("course.teleport", new Object[]{course.getId()}));
                    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException ex) {
                    }
                    return;
                }
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getPlayer().getItemInHand().getType() == Material.ENDER_PEARL) {
                plugin.blindPlayers.remove(event.getPlayer());
                plugin.refreshVision(event.getPlayer());
                plugin.refreshHand(event.getPlayer());
                event.getPlayer().sendMessage(Parkour.getString("blind.disable", new Object[]{}));
            } else if (event.getPlayer().getItemInHand().getType() == Material.EYE_OF_ENDER) {
                plugin.blindPlayers.remove(event.getPlayer());
                plugin.blindPlayers.add(event.getPlayer());
                plugin.refreshVision(event.getPlayer());
                plugin.refreshHand(event.getPlayer());
                event.getPlayer().sendMessage(Parkour.getString("blind.enable", new Object[]{}));
            } else if (event.getPlayer().getItemInHand().getType() == Material.PAPER) {
                synchronized (plugin.deafPlayers) {
                    if (plugin.deafPlayers.contains(event.getPlayer())) {
                        plugin.deafPlayers.remove(event.getPlayer());
                        event.getPlayer().sendMessage(Parkour.getString("deaf.disable", new Object[]{}));
                    } else {
                        plugin.deafPlayers.add(event.getPlayer());
                        event.getPlayer().sendMessage(Parkour.getString("deaf.enable", new Object[]{}));
                    }
                }
            } else if (event.getPlayer().getItemInHand().getType() == Material.NETHER_STAR) {
                event.getPlayer().chat("/spawn");
            }
        }
    }

    @EventHandler
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        synchronized (plugin.deafPlayers) {
            for (Iterator<Player> it = event.getRecipients().iterator(); it.hasNext();) {
                Player player = it.next();
                if (plugin.deafPlayers.contains(player)) {
                    try {
                        it.remove();
                    } catch (Exception ex) {
                        // Impossible to modify recipients 
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) throws SQLException {
        PlayerExperience playerXp = PlayerExperience.loadExperience(
            plugin.getCourseDatabase(), event.getPlayer());
        event.getPlayer().setDisplayName(Parkour.getString("xp.prefix", new Object[]{
            plugin.getLevel(playerXp.getExperience()), event.getPlayer().getName()}));
        for (Player blindPlayer : plugin.blindPlayers) {
            plugin.refreshVision(blindPlayer);
        }
        if (!event.getPlayer().getInventory().contains(Material.EYE_OF_ENDER)) {
            event.getPlayer().getInventory().addItem(plugin.VISION);
        }
        if (!event.getPlayer().getInventory().contains(Material.PAPER)) {
            event.getPlayer().getInventory().addItem(plugin.CHAT);
        }
        if (!event.getPlayer().getInventory().contains(Material.NETHER_STAR)) {
            event.getPlayer().getInventory().addItem(plugin.SPAWN);
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        plugin.blindPlayers.remove(event.getPlayer());
        plugin.deafPlayers.remove(event.getPlayer());
        this.handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        plugin.blindPlayers.remove(event.getPlayer());
        plugin.deafPlayers.remove(event.getPlayer());
        this.handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getTo().getWorld() != event.getFrom().getWorld()
                || event.getTo().distance(event.getFrom()) >= 3) {
            this.handlePlayerLeave(event.getPlayer(), false);
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

    @EventHandler
    public void onSignChange(final SignChangeEvent event) {
        String firstLine = event.getLine(0);
        if (firstLine.startsWith("[") && firstLine.endsWith("]")) {
            if (!event.getPlayer().hasPermission("parkour.set")) {
                event.setLine(0, "-removed-");
                event.getPlayer().sendMessage(Parkour.getString("sign.noperms", new Object[]{}));
            }
        }
    }

    private void handlePlayerLeave(Player player) {
        this.handlePlayerLeave(player, true);
    }

    private void handlePlayerLeave(Player player, boolean teleport) {
        if (playerCourseTracker.containsKey(player)) {
            PlayerCourseData data = playerCourseTracker.remove(player);
            player.setLevel(data.previousLevel);
            player.setExp(0.0F);
            if (teleport) {
                player.teleport(player.getLocation().getWorld().getSpawnLocation());
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            } // Get them out of the arena
        }
    }

    private class PlayerCourseData {

        public final ParkourCourse course;
        public final long startTime;
        public final int previousLevel;

        public void restoreState(Player player) {
            player.setExp(0.0F);
            player.setLevel(previousLevel);
        }

        public PlayerCourseData(ParkourCourse course, Player player) {
            this.course = course;
            this.startTime = System.currentTimeMillis();
            this.previousLevel = player.getLevel();
            player.setExp(0.0F);
            player.setLevel(0);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.SLOW);
        }
    }

    private class XpCounterTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Player player : playerCourseTracker.keySet()) {
                int secondsPassed = (int) ((System.currentTimeMillis() - playerCourseTracker.get(player).startTime) / 1000);
                float remainder = (int) ((System.currentTimeMillis() - playerCourseTracker.get(player).startTime) % 1000);
                float tenthsPassed = remainder / 1000F;
                player.setLevel(secondsPassed);
                player.setExp(tenthsPassed);
            }
        }
    }
}
