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
    VISION(Material.EYE_OF_ENDER, Parkour.getString("item.vision"), 1),
    CHAT(Material.PAPER, Parkour.getString("item.chat"), 1),
    SPAWN(Material.NETHER_STAR, Parkour.getString("item.spawn"), 1),
    POINT(Material.STICK, Parkour.getString("item.point"), 1, Parkour.getString("item.point.description.0"), Parkour.getString("item.point.description.1"), Parkour.getString("item.point.description.2")),
    HELMET(Material.GOLD_HELMET, 1, (new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }})),
    CHESTPLATE(Material.GOLD_CHESTPLATE, 1),
    LEGGINGS(Material.GOLD_LEGGINGS, 1),
    BOOTS(Material.GOLD_BOOTS, 1),
    FIREWORK_SPAWNER(Material.FIREWORK, Parkour.getString("item.firework"), 1),
    SCOREBOARD(Material.BOOK, Parkour.getString("item.scoreboard"), 1),
    FAVORITES(Material.EMERALD, Parkour.getString("favorites.item.base"), 1, Parkour.getString("favorites.item.base.lore0")),
    NEXT_PAGE(Material.ACTIVATOR_RAIL, Parkour.getString("favorites.item.next"), 1),
    PREV_PAGE(Material.RAILS, Parkour.getString("favorites.item.prev"), 1),
    EASY(Material.MINECART, 1),
    MEDIUM(Material.STORAGE_MINECART, 1),
    HIDDEN(Material.HOPPER_MINECART, 1),
    HARD(Material.POWERED_MINECART, 1),
    V_HARD(Material.EXPLOSIVE_MINECART, 1),
    THEMATIC(Material.BOAT, 1),
    ADVENTURE(Material.SADDLE, 1),
    ACHIEVEMENT(Material.COAL, Parkour.getString("item.chat"), 1),
    ACHIEVEMENT_ACHIEVED(Material.DIAMOND, Parkour.getString("item.chat"), 1),
    ACHIEVEMENTS_MENU(Material.EXP_BOTTLE, Parkour.getString("achievement.inventory.opener"), 1);

    private final ItemStack item;

    private Items(Material material, String name, int amount, String... desc) {
        this.item = new ItemStack(material, amount);
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(desc));
        item.setItemMeta(meta);
    }

    private Items(Material material, String name, int amount, HashMap<Enchantment, Integer> ens, String... desc) {
        this.item = new ItemStack(material, amount);
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(desc));
        item.setItemMeta(meta);
        for (Map.Entry<Enchantment, Integer> ench : ens.entrySet()) {
            item.addEnchantment(ench.getKey(), ench.getValue());
        }
    }

    private Items(Material material, int amount, HashMap<Enchantment, Integer> ens) {
        this.item = new ItemStack(material, amount);
        for (Map.Entry<Enchantment, Integer> ench : ens.entrySet()) {
            item.addEnchantment(ench.getKey(), ench.getValue());
        }
    }

    private Items(Material material, int amount) {
        this.item = new ItemStack(material, amount);
    }

    public ItemStack getItem() {
        return item.clone();
    }
}
