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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Item {
    VISION(ItemType.SPAWN,Material.EYE_OF_ENDER, Parkour.getString("item.vision"), 1, (short) 0),
    CHAT(ItemType.SPAWN,Material.PAPER, Parkour.getString("item.chat"), 1, (short) 0),
    SPAWN(ItemType.SPAWN,Material.NETHER_STAR, Parkour.getString("item.spawn"), 1, (short) 0),
    POINT(ItemType.SPAWN,Material.STICK, Parkour.getString("item.point"), 1, (short) 0, Parkour.getString("item.point.description.0"), Parkour.getString("item.point.description.1"), Parkour.getString("item.point.description.2")),
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
    FIREWORK_SPAWNER(ItemType.VIP,Material.FIREWORK, Parkour.getString("item.firework"), 1, (short) 0),
    SCOREBOARD(ItemType.SPAWN,Material.BOOK, Parkour.getString("item.scoreboard"), 1, (short) 0),
    FAVORITES(ItemType.SPAWN,Material.EMERALD, Parkour.getString("favorites.item.base"), 1, (short) 0, Parkour.getString("favorites.item.base.lore0"), Parkour.getString("favorites.item.base.lore1")),
    NEXT_PAGE(ItemType.MISC,Material.ACTIVATOR_RAIL, Parkour.getString("favorites.item.next"), 1, (short) 0),
    PREV_PAGE(ItemType.MISC,Material.RAILS, Parkour.getString("favorites.item.prev"), 1, (short) 0),
    EASY(ItemType.MISC,Material.MINECART, 1),
    MEDIUM(ItemType.MISC,Material.STORAGE_MINECART, 1),
    HIDDEN(ItemType.MISC,Material.HOPPER_MINECART, 1),
    HARD(ItemType.MISC,Material.POWERED_MINECART, 1),
    V_HARD(ItemType.MISC,Material.EXPLOSIVE_MINECART, 1),
    THEMATIC(ItemType.MISC,Material.BOAT, 1),
    ADVENTURE(ItemType.MISC,Material.SADDLE, 1),
    ACHIEVEMENT(ItemType.MISC,Material.COAL, Parkour.getString("achievement.hidden"), 1,(short) 0),
    ACHIEVEMENT_ACHIEVED(ItemType.MISC,Material.DIAMOND, 1),
    MILESTONE(ItemType.MISC,Material.COAL, 1, (short) 1),
    MILESTONE_ACHIEVED(ItemType.MISC,Material.EMERALD, 1),
    ACHIEVEMENTS_MENU(ItemType.SPAWN,Material.EXP_BOTTLE, Parkour.getString("achievement.inventory.opener"), 1, (short) 0),
    GUIDE_BOOK(ItemType.SPAWN,Material.WRITTEN_BOOK, Parkour.getString("item.guide"),1,(short) 0);

    private final ItemStack item;
    private final ItemType type;

    private Item(ItemType type, Material material, String name, int amount, short data, String... desc) {
        this(type,material,name,amount,data);
        ItemMeta meta = this.item.getItemMeta();
        meta.setLore(Arrays.asList(desc));
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
        MISC
    }
}
