/*
 * Copyright (C) 2013 Maciej Mionskowski
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

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Items {
    VISION(Material.EYE_OF_ENDER, Parkour.getString("item.vision"), 1, (short) 0),
    CHAT(Material.PAPER, Parkour.getString("item.chat"), 1, (short) 0),
    SPAWN(Material.NETHER_STAR, Parkour.getString("item.spawn"), 1, (short) 0),
    POINT(Material.STICK, Parkour.getString("item.point"), 1, (short) 0, Parkour.getString("item.point.description.0"), Parkour.getString("item.point.description.1"), Parkour.getString("item.point.description.2")),
    HELMET(Material.GOLD_HELMET, 1, (short) 0, (new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }})),
    CHESTPLATE(Material.GOLD_CHESTPLATE, 1, (short) 0),
    LEGGINGS(Material.GOLD_LEGGINGS, 1, (short) 0),
    BOOTS(Material.GOLD_BOOTS, 1, (short) 0),
    FIREWORK_SPAWNER(Material.FIREWORK, Parkour.getString("item.firework"), 1, (short) 0),
    SCOREBOARD(Material.BOOK, Parkour.getString("item.scoreboard"), 1, (short) 0),
    FAVORITES(Material.EMERALD, Parkour.getString("favorites.item.base"), 1, (short) 0, Parkour.getString("favorites.item.base.lore0")),
    NEXT_PAGE(Material.ACTIVATOR_RAIL, Parkour.getString("favorites.item.next"), 1, (short) 0),
    PREV_PAGE(Material.RAILS, Parkour.getString("favorites.item.prev"), 1, (short) 0),
    EASY(Material.MINECART, 1, (short) 0),
    MEDIUM(Material.STORAGE_MINECART, 1, (short) 0),
    HIDDEN(Material.HOPPER_MINECART, 1, (short) 0),
    HARD(Material.POWERED_MINECART, 1, (short) 0),
    V_HARD(Material.EXPLOSIVE_MINECART, 1, (short) 0),
    THEMATIC(Material.BOAT, 1, (short) 0),
    ADVENTURE(Material.SADDLE, 1, (short) 0),
    ACHIEVEMENT(Material.COAL, 1, (short) 0),
    ACHIEVEMENT_ACHIEVED(Material.DIAMOND, 1, (short) 0),
    MILESTONE(Material.COAL, 1, (short) 1),
    MILESTONE_ACHIEVED(Material.EMERALD, 1, (short) 0),
    ACHIEVEMENTS_MENU(Material.EXP_BOTTLE, Parkour.getString("achievement.inventory.opener"), 1, (short) 0);

    private final ItemStack item;

    private Items(Material material, String name, int amount, short data, String... desc) {
        this.item = new ItemStack(material, amount, data);
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(desc));
        item.setItemMeta(meta);
    }

    private Items(Material material, String name, int amount, short data, HashMap<Enchantment, Integer> ens, String... desc) {
        this.item = new ItemStack(material, amount, data);
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(desc));
        item.setItemMeta(meta);
        for (Map.Entry<Enchantment, Integer> ench : ens.entrySet()) {
            item.addEnchantment(ench.getKey(), ench.getValue());
        }
    }

    private Items(Material material, int amount, short data, HashMap<Enchantment, Integer> ens) {
        this.item = new ItemStack(material, amount, data);
        for (Map.Entry<Enchantment, Integer> ench : ens.entrySet()) {
            item.addEnchantment(ench.getKey(), ench.getValue());
        }
    }

    private Items(Material material, int amount, short data) {
        this.item = new ItemStack(material, amount, data);
    }

    public ItemStack getItem() {
        return item.clone();
    }
}
