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

package me.cmastudios.mcparkour;

import me.cmastudios.mcparkour.data.Guild;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Utils {
    public final static Random RANDOM = new Random();

    public static void spawnRandomFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();
        int rt = RANDOM.nextInt(5) + 1;
        FireworkEffect.Type type = FireworkEffect.Type.BALL;
        switch (rt) {
            case 1:
                type = FireworkEffect.Type.BALL;
                break;
            case 2:
                type = FireworkEffect.Type.BURST;
                break;
            case 3:
                type = FireworkEffect.Type.CREEPER;
                break;
            case 4:
                type = FireworkEffect.Type.STAR;
                break;
            case 5:
                type = FireworkEffect.Type.BALL_LARGE;
                break;
        }
        FireworkEffect effect = FireworkEffect.builder().flicker(RANDOM.nextBoolean()).withColor(getRandomColor()).withFade(getRandomColor()).with(type).trail(RANDOM.nextBoolean()).build();
        fwm.addEffect(effect);
        fwm.setPower(0);
        fw.setFireworkMeta(fwm);
    }

    private static Color getRandomColor() {
        Color c = null;
        Random r = new Random();
        int i = r.nextInt(17) + 1;
        switch (i) {
            case 1:
                c = Color.AQUA;
                break;
            case 2:
                c = Color.BLACK;
                break;
            case 3:
                c = Color.BLUE;
                break;
            case 4:
                c = Color.FUCHSIA;
                break;
            case 5:
                c = Color.GRAY;
                break;
            case 6:
                c = Color.GREEN;
                break;
            case 7:
                c = Color.LIME;
                break;
            case 8:
                c = Color.MAROON;
                break;
            case 9:
                c = Color.NAVY;
                break;
            case 10:
                c = Color.OLIVE;
                break;
            case 11:
                c = Color.ORANGE;
                break;
            case 12:
                c = Color.PURPLE;
                break;
            case 13:
                c = Color.RED;
                break;
            case 14:
                c = Color.SILVER;
                break;
            case 15:
                c = Color.TEAL;
                break;
            case 16:
                c = Color.WHITE;
                break;
            case 17:
                c = Color.YELLOW;
                break;
        }
        return c;
    }

    public static boolean canUse(Plugin plugin, Player player, String cooldown, long seconds) {
        if (player.hasMetadata("parkour" + cooldown)) {
            for (MetadataValue val : player.getMetadata("parkour" + cooldown)) {
                if (val.getOwningPlugin() == plugin) {
                    if ((System.currentTimeMillis() - val.asLong()) / 1000 <= seconds) {
                        return false;
                    }
                }
            }
        }
        player.setMetadata("parkour" + cooldown, new FixedMetadataValue(plugin, System.currentTimeMillis()));
        return true;
    }

    public static SkullType getSkullFromDurability(short durability) {
        switch (durability) {
            case 1:
                return SkullType.WITHER;
            case 3:
                return SkullType.PLAYER;
            default:
                return SkullType.SKELETON;
        }
    }

    public static void broadcast(List<Player> list, String message) {
        for (Player recipient : list) {
            recipient.sendMessage(message);
        }
    }

    public static void removeEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType() == PotionEffectType.INVISIBILITY) {
                continue; //We don't want to remove vanish
            }
            player.removePotionEffect(effect.getType());
        }
    }

    public static OfflinePlayer getPlayerUUID(String playerName, Connection conn) throws SQLException {
        OfflinePlayer player = null;
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT `uuid` FROM `experience` WHERE `player` = ?")) {
            stmt.setString(1, playerName);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    player = Bukkit.getOfflinePlayer(UUID.fromString(result
                            .getString("uuid")));
                }
            }
        }
        Bukkit.getLogger().info(player.getName());
        return player;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
