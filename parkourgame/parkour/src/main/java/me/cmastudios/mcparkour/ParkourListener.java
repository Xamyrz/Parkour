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

import com.jeff_media.customblockdata.CustomBlockData;
import me.cmastudios.experience.IPlayerExperience;
import me.cmastudios.mcparkour.Parkour.PlayerCourseData;
import me.cmastudios.mcparkour.Parkour.PlayerTrackerData;
import me.cmastudios.mcparkour.data.EffectHead;
import me.cmastudios.mcparkour.data.Guild.GuildPlayer;
import me.cmastudios.mcparkour.data.Guild.GuildWar;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.ParkourCourse.CourseMode;
import me.cmastudios.mcparkour.event.configurations.OwnEndingEvent;
import me.cmastudios.mcparkour.event.configurations.SignConfigurableEvent;
import me.cmastudios.mcparkour.event.configurations.TimerableEvent;
import me.cmastudios.mcparkour.events.PlayerCancelParkourEvent;
import me.cmastudios.mcparkour.events.PlayerCompleteDuelEvent;
import me.cmastudios.mcparkour.menu.Menu;
import me.cmastudios.mcparkour.tasks.*;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Responds to Bukkit events for the Parkour plugin.
 *
 * @author Connor Monahan
 */
public class ParkourListener implements Listener {

    private final Parkour plugin;
    public static final int DETECTION_MIN = 1;
    public static final int SIGN_DETECTION_MAX = 6;

    private Scoreboard sb;
    private Team team;

    public ParkourListener(Parkour instance) {
        this.plugin = instance;
        plugin.getServer().getScheduler().runTaskTimer(plugin, new XpCounterTask(), 1L, 1L);
        sb = Bukkit.getScoreboardManager().getMainScoreboard();
        if(sb.getTeam("pk") == null){
            sb.registerNewTeam("pk");
        }

        team = sb.getTeam("pk");
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    //Nocheat is listening at LOWEST, lower value = faster callback
    public void onPlayerMove(final PlayerMoveEvent event) throws SQLException {
        final long now = System.currentTimeMillis();

        if (event.getFrom().getBlock() == event.getTo().getBlock()) {
            return;
        }

        Player player = event.getPlayer();

        if(plugin.playerTracker.get(player) == null) {
            plugin.playerTracker.put(player, new PlayerTrackerData());
        } else {
            plugin.playerTracker.get(player).packets++;
        }

        if (event.getTo().getBlockY() < 0 && !player.hasPermission("parkour.belowzero")) {
            player.teleport(plugin.getSpawn());
            plugin.completedCourseTracker.remove(player);
            plugin.playerCheckpoints.remove(player);
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            player.setScoreboard(sb);
        }

        Block below = this.detectBlocks(event.getTo(), Material.OAK_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX)
                ? this.getBlockInDepthRange(event.getTo(), Material.OAK_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX)
                : this.getBlockInDepthRange(event.getTo(), Material.OAK_WALL_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX);
        if (below != null && (below.getType() == Material.OAK_SIGN
                || below.getType() == Material.OAK_WALL_SIGN)) {
            Block belowfrom = this.detectBlocks(event.getFrom(), Material.OAK_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX)
                    ? this.getBlockInDepthRange(event.getFrom(), Material.OAK_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX)
                    : this.getBlockInDepthRange(event.getFrom(), Material.OAK_WALL_SIGN, DETECTION_MIN, SIGN_DETECTION_MAX);
            final Sign sign = (Sign) below.getState();
            final String controlLine = sign.getLine(0);
            if(belowfrom!=null&&(belowfrom.getType() == Material.OAK_SIGN
                    || belowfrom.getType() == Material.OAK_WALL_SIGN)&&((Sign) belowfrom.getState()).getLine(0).equals(controlLine)) {
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
            if (detection < 0) {
                if (!isJumpBlock(event.getTo(), player.getVelocity().getY())) {
                    if (playerFailedCourse(event, player)) return;
                }
            } else {
                if (detectBlocks(event.getTo(), Material.BEDROCK, DETECTION_MIN, detection)) {
                    if (playerFailedCourse(event, player)) return;
                }
            }
        } else if (plugin.completedCourseTracker.containsKey(player)) {
            int detection = plugin.completedCourseTracker.get(player).course.getDetection();
            if (detection < 0) {
                if (!isJumpBlock(event.getTo(), player.getVelocity().getY())) {
                    removePlayerTracker(event, player);
                }
            } else {
                if (detectBlocks(event.getTo(), Material.BEDROCK, DETECTION_MIN, detection)) {
                    removePlayerTracker(event, player);
                }
            }
        }
        Duel duel = plugin.getDuel(player);
        if (duel != null && duel.isAccepted() && !duel.hasStarted()) {
            event.setTo(duel.getCourse().getTeleport());
        }
        GuildWar war = plugin.getWar(player);
        if (war != null && war.hasStarted()) {
            Block potentialHead = this.getBlockInDepthRange(event.getTo(), Material.SKELETON_SKULL, 0, 1);
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

    private boolean playerFailedCourse(PlayerMoveEvent event, Player player) {
        PlayerCourseData data = plugin.playerCourseTracker.get(player);
        player.setFallDistance(0.0F);
        Checkpoint cp = plugin.playerCheckpoints.get(event.getPlayer());
        if (cp != null && cp.getCourse().getId() == data.course.getId()) {
            ignoreTeleport = true;
            event.setTo(cp.getLocation());
            return true;
        }
        Bukkit.getPluginManager().callEvent(new PlayerCancelParkourEvent(PlayerCancelParkourEvent.CancelReason.BED_ROCK, data, event.getPlayer()));
        plugin.playerCourseTracker.remove(player);
        data.restoreState(event.getPlayer());
        event.setTo(data.course.getTeleport());
        player.setVelocity(new Vector());
        return false;
    }

    private void removePlayerTracker(PlayerMoveEvent event, Player player) {
        player.setFallDistance(0.0F);
        event.setTo(plugin.completedCourseTracker.remove(player).course.getTeleport());
    }

    private boolean isJumpBlock(Location loc, Double playerVelocity) {
        Block block = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        PersistentDataContainer checkBlock = new CustomBlockData(block, plugin);
        Material blockType = block.getType();
        boolean notJumping = playerVelocity == -0.0784000015258789;

        if (checkBlock.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)) {
            return true;
        }

        if (!notJumping) {
            if (blockType == Material.WATER || blockType == Material.LAVA) {
                return false;
            }
            if (blockType.isAir()) {
                return true;
            }
        }

        if (notJumping && (blockType == Material.AIR || blockType == Material.WATER || blockType == Material.LAVA)) {
            Block headBlock = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + 2, loc.getBlockZ());
            if (!headBlock.getType().isAir()) {
                return true;
            } else {
                PersistentDataContainer checkBlock1 = new CustomBlockData(headBlock, plugin);
                if (checkBlock1.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)) {
                    return true;
                }
            }
            for (int x = loc.getBlockX() - 1; x <= loc.getBlockX() + 1; x++) {
                for (int z = loc.getBlockZ() - 1; z <= loc.getBlockZ() + 1; z++) {
                    PersistentDataContainer checkBlock2 = new CustomBlockData(loc.getWorld().getBlockAt(x, loc.getBlockY() - 1, z), plugin);
                    if (checkBlock2.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
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
        Menu playerMenus = plugin.playersMenus.get(event.getWhoClicked());
        if (!event.getInventory().equals(playerMenus.getSettingsMenu()) && !event.getInventory().equals(playerMenus.getChooseMenu())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
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
        } else if (Item.CLOCK.isSimilar(event.getCurrentItem())) {
            if (player.hasMetadata("clockSwitch")){
                int playerTime =  player.getMetadata("clockSwitch").get(0).asInt()+1;
                switch (playerTime){
                    case 1: player.setPlayerTime(6000L, false);
                            player.sendMessage(Parkour.getString("clock.noon"));
                            break;

                    case 2: player.setPlayerTime(12000L, false);
                            player.sendMessage(Parkour.getString("clock.sunset"));
                            break;

                    case 3: player.setPlayerTime(13000L, false);
                            player.sendMessage(Parkour.getString("clock.night"));
                            break;

                    case 4: player.setPlayerTime(18000L, false);
                            player.sendMessage(Parkour.getString("clock.midnight"));
                            playerTime = 0;
                            break;

                }
                player.setMetadata("clockSwitch", new FixedMetadataValue(plugin, playerTime));
                return;
            }else{
                player.setPlayerTime(6000L, false);
                player.sendMessage(Parkour.getString("clock.noon"));
                player.setMetadata("clockSwitch", new FixedMetadataValue(plugin, 1));
            }
        } else if (Item.VISION_USED.isSimilar(event.getCurrentItem())) {
            plugin.blindPlayers.remove(player);
            plugin.refreshVision(player);
            event.getClickedInventory().remove(event.getCurrentItem());
            event.getClickedInventory().addItem(Item.VISION.getItem());
            player.sendMessage(Parkour.getString("blind.disable"));
        } else if (Item.VISION.isSimilar(event.getCurrentItem())) {
            plugin.blindPlayers.remove(player);
            plugin.blindPlayers.add(player);
            plugin.refreshVision(player);
            event.getClickedInventory().remove(event.getCurrentItem());
            event.getClickedInventory().addItem(Item.VISION_USED.getItem());
            player.sendMessage(Parkour.getString("blind.enable"));
        } else {
            event.setCancelled(true);
            if(event.getSlot()==-1) {
                return;
            }
            plugin.playersMenus.get(player).getChooseMenuData().handleClick(event.getInventory(),event.getCurrentItem(), event.getSlot(),plugin,player);
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) throws SQLException {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {

            if(event.getPlayer().hasMetadata("setJumps")){
                event.setCancelled(true);
                plugin.jumpBlocks.editJumpBlocks(event);
                return;
            }

            if (!event.hasItem()) {
                return;
            }

            Player player = event.getPlayer();
            if (Item.SPAWN.isSimilar(event.getItem())) {
                event.setCancelled(true);
                player.teleport(plugin.getSpawn(), TeleportCause.COMMAND);
            } else if (Item.SETTINGS.isSimilar(event.getItem())) {
                event.setCancelled(true);
                ArrayList<Item> items = Item.getItemsByType(Item.ItemType.SETTINGS);
                Inventory playerSettings = plugin.playersMenus.get(event.getPlayer()).getSettingsMenu();
                if(playerSettings.isEmpty()) {
                    for (Item item : items) {
                        playerSettings.addItem(item.getItem());
                    }
                }
                player.openInventory(playerSettings);
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
                    event.getPlayer().sendMessage(Parkour.getString("choosemenu.delay"));
                    return;
                }
                plugin.playersMenus.get(player).renderChooseMenu();
                Inventory playerChooseMenu = plugin.playersMenus.get(player).getChooseMenu();
                event.getPlayer().openInventory(playerChooseMenu);
//                this.getChooseMenuData(event.getPlayer()).render(chooseMenu,event.getPlayer(),plugin);
//                event.getPlayer().openInventory(chooseMenu);
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
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BURP, 10, 0);
        }
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
        team.addEntry(event.getPlayer().getName());
        event.getPlayer().setScoreboard(sb);
        Utils.savePlayer(plugin.getCourseDatabase(), event.getPlayer());
        plugin.playersMenus.put(event.getPlayer(), new Menu(event.getPlayer(), plugin));
        if (plugin.getEvent() != null && plugin.getEvent().hasStarted()) {
            if (Parkour.isBarApiEnabled) {
                //plugin.getEvent().bar.setVisible(true);
                //plugin.getEvent().bar.setTitle(Parkour.getString("event.started", Parkour.getString(plugin.getEvent().getCourse().getType().getNameKey())));
            } else {
                event.getPlayer().sendMessage(Parkour.getString("event.started", Parkour.getString(plugin.getEvent().getCourse().getType().getNameKey())));
            }
        }

        plugin.refreshVision(event.getPlayer());
        plugin.refreshVisionOfOnePlayer(event.getPlayer());

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

        //VIP items not needed for time being
//        if (event.getPlayer().hasPermission("parkour.vip")) {
//            for (Item item : Item.getItemsByType(Item.ItemType.VIP)) {
//                if (!event.getPlayer().getInventory().contains(item.getItem().getType()) || (event.getPlayer().getInventory().contains(item.getItem().getType()) && event.getPlayer().getInventory().all(item.getItem()).isEmpty())) {
//                    switch (item) {
//                        default -> event.getPlayer().getInventory().addItem(item.getItem());
//                    }
//                }
//            }
//        }

        if (!event.getPlayer().hasPermission("parkour.tpexempt")) {
            event.getPlayer().teleport(plugin.getSpawn());
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new GuildRejoinHandling(event.getPlayer(), plugin));
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) throws SQLException {
        event.setQuitMessage("");
        plugin.playersMenus.remove(event.getPlayer());
        plugin.blindPlayers.remove(event.getPlayer());
        plugin.deafPlayers.remove(event.getPlayer());
        plugin.playerCheckpoints.remove(event.getPlayer());
        plugin.guildChat.remove(event.getPlayer());
        plugin.completedCourseTracker.remove(event.getPlayer());
        plugin.vanishedPlayers.remove(event.getPlayer());
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
        event.setLeaveMessage("");
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
                event.getPlayer().playSound(event.getBlock().getLocation(), Sound.BLOCK_ANVIL_BREAK, 10, 1); // Confirmation
            } else {
                event.getPlayer().sendMessage(Parkour.getString("sign.noperms"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) throws SQLException {
        if (event.getBlock().getType() == Material.SKELETON_SKULL
                && event.getItemInHand().getItemMeta().hasDisplayName()
                && event.getPlayer().hasPermission("parkour.set")) {
            try {
                int courseId = Integer.parseInt(event.getItemInHand().getItemMeta().getDisplayName());
                event.setCancelled(true);
                ParkourCourse course = plugin.courses.get(courseId);
                Validate.notNull(course, Parkour.getString("error.course404"));
                Validate.isTrue(course.getMode() == CourseMode.GUILDWAR, Parkour.getString("error.coursewar"));
                Validate.isTrue(event.getBlock().getState() instanceof Skull); // assert
                SkullType type = Utils.getSkullFromDurability(event.getItemInHand().getDurability());
                Validate.notNull(type); // assert
                EffectHead head = new EffectHead(event.getBlock().getLocation(), course, type);
                head.save(plugin.getCourseDatabase());
                head.setBlock(plugin);
                event.setCancelled(false);
                event.getPlayer().playSound(event.getBlock().getLocation(), Sound.BLOCK_ANVIL_USE, 10, 1); // Confirmation
            } catch (NumberFormatException ignored) { // Why a skull decoration would have a custom name, I don't know
            } catch (Exception ex) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + ex.toString());
            }
        }
    }

    private class XpCounterTask implements Runnable {
        @Override
        public void run() {
            for (Player player : plugin.playerTracker.keySet()) {
                PlayerTrackerData playerTracker = plugin.playerTracker.get(player);
                PlayerCourseData playerCourseTracker = plugin.playerCourseTracker.get(player);

                if (playerTracker.lagged && playerCourseTracker != null) {
                    if (!playerCourseTracker.lagged) {
                        player.sendMessage(Parkour.getString("anti.lag.notify"));
                        playerCourseTracker.lagged = true;
                    }
                }

                playerTracker.lagged = false;

                if (playerTracker.packets > 6) {
                    playerTracker.lagged = true;
                }

                if (playerCourseTracker != null) {

                    if (playerCourseTracker.course.getMode() == CourseMode.GUILDWAR || (playerCourseTracker.course.getMode() == CourseMode.EVENT && plugin.getEvent() != null && plugin.getEvent() instanceof TimerableEvent)) {
                        continue;
                    }

                    int secondsPassed = (int) ((System.currentTimeMillis() - playerCourseTracker.startTime) / 1000);
                    float remainder = (int) ((System.currentTimeMillis() - playerCourseTracker.startTime) % 1000);
                    float tenthsPassed = remainder / 1000F;
                    player.setLevel(secondsPassed);
                    player.setExp(tenthsPassed);
                }
                playerTracker.packets = 0;
            }

        }
    }
}