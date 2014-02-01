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

package tk.maciekmm.achievements;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Item {
    NEXT_PAGE(ItemType.MISC,Material.ACTIVATOR_RAIL, Achievements.getString("achievement.item.next"), 1, (short) 0),
    PREV_PAGE(ItemType.MISC,Material.RAILS, Achievements.getString("achievement.item.prev"), 1, (short) 0),
    ACHIEVEMENT(ItemType.MISC,Material.COAL, Achievements.getString("achievement.hidden"), 1,(short) 0),
    ACHIEVEMENT_ACHIEVED(ItemType.MISC,Material.DIAMOND, 1),
    MILESTONE(ItemType.MISC,Material.COAL, 1, (short) 1),
    MILESTONE_ACHIEVED(ItemType.MISC,Material.EMERALD, 1),
    ACHIEVEMENTS_MENU(ItemType.SPAWN,Material.EXP_BOTTLE, Achievements.getString("achievement.inventory.opener"), 1, (short) 0);
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
        MISC,
    }
}
