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

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public enum Item {
    VISION(ItemType.SPAWN,Material.EYE_OF_ENDER, Parkour.getString("item.vision"), 1, (short) 0),
    CHAT(ItemType.SETTINGS,Material.PAPER, Parkour.getString("item.chat"), 1, (short) 0, Parkour.getMessageArrayFromPrefix("item.chat.description")),
    SPAWN(ItemType.SPAWN,Material.NETHER_STAR, Parkour.getString("item.spawn"), 1, (short) 0),
    POINT(ItemType.SPAWN,Material.STICK, Parkour.getString("item.point"), 1, (short) 0, Parkour.getMessageArrayFromPrefix("item.point.description")),
    HELMET(ItemType.VIP,Material.GOLD_HELMET, 1, (short) 0, new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }}),
    CHESTPLATE(ItemType.VIP,Material.GOLD_CHESTPLATE, 1, (short) 0, new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }}),
    LEGGINGS(ItemType.VIP,Material.GOLD_LEGGINGS, 1, (short) 0, new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }}),
    BOOTS(ItemType.VIP,Material.GOLD_BOOTS, 1, (short) 0, new HashMap<Enchantment, Integer>() {{
        put(Enchantment.DURABILITY, 3);
    }}),
    FIREWORK_SPAWNER(ItemType.VIP,Material.FIREWORK, Parkour.getString("item.firework"), 1, (short) 0, Parkour.getMessageArrayFromPrefix("item.firework.description")),
    SCOREBOARD(ItemType.SETTINGS,Material.BOOK, Parkour.getString("item.scoreboard"), 1, (short) 0, Parkour.getMessageArrayFromPrefix("item.scoreboard.description")),
    SETTINGS(ItemType.SPAWN,Material.REDSTONE, Parkour.getString("item.settings"),1,(short) 0,Parkour.getMessageArrayFromPrefix("item.settings.description"));

    private final ItemStack item;
    private final ItemType type;

    private Item(ItemType type, Material material, String name, int amount, short data, List<String> desc) {
        this(type,material,name,amount,data);
        ItemMeta meta = this.item.getItemMeta();
        meta.setLore(desc);
        item.setItemMeta(meta);
    }

    private Item(ItemType type, Material material, String name, int amount, short data) {
        this(type,material,amount,data);
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    private Item(ItemType type, Material material, int amount, short data, HashMap<Enchantment, Integer> ens) {
        this(type,material,amount,data);
        for (Map.Entry<Enchantment, Integer> ench : ens.entrySet()) {
            item.addEnchantment(ench.getKey(), ench.getValue());
        }

    }

    private Item(ItemType type, Material material, int amount, short data) {
        this.type =type;
        this.item = new ItemStack(material, amount, data);
    }

    private Item(ItemType type, Material material, int amount) {
        this(type,material,amount,(short)0);
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public ItemType getType() {
        return type;
    }

    public static ArrayList<Item> getItemsByType(ItemType type) {
        ArrayList<Item> items = new ArrayList<>();
        for(Item item : values()) {
            if(item.getType()==type) {
                items.add(item);
            }
        }
        return items;
    }

    public boolean isSimilar(ItemStack is) {
        return getItem().getType()==is.getType()&&getItem().getDurability()==is.getDurability()&&getItem().getEnchantments().equals(is.getEnchantments())&&((is.getItemMeta().hasDisplayName()&&getItem().getItemMeta().hasDisplayName())||(!is.getItemMeta().hasDisplayName()&&!getItem().getItemMeta().hasDisplayName()));
    }

    public enum ItemType {
        SPAWN,
        VIP,
        MISC,
        SETTINGS
    }
}
