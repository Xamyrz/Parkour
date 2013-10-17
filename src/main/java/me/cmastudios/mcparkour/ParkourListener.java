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
import java.util.Iterator;
import java.util.List;

import me.cmastudios.mcparkour.Parkour.PlayerCourseData;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Responds to Bukkit events for the Parkour plugin.
 *
 * @author Connor Monahan
 */
public class ParkourListener implements Listener {

    private final Parkour plugin;
    public static final int DETECTION_MIN = 2;
    public static final int SIGN_DETECTION_MAX = 5;

    public ParkourListener(Parkour instance) {
        this.plugin = instance;
        plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(), 1L, 1L);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) throws SQLException {
        Player player = event.getPlayer();
        Block below = this.detectBlocks(player.getLocation(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX) ?
                this.getBlockInDepthRange(player.getLocation(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX)
                : this.getBlockInDepthRange(player.getLocation(), Material.WALL_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX);
        if (below != null) {
            if (below.getType() == Material.SIGN_POST
                    || below.getType() == Material.WALL_SIGN) {
                final Sign sign = (Sign) below.getState();
                final String controlLine = sign.getLine(0);
                switch (controlLine) {
                    case "[start]":
                        plugin.completedCourseTracker.remove(player);
                        int startParkourId;
                        try {
                            startParkourId = Integer.parseInt(sign.getLine(1));
                        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                            return; // Prevent console spam
                        }
                        Checkpoint startCp = plugin.playerCheckpoints.get(player);
                        if (plugin.playerCourseTracker.containsKey(player)) {
                            if (plugin.playerCourseTracker.get(player).course.getId() == startParkourId) {
                                if (startCp != null && startCp.getCourse().getId() == startParkourId) {
                                    ignoreTeleport = true;
                                    event.setTo(startCp.getLocation());
                                    return;
                                }
                                plugin.playerCourseTracker.put(player, new PlayerCourseData(plugin.playerCourseTracker.get(player).course, player));
                                return;
                            } else {
                                // Remove players from a previous parkour course
                                PlayerCourseData remove = plugin.playerCourseTracker.remove(player);
                                player.setLevel(remove.previousLevel);
                            }
                        }
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), startParkourId);
                        if (course == null) {
                            event.setTo(player.getLocation().add(2, 0, 0)); // Prevent database spam
                            player.sendMessage(Parkour.getString("error.course404", new Object[]{}));
                            return; // Prevent console spam
                        }
                        List<PlayerHighScore> startScores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), course.getId(), 10);
                        player.setScoreboard(course.getScoreboard(startScores));
                        PlayerCourseData data = new PlayerCourseData(course, player);
                        plugin.playerCourseTracker.put(player, data);
                        break;
                    case "[end]":
                        if (plugin.playerCourseTracker.containsKey(player)) {
                            PlayerCourseData endData = plugin.playerCourseTracker.remove(player); // They have ended their course anyhow
                            endData.restoreState(player);
                            PlayerHighScore highScore = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), player, endData.course.getId());
                            long completionTime = System.currentTimeMillis() - endData.startTime;
                            if (highScore.getTime() > completionTime) {
                                highScore.setTime(completionTime);
                                player.sendMessage(Parkour.getString("course.end.personalbest", new Object[]{endData.course.getId()}));
                            }
                            highScore.setPlays(highScore.getPlays() + 1);
                            highScore.save(plugin.getCourseDatabase());
                            DecimalFormat df = new DecimalFormat("#.###");
                            double completionTimeSeconds = ((double) completionTime) / 1000;
                            player.sendMessage(Parkour.getString("course.end", new Object[]{df.format(completionTimeSeconds)}));
                            List<PlayerHighScore> scores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), endData.course.getId(), 10);
                            PlayerHighScore bestScore = scores.get(0);
                            if (highScore.equals(bestScore) && highScore.getTime() == completionTime) {
                                plugin.getServer().broadcast(Parkour.getString("course.end.best", new Object[]{player.getDisplayName() + ChatColor.RESET, endData.course.getId(), df.format(completionTimeSeconds)}), "parkour.play");
                            }
                            PlayerExperience playerXp = PlayerExperience.loadExperience(plugin.getCourseDatabase(), player);
                            try {
                                int courseXp = Integer.parseInt(sign.getLine(1));
                                Checkpoint cp = plugin.playerCheckpoints.get(player);
                                if (cp != null && cp.getCourse().getId() == endData.course.getId()) {
                                    courseXp = cp.getReducedExp(courseXp);
                                }
                                courseXp = highScore.getReducedXp(courseXp);
                                playerXp.setExperience(playerXp.getExperience() + courseXp);
                                playerXp.save(plugin.getCourseDatabase());
                                player.sendMessage(Parkour.getString("xp.gain", new Object[]{courseXp, playerXp.getExperience()}));
                            } catch (NumberFormatException | IndexOutOfBoundsException e) { // No XP gain for this course
                            }
                            plugin.playerCheckpoints.remove(player);
                            plugin.completedCourseTracker.put(player, endData);
                            player.setScoreboard(endData.course.getScoreboard(scores));
                            Duel duel = plugin.getDuel(player);
                            if (duel != null && duel.isAccepted() && duel.hasStarted()) {
                                duel.win(player, plugin);
                                plugin.activeDuels.remove(duel);
                                event.setTo(duel.getCourse().getTeleport());
                            }
                        }
                        break;
                    case "[vwall]":
                        if (!plugin.playerCourseTracker.containsKey(player)) {
                            event.setTo(player.getLocation().getBlock().getRelative(
                                    ((org.bukkit.material.Sign)sign.getData()).getFacing()).getLocation());
                        }
                        break;
                    case "[avwall]":
                        event.setTo(player.getLocation().getBlock().getRelative(
                                ((org.bukkit.material.Sign)sign.getData()).getFacing()).getLocation());
                        break;
                    case "[cancel]":
                        if (plugin.playerCourseTracker.containsKey(player)) {
                            PlayerCourseData cancelData = plugin.playerCourseTracker.remove(event.getPlayer());
                            cancelData.restoreState(event.getPlayer());
                            event.setTo(cancelData.course.getTeleport());
                            plugin.playerCheckpoints.remove(player);
                        }
                        break;
                    case "[tp]":
                        plugin.completedCourseTracker.remove(player);
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
                    case "[portal]":
                        try {
                            String[] xyz = sign.getLine(1).split(",");
                            int x = Integer.parseInt(xyz[0]);
                            int y = Integer.parseInt(xyz[1]);
                            int z = Integer.parseInt(xyz[2]);
                            World world = Bukkit.getWorld(sign.getLine(2));
                            if (world == null) {
                                world = player.getWorld();
                            }
                            Location loc = new Location(world, x, y, z);
                            player.teleport(loc);
                        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                        }
                        break;
                }
            }
        }
        if (plugin.playerCourseTracker.containsKey(player)) {
            int detection = plugin.playerCourseTracker.get(player).course.getDetection();
            if (detectBlocks(player.getLocation(), Material.BEDROCK, DETECTION_MIN, detection)) {
                PlayerCourseData data = plugin.playerCourseTracker.get(player);
                player.setFallDistance(0.0F);
                Checkpoint cp = plugin.playerCheckpoints.get(event.getPlayer());
                if (cp != null && cp.getCourse().getId() == data.course.getId()) {
                    ignoreTeleport = true;
                    event.setTo(cp.getLocation());
                    return;
                }
                plugin.playerCourseTracker.remove(player);
                data.restoreState(event.getPlayer());
                event.setTo(data.course.getTeleport());
            }
        } else if (plugin.completedCourseTracker.containsKey(player)) {
            int detection = plugin.completedCourseTracker.get(player).course.getDetection();
            if (detectBlocks(player.getLocation(), Material.BEDROCK, DETECTION_MIN, detection)) {
                player.setFallDistance(0.0F);
                event.setTo(plugin.completedCourseTracker.remove(player).course.getTeleport());
            }
        }
        Duel duel = plugin.getDuel(player);
        if (duel != null && duel.isAccepted() && !duel.hasStarted()) {
            event.setTo(duel.getCourse().getTeleport());
        }
    }

    private boolean detectBlocks(Location loc, Material type, int min, int max) {
        return this.getBlockInDepthRange(loc, type, min, max) != null;
    }

    /**
     * Get a block in a range below a certain location.
     *
     * @param loc Base location to search under.
     * @param type Type of block to look for.
     * @param min Starting depth.
     * @param max Maximum depth.
     * @return block matching type or null if not found.
     */
    private Block getBlockInDepthRange(Location loc, Material type, int min, int max) {
        for (int i = min; i <= max; i++) {
            Block block = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - i, loc.getBlockZ());
            if (block.getType() == type) {
                return block;
            }
        }
        return null;
    }

    @EventHandler
    public void onXpLevelChange(final PlayerExpChangeEvent event) {
        if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
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
                    plugin.completedCourseTracker.remove(event.getPlayer());
                    try {
                        int parkourNumber = Integer.parseInt(sign.getLine(1));
                        ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), parkourNumber);
                        event.setCancelled(true);
                        event.getPlayer().teleport(course.getTeleport());
                        event.getPlayer().sendMessage(Parkour.getString("course.teleport", new Object[]{course.getId()}));
                        List<PlayerHighScore> highScores = PlayerHighScore.loadHighScores(plugin.getCourseDatabase(), parkourNumber);
                        event.getPlayer().setScoreboard(course.getScoreboard(highScores));
                    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException ex) {
                    }
                    return;
                }
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getPlayer().getItemInHand().getType() == Material.ENDER_PEARL) {
                event.setCancelled(true);
                plugin.blindPlayers.remove(event.getPlayer());
                plugin.refreshVision(event.getPlayer());
                plugin.refreshHand(event.getPlayer());
                event.getPlayer().sendMessage(Parkour.getString("blind.disable", new Object[]{}));
            } else if (event.getPlayer().getItemInHand().getType() == Material.EYE_OF_ENDER) {
                event.setCancelled(true);
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
                event.getPlayer().teleport(plugin.getSpawn(), TeleportCause.COMMAND);
            } else if (event.getPlayer().getItemInHand().getType() == Material.STICK) {
                event.getPlayer().chat("/cp");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) throws SQLException {
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
        PlayerExperience playerXp = PlayerExperience.loadExperience(
                plugin.getCourseDatabase(), event.getPlayer());
        event.setFormat(Parkour.getString("xp.prefix", plugin.getLevel(playerXp
                .getExperience()), event.getPlayer().getName())
                + event.getFormat());
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) throws SQLException {
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
        if (!event.getPlayer().getInventory().contains(Material.STICK)) {
            event.getPlayer().getInventory().addItem(plugin.POINT);
        }
        if (!event.getPlayer().hasPermission("parkour.tpexempt")) {
            event.getPlayer().teleport(plugin.getSpawn());
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) throws SQLException {
        plugin.blindPlayers.remove(event.getPlayer());
        event.getPlayer().getInventory().remove(Material.ENDER_PEARL);
        plugin.deafPlayers.remove(event.getPlayer());
        plugin.playerCheckpoints.remove(event.getPlayer());
        plugin.completedCourseTracker.remove(event.getPlayer());
        if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
            plugin.playerCourseTracker.remove(event.getPlayer()).leave(event.getPlayer());
        }
        Duel duel = plugin.getDuel(event.getPlayer());
        if (duel != null) {
            duel.cancel(plugin, event.getPlayer());
            plugin.activeDuels.remove(duel);
        }
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) throws SQLException {
        plugin.blindPlayers.remove(event.getPlayer());
        event.getPlayer().getInventory().remove(Material.ENDER_PEARL);
        plugin.deafPlayers.remove(event.getPlayer());
        plugin.playerCheckpoints.remove(event.getPlayer());
        plugin.completedCourseTracker.remove(event.getPlayer());
        if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
            plugin.playerCourseTracker.remove(event.getPlayer()).leave(event.getPlayer());
        }
        Duel duel = plugin.getDuel(event.getPlayer());
        if (duel != null) {
            duel.cancel(plugin, event.getPlayer());
            plugin.activeDuels.remove(duel);
        }
    }

    boolean ignoreTeleport = false;
    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.COMMAND) {
            event.getPlayer().setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
        if (event.getTo().getWorld() != event.getFrom().getWorld()
                || event.getTo().distance(event.getFrom()) >= 3 && !ignoreTeleport) {
            Duel duel = plugin.getDuel(event.getPlayer());
            if (duel != null && duel.isAccepted() && duel.getCourse() != null) { //stopgap
                event.setTo(duel.getCourse().getTeleport());
                return;
            }
            plugin.completedCourseTracker.remove(event.getPlayer());
            if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
                plugin.playerCheckpoints.remove(event.getPlayer());
                plugin.playerCourseTracker.remove(event.getPlayer()).restoreState(event.getPlayer());
            }
        }
        ignoreTeleport = false;
    }

    @EventHandler
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equals("/spawn")) {
            event.getPlayer().setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
    }

    @EventHandler
    public void onPlayerDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.playerCourseTracker.containsKey(player)) {
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

    private class XpCounterTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Player player : plugin.playerCourseTracker.keySet()) {
                int secondsPassed = (int) ((System.currentTimeMillis() - plugin.playerCourseTracker.get(player).startTime) / 1000);
                float remainder = (int) ((System.currentTimeMillis() - plugin.playerCourseTracker.get(player).startTime) % 1000);
                float tenthsPassed = remainder / 1000F;
                player.setLevel(secondsPassed);
                player.setExp(tenthsPassed);
            }
        }
    }
}
