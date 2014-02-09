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

import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour.PlayerCourseData;
import me.cmastudios.mcparkour.data.*;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;
import me.cmastudios.mcparkour.event.configurations.OwnEndingEvent;
import me.cmastudios.mcparkour.event.configurations.SignConfigurableEvent;
import me.cmastudios.mcparkour.event.configurations.TimerableEvent;
import me.cmastudios.mcparkour.events.*;
import me.cmastudios.mcparkour.tasks.*;
import me.confuser.barapi.BarAPI;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/**
 * Responds to Bukkit events for the Parkour plugin.
 *
 * @author Connor Monahan
 */
public class ParkourListener implements Listener {

    private final Parkour plugin;
    public static final int DETECTION_MIN = 1;
    public static final int SIGN_DETECTION_MAX = 6;

    public ParkourListener(Parkour instance) {
        this.plugin = instance;
        plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(), 1L, 1L);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    //Nocheat is listening at LOWEST, lower value = faster callback
    public void onPlayerMove(final PlayerMoveEvent event) throws SQLException {
        final long now = System.currentTimeMillis();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        Block below = this.detectBlocks(event.getTo(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX)
                ? this.getBlockInDepthRange(event.getTo(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX)
                : this.getBlockInDepthRange(event.getTo(), Material.WALL_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX);
        if (below != null && (below.getType() == Material.SIGN_POST
                || below.getType() == Material.WALL_SIGN)) {
            Block belowfrom = this.detectBlocks(event.getFrom(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX)
                    ? this.getBlockInDepthRange(event.getFrom(), Material.SIGN_POST, DETECTION_MIN, SIGN_DETECTION_MAX)
                    : this.getBlockInDepthRange(event.getFrom(), Material.WALL_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX);
            final Sign sign = (Sign) below.getState();
            final String controlLine = sign.getLine(0);
            if(belowfrom!=null&&(belowfrom.getType() == Material.SIGN_POST
                    || belowfrom.getType() == Material.WALL_SIGN)&&((Sign) belowfrom.getState()).getLine(0).equals(controlLine)) {
                return;
            }
            switch (controlLine) {
                case "[start]":
                    plugin.completedCourseTracker.remove(player);
                    plugin.playerCheckpoints.remove(player);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new ParkourStartTask(sign, player, plugin, now));
                    break;
                case "[event]":
                    if (plugin.getEvent() != null && plugin.getEvent().hasStarted() && plugin.getEvent() instanceof SignConfigurableEvent) {
                        ((SignConfigurableEvent) plugin.getEvent()).handleEventSign(player, sign);
                    }
                    break;
                case "[end]":
                    if (plugin.playerCourseTracker.containsKey(player)) {

                        try {
                            int courseCons = Integer.parseInt(sign.getLine(2));
                            if (plugin.playerCourseTracker.get(player).course.getId() != courseCons) {
                                return;
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) { // No course constraint
                        }

                        PlayerCourseData endData = plugin.playerCourseTracker.remove(player); // They have ended their course anyhow
                        endData.restoreState(player);
                        long completionTime = now - endData.startTime;
                        plugin.playerCheckpoints.remove(player);
                        plugin.completedCourseTracker.put(player, endData);
                        if (endData.course.getMode() == CourseMode.EVENT && plugin.getEvent() != null && plugin.getEvent() instanceof OwnEndingEvent) {
                            ((OwnEndingEvent) plugin.getEvent()).handleEnding(player, completionTime, endData);
                            return;
                        }
                        Duel duel = plugin.getDuel(player);
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new ParkourCompleteTask(player, plugin, endData, completionTime, duel != null && duel.hasStarted(), sign.getLine(1)));
                        if (duel != null && duel.isAccepted() && duel.hasStarted()) {
                            duel.win(player, plugin);
                            plugin.activeDuels.remove(duel);
                            event.setTo(duel.getCourse().getTeleport());
                            Bukkit.getPluginManager().callEvent(new PlayerCompleteDuelEvent(duel, player, completionTime));
                        }
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new GuildFinishHandling(plugin, player, now));
                    }
                    break;
                case "[cancel]":
                    if (plugin.playerCourseTracker.containsKey(player)) {
                        PlayerCourseData cancelData = plugin.playerCourseTracker.remove(event.getPlayer());
                        Bukkit.getPluginManager().callEvent(new PlayerCancelParkourEvent(PlayerCancelParkourEvent.CancelReason.SIGN, cancelData, event.getPlayer()));
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
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new TeleportToCourseTask(plugin, player, TeleportCause.PLUGIN, tpParkourId));
                    break;
                case "[portal]":
                    try {
                        double x = Double.parseDouble(sign.getLine(1));
                        double z = Double.parseDouble(sign.getLine(2));
                        String[] yworld = sign.getLine(3).split(" ");
                        double y = Double.parseDouble(yworld[0]);
                        String sWorld = yworld[1];
                        World world = Bukkit.getWorld(sWorld);
                        if (world == null) {
                            world = player.getWorld();
                        }
                        Location loc = new Location(world, x, y, z);
                        player.teleport(loc);
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    }
                    break;
            }
        }
        if (plugin.playerCourseTracker.containsKey(player)) {
            int detection = plugin.playerCourseTracker.get(player).course.getDetection();
            if (detectBlocks(event.getTo(), Material.BEDROCK, DETECTION_MIN, detection)) {
                PlayerCourseData data = plugin.playerCourseTracker.get(player);
                player.setFallDistance(0.0F);
                Checkpoint cp = plugin.playerCheckpoints.get(event.getPlayer());
                if (cp != null && cp.getCourse().getId() == data.course.getId()) {
                    ignoreTeleport = true;
                    event.setTo(cp.getLocation());
                    return;
                }
                Bukkit.getPluginManager().callEvent(new PlayerCancelParkourEvent(PlayerCancelParkourEvent.CancelReason.BED_ROCK, data, event.getPlayer()));
                plugin.playerCourseTracker.remove(player);
                data.restoreState(event.getPlayer());
                event.setTo(data.course.getTeleport());
                player.setVelocity(new Vector());
            }
        } else if (plugin.completedCourseTracker.containsKey(player)) {
            int detection = plugin.completedCourseTracker.get(player).course.getDetection();
            if (detectBlocks(event.getTo(), Material.BEDROCK, DETECTION_MIN, detection)) {
                player.setFallDistance(0.0F);
                event.setTo(plugin.completedCourseTracker.remove(player).course.getTeleport());
            }
        }
        Duel duel = plugin.getDuel(player);
        if (duel != null && duel.isAccepted() && !duel.hasStarted()) {
            event.setTo(duel.getCourse().getTeleport());
        }
        GuildWar war = plugin.getWar(player);
        if (war != null && war.hasStarted()) {
            Block potentialHead = this.getBlockInDepthRange(event.getTo(), Material.SKULL, 0, 1);
            if (potentialHead != null && potentialHead.hasMetadata("mcparkour-head")) {
                List<MetadataValue> metadata = potentialHead.getMetadata("mcparkour-head");
                Validate.notEmpty(metadata); // assert
                Validate.notNull(metadata.get(0)); // assert
                Validate.isTrue(metadata.get(0).value() instanceof EffectHead); // assert
                EffectHead head = (EffectHead) metadata.get(0).value();
                potentialHead.setType(Material.AIR);
                event.getPlayer().getInventory().addItem(head.getPotion());
            }
        }
    }

    private boolean detectBlocks(Location loc, Material type, int min, int max) {
        return this.getBlockInDepthRange(loc, type, min, max) != null;
    }

    /**
     * Get a block in a range below a certain location.
     *
     * @param loc  Base location to search under.
     * @param type Type of block to look for.
     * @param min  Starting depth.
     * @param max  Maximum depth.
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
    public void onInventoryClick(final InventoryClickEvent event) throws SQLException {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory().getName().equals(Parkour.getString("settings.inventory.name"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            final Player player = (Player) event.getWhoClicked();
            if (Item.CHAT.isSimilar(event.getCurrentItem())) {
                synchronized (plugin.deafPlayers) {
                    if (plugin.deafPlayers.contains(player)) {
                        plugin.deafPlayers.remove(player);
                        player.sendMessage(Parkour.getString("deaf.disable", new Object[]{}));
                    } else {
                        plugin.deafPlayers.add(player);
                        player.sendMessage(Parkour.getString("deaf.enable", new Object[]{}));
                    }
                }
            } else if (Item.SCOREBOARD.isSimilar(event.getCurrentItem())) {
                event.setCancelled(true);
                if (player.hasMetadata("disableScoreboard")) {
                    if (player.getMetadata("disableScoreboard").get(0).asBoolean()) {
                        player.sendMessage(Parkour.getString("scoreboard.enable"));
                        if (plugin.playerCourseTracker.containsKey(event.getWhoClicked())) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, new DisplayHighscoresTask(plugin, player, plugin.playerCourseTracker.get(event.getWhoClicked()).course));
                        }
                        player.removeMetadata("disableScoreboard", plugin);
                        return;
                    }
                }
                player.setMetadata("disableScoreboard", new FixedMetadataValue(plugin, true));
                player.sendMessage(Parkour.getString("scoreboard.disable"));
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        } else if(event.getInventory().getName().equals(Parkour.getString("choosemenu.title"))) {
            event.setCancelled(true);
            final Player player = (Player) event.getWhoClicked();
            if(event.getSlot()==-1) {
                return;
            }
            if (event.getCurrentItem() == null) {
                return;
            }
            this.getChooseMenuData(player).handleClick(event.getInventory(),event.getCurrentItem(),plugin,player);
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
                        event.setCancelled(true);
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new TeleportToCourseTask(plugin, event.getPlayer(), TeleportCause.PLUGIN, parkourNumber));
                    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException ignored) {
                    }
                    return;
                }
            }
            if (plugin.playerCourseTracker.containsKey(event.getPlayer()) && (event.getClickedBlock().getType() == Material.ANVIL || event.getClickedBlock().getType() == Material.ENCHANTMENT_TABLE)) {
                event.setCancelled(true);
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!event.hasItem()) {
                return;
            }
            Player player = event.getPlayer();
            if (Item.VISION_USED.isSimilar(event.getItem())) {
                event.setCancelled(true);
                plugin.blindPlayers.remove(player);
                plugin.refreshVision(player);
                if (player.getItemInHand().isSimilar(event.getItem())) { //Should be always true
                    player.setItemInHand(Item.VISION.getItem());
                } else {
                    player.getInventory().remove(event.getItem());
                    player.getInventory().addItem(Item.VISION.getItem());
                }
                player.sendMessage(Parkour.getString("blind.disable"));
            } else if (Item.VISION.isSimilar(event.getItem())) {
                event.setCancelled(true);
                plugin.blindPlayers.remove(event.getPlayer());
                plugin.blindPlayers.add(event.getPlayer());
                plugin.refreshVision(event.getPlayer());
                player.sendMessage(Parkour.getString("blind.enable"));
                if (player.getItemInHand().isSimilar(event.getItem())) {
                    player.setItemInHand(Item.VISION_USED.getItem());
                } else {
                    player.getInventory().remove(event.getItem());
                    player.getInventory().addItem(Item.VISION_USED.getItem());
                }
            } else if (Item.SPAWN.isSimilar(event.getItem())) {
                event.setCancelled(true);
                player.teleport(plugin.getSpawn(), TeleportCause.COMMAND);
            } else if (Item.SETTINGS.isSimilar(event.getItem())) {
                event.setCancelled(true);
                ArrayList<Item> items = Item.getItemsByType(Item.ItemType.SETTINGS);
                Inventory inv = Bukkit.createInventory(event.getPlayer(), 9, Parkour.getString("settings.inventory.name"));
                for (Item item : items) {
                    inv.addItem(item.getItem());
                }
                player.openInventory(inv);
            } else if (Item.POINT.isSimilar(event.getItem())) {
                event.setCancelled(true);
                if (player.isSneaking() && plugin.playerCourseTracker.containsKey(event.getPlayer())) {
                    if (plugin.playerCheckpoints.containsKey(event.getPlayer())) {
                        plugin.playerCheckpoints.remove(event.getPlayer());
                        PlayerCourseData data = plugin.playerCourseTracker.remove(event.getPlayer());
                        player.teleport(data.course.getTeleport());
                        data.restoreState(event.getPlayer());
                        player.sendMessage(Parkour.getString("checkpoint.deleted"));
                    } else {
                        player.sendMessage(Parkour.getString("checkpoint.notset"));
                    }
                    return;
                }
                player.performCommand("cp");
            } else if (Item.ITEM_MENU.isSimilar(event.getItem())) {
                event.setCancelled(true);
                if (!Utils.canUse(plugin, event.getPlayer(), "choosemenu", 1)) {
                    player.sendMessage(Parkour.getString("choosemenu.delay"));
                    return;
                }
                Inventory inv = Bukkit.createInventory(event.getPlayer(),54,Parkour.getString("choosemenu.title"));
                this.getChooseMenuData(player).render(inv,player,plugin);
                player.openInventory(inv);
                return;
            } else if (Item.FIREWORK_SPAWNER.isSimilar(event.getItem())) {
                event.setCancelled(true);
                if (plugin.playerCourseTracker.get(event.getPlayer()) != null) {
                    player.sendMessage(Parkour.getString("firework.incourse"));
                    return;
                }
                if (!Utils.canUse(plugin, event.getPlayer(), "firework", 5)) {
                    player.sendMessage(Parkour.getString("firework.delay"));
                    return;
                }
                Utils.spawnRandomFirework(player.getLocation());
            }
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
                && event.getPlayer().getItemInHand().getType() == Material.POTION) {
            event.setCancelled(true);
            PotionMeta potion = (PotionMeta) event.getPlayer().getItemInHand().getItemMeta();
            event.getPlayer().addPotionEffects(potion.getCustomEffects());
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BURP, 10, 0);
        }
    }

    private ParkourChooseMenu getChooseMenuData(Player player) {
        if(player.hasMetadata("choosemenu")) {
            for(MetadataValue value : player.getMetadata("choosemenu")) {
                if(value.getOwningPlugin()==plugin) {
                    return (ParkourChooseMenu) value.value();
                }
            }
        }
        ParkourChooseMenu menu = new ParkourChooseMenu();
        player.setMetadata("choosemenu",new FixedMetadataValue(plugin,menu));
        return menu;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(final AsyncPlayerChatEvent event) throws SQLException {
        synchronized (plugin.deafPlayers) {
            for (Iterator<Player> it = event.getRecipients().iterator(); it.hasNext(); ) {
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
        if (plugin.guildChat.containsKey(event.getPlayer())) {
            GuildPlayer gp = plugin.guildChat.get(event.getPlayer());
            try {
                event.getRecipients().clear();
                event.getRecipients().addAll(GuildPlayer.getPlayers(gp.getGuild().getPlayers(plugin.getCourseDatabase())));
                event.setFormat(Parkour.getString("guild.chat.format", gp.getRank().toString(), event.getPlayer().getName(), event.getMessage()));
                return;
            } catch (Exception e) {
                event.setCancelled(true);
            }
            return;
        }
        IPlayerExperience playerXp = Parkour.experience.getPlayerExperience(event.getPlayer());
        GuildPlayer playerGp = GuildPlayer.loadGuildPlayer(plugin.getCourseDatabase(), event.getPlayer());
        String prefix = Parkour.getString("xp.prefix", Parkour.experience.getLevel(playerXp.getExperience()));
        if (playerGp.inGuild()) {
            prefix += Parkour.getString("guild.chat.prefix", playerGp.getGuild().getTag());
        }
        event.setFormat(prefix + event.getFormat());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(final PlayerJoinEvent event) throws SQLException {
        event.setJoinMessage(null);
        if (plugin.getEvent() != null && plugin.getEvent().hasStarted()) {
            if (Parkour.isBarApiEnabled) {
                BarAPI.setMessage(event.getPlayer(), Parkour.getString("event.started", Parkour.getString(plugin.getEvent().getCourse().getType().getNameKey())));
            } else {
                event.getPlayer().sendMessage(Parkour.getString("event.started", Parkour.getString(plugin.getEvent().getCourse().getType().getNameKey())));
            }
        }

        for (Player blindPlayer : plugin.blindPlayers) {
            plugin.refreshVision(blindPlayer);
        }

        for (ItemStack is : event.getPlayer().getInventory().all(Item.VISION_USED.getItem().getType()).values()) {
            if (Item.VISION_USED.isSimilar(is)) {
                event.getPlayer().getInventory().remove(is);
                break;
            }
        }

        parent:
        for (Item item : Item.getItemsByType(Item.ItemType.SPAWN)) {
            if (!event.getPlayer().getInventory().contains(item.getItem().getType())) {
                if (event.getPlayer().getInventory().contains(item.getItem().getType())) {
                    for (Map.Entry<Integer, ? extends ItemStack> entry : event.getPlayer().getInventory().all(item.getItem().getType()).entrySet()) {
                        if (item.isSimilar(entry.getValue())) {
                            continue parent;
                        }
                    }
                }
                event.getPlayer().getInventory().addItem(item.getItem());
            }
        }

        if (event.getPlayer().hasPermission("parkour.vip")) {
            for (Item item : Item.getItemsByType(Item.ItemType.VIP)) {
                if (!event.getPlayer().getInventory().contains(item.getItem().getType()) || (event.getPlayer().getInventory().contains(item.getItem().getType()) && event.getPlayer().getInventory().all(item.getItem()).isEmpty())) {
                    switch (item) {
                        case HELMET:
                            event.getPlayer().getInventory().setHelmet(item.getItem());
                            break;
                        case CHESTPLATE:
                            event.getPlayer().getInventory().setChestplate(item.getItem());
                            break;
                        case LEGGINGS:
                            event.getPlayer().getInventory().setLeggings(item.getItem());
                            break;
                        case BOOTS:
                            event.getPlayer().getInventory().setBoots(item.getItem());
                            break;
                        default:
                            event.getPlayer().getInventory().addItem(item.getItem());
                            break;
                    }
                }
            }
        }

        if (!event.getPlayer().hasPermission("parkour.tpexempt")) {
            event.getPlayer().teleport(plugin.getSpawn());
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new GuildRejoinHandling(event.getPlayer(), plugin));
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) throws SQLException {
        event.setQuitMessage(null);
        plugin.blindPlayers.remove(event.getPlayer());
        event.getPlayer().getInventory().remove(Material.ENDER_PEARL);
        plugin.deafPlayers.remove(event.getPlayer());
        plugin.playerCheckpoints.remove(event.getPlayer());
        plugin.guildChat.remove(event.getPlayer());
        plugin.completedCourseTracker.remove(event.getPlayer());
        if (plugin.blindPlayerExempts.containsKey(event.getPlayer())) {
            plugin.blindPlayerExempts.remove(event.getPlayer());
        }
        for (List<Player> pl : plugin.blindPlayerExempts.values()) {
            if (pl.contains(event.getPlayer())) {
                pl.remove(event.getPlayer());
            }
        }
        if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
            PlayerCourseData courseData = plugin.playerCourseTracker.remove(event.getPlayer());
            courseData.leave(event.getPlayer());
            Bukkit.getPluginManager().callEvent(new PlayerCancelParkourEvent(PlayerCancelParkourEvent.CancelReason.LEAVE, courseData, event.getPlayer()));
        }
        Duel duel = plugin.getDuel(event.getPlayer());
        if (duel != null) {
            duel.cancel(plugin, event.getPlayer());
            plugin.activeDuels.remove(duel);
        }
        GuildWar war = plugin.getWar(event.getPlayer());
        if (war != null) {
            GuildPlayer gp = war.getPlayer(event.getPlayer());
            war.handleDisconnect(gp, plugin);
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        event.setLeaveMessage(null);
    }

    @EventHandler
    public void onParkourCancel(PlayerCancelParkourEvent event) {
        Utils.removeEffects(event.getPlayer());
    }

    boolean ignoreTeleport = false;
    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.COMMAND) {
            event.getPlayer().setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
        if ((event.getTo().getWorld() != event.getFrom().getWorld()
                || event.getTo().distance(event.getFrom()) >= 6 || event.getCause() == TeleportCause.COMMAND) && !ignoreTeleport && !event.getPlayer().hasPermission("parkour.ignoreteleport")) {
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
        if (plugin.playerCourseTracker.containsKey(event.getPlayer())) {
            switch (event.getMessage().split(" ")[0]) {
                case "/spectate":
                case "/spec":
                case "/pkpodglad":
                    event.setCancelled(true);
                    break;
            }
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

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("mcparkour-head")) {
            if (event.getPlayer().hasPermission("parkour.set")) {
                try {
                    List<MetadataValue> metadata = event.getBlock().getMetadata("mcparkour-head");
                    Validate.notEmpty(metadata); // assert
                    Validate.notNull(metadata.get(0)); // assert
                    Validate.isTrue(metadata.get(0).value() instanceof EffectHead); // assert
                    EffectHead head = (EffectHead) metadata.get(0).value();
                    head.delete(plugin);
                    event.getBlock().removeMetadata("mcparkour-head", plugin);
                } catch (Exception ex) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + ex.toString());
                }
                event.getPlayer().playSound(event.getBlock().getLocation(), Sound.ANVIL_BREAK, 10, 1); // Confirmation
            } else {
                event.getPlayer().sendMessage(Parkour.getString("sign.noperms"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) throws SQLException {
        if (event.getBlock().getType() == Material.SKULL
                && event.getItemInHand().getItemMeta().hasDisplayName()
                && event.getPlayer().hasPermission("parkour.set")) {
            try {
                int courseId = Integer.parseInt(event.getItemInHand().getItemMeta().getDisplayName());
                event.setCancelled(true);
                ParkourCourse course = ParkourCourse.loadCourse(plugin.getCourseDatabase(), courseId);
                Validate.notNull(course, Parkour.getString("error.course404"));
                Validate.isTrue(course.getMode() == CourseMode.GUILDWAR, Parkour.getString("error.coursewar"));
                Validate.isTrue(event.getBlock().getState() instanceof Skull); // assert
                SkullType type = Utils.getSkullFromDurability(event.getItemInHand().getDurability());
                Validate.notNull(type); // assert
                EffectHead head = new EffectHead(event.getBlock().getLocation(), course, type);
                head.save(plugin.getCourseDatabase());
                head.setBlock(plugin);
                event.setCancelled(false);
                event.getPlayer().playSound(event.getBlock().getLocation(), Sound.ANVIL_USE, 10, 1); // Confirmation
            } catch (NumberFormatException ignored) { // Why a skull decoration would have a custom name, I don't know
            } catch (Exception ex) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + ex.toString());
            }
        }
    }

    private class XpCounterTask extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : plugin.playerCourseTracker.keySet()) {
                if (plugin.playerCourseTracker.get(player).course.getMode() == CourseMode.GUILDWAR || (plugin.playerCourseTracker.get(player).course.getMode() == CourseMode.EVENT && plugin.getEvent() != null && plugin.getEvent() instanceof TimerableEvent)) {
                    continue;
                }
                int secondsPassed = (int) ((System.currentTimeMillis() - plugin.playerCourseTracker.get(player).startTime) / 1000);
                float remainder = (int) ((System.currentTimeMillis() - plugin.playerCourseTracker.get(player).startTime) % 1000);
                float tenthsPassed = remainder / 1000F;
                player.setLevel(secondsPassed);
                player.setExp(tenthsPassed);
            }
        }
    }
}
