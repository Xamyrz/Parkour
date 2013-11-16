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

package me.cmastudios.mcparkour.data;

import me.cmastudios.mcparkour.Parkour;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EffectHead {
    private String worldName;
    private int x;
    private int y;
    private int z;
    private int courseId;
    private String skullTypeName;

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void setWorld(World world) {
        this.worldName = world.getName();
    }

    public Location getLocation() {
        return new Location(this.getWorld(), x, y, z);
    }

    public void setLocation(Location location) {
        this.setWorld(location.getWorld());
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    public void setCourse(ParkourCourse course) {
        this.courseId = course.getId();
    }

    public SkullType getSkullType() {
        return SkullType.valueOf(this.skullTypeName);
    }

    public EffectHead(String worldName, int x, int y, int z, int courseId, String skullTypeName) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.courseId = courseId;
        this.skullTypeName = skullTypeName;
    }

    public EffectHead(Location location, ParkourCourse course, SkullType skullType) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                course.getId(), skullType.name());
    }

    public static List<EffectHead> loadHeads(Connection conn, ParkourCourse course) throws SQLException {
        List<EffectHead> heads = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courseheads WHERE course_id = ?")) {
            stmt.setInt(1, course.getId());
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    EffectHead head = new EffectHead(result.getString("world_name"), result.getInt("x"),
                            result.getInt("y"), result.getInt("z"), result.getInt("course_id"),
                            result.getString("skull_type_name"));
                    heads.add(head);
                }
            }
        }
        return heads;
    }

    public static List<EffectHead> loadHeads(Connection conn) throws SQLException {
        List<EffectHead> heads = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM courseheads")) {
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    EffectHead head = new EffectHead(result.getString("world_name"), result.getInt("x"),
                            result.getInt("y"), result.getInt("z"), result.getInt("course_id"),
                            result.getString("skull_type_name"));
                    heads.add(head);
                }
            }
        }
        return heads;
    }

    public void delete(Parkour plugin) throws SQLException {
        Location block = this.getLocation();
        try (PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement(
                "DELETE FROM courseheads WHERE world_name = ? AND x = ? AND y = ? AND z = ?")) {
            stmt.setString(1, block.getWorld().getName());
            stmt.setInt(2, block.getBlockX());
            stmt.setInt(3, block.getBlockY());
            stmt.setInt(4, block.getBlockZ());
            stmt.executeUpdate();
        }
        this.getLocation().getBlock().removeMetadata("mcparkour-head", plugin);
    }

    public void setBlock(Parkour plugin) {
        Random rotator = new Random();
        Block block = this.getLocation().getBlock();
        block.setType(Material.SKULL);
        block.setMetadata("mcparkour-head", new FixedMetadataValue(plugin, this));
        BlockState modify = block.getState();
        if (modify.getData() instanceof Skull) {
            Skull data = (Skull) modify.getData();
            BlockFace randomAngle = BlockFace.values()[rotator.nextInt(BlockFace.values().length)];
            data.setRotation(randomAngle);
            data.setSkullType(this.getSkullType());
            modify.update(true, true);
        }
    }

    public void save(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO courseheads (world_name, x, y, z, course_id, skull_type_name) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, this.worldName);
            stmt.setInt(2, this.x);
            stmt.setInt(3, this.y);
            stmt.setInt(4, this.z);
            stmt.setInt(5, this.courseId);
            stmt.setString(6, this.skullTypeName);
            stmt.executeUpdate();
        }
    }

    private static final List<PotionEffect> playerEffects = Arrays.asList(
            new PotionEffect(PotionEffectType.SPEED, 4, 1),
            new PotionEffect(PotionEffectType.SPEED, 3, 2),
            new PotionEffect(PotionEffectType.JUMP, 3, 1),
            new PotionEffect(PotionEffectType.NIGHT_VISION, 30, 1),
            new PotionEffect(PotionEffectType.SPEED, 3, 1),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10, 1)
    );
    private static final List<PotionEffect> witherEffects = Arrays.asList(
            new PotionEffect(PotionEffectType.SLOW, 4, 1),
            new PotionEffect(PotionEffectType.BLINDNESS, 3, 1),
            new PotionEffect(PotionEffectType.CONFUSION, 10, 1),
            new PotionEffect(PotionEffectType.POISON, 4, 1)
    );

    Random potionTypeRNG = new Random();

    public ItemStack getPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        Validate.isTrue(potion.getItemMeta() instanceof PotionMeta);
        PotionMeta itemMeta = (PotionMeta) potion.getItemMeta();
        switch (this.getSkullType()) {
            case PLAYER:
                itemMeta.addCustomEffect(playerEffects.get(potionTypeRNG.nextInt(playerEffects.size())), true);
                break;
            case WITHER:
                itemMeta.addCustomEffect(witherEffects.get(potionTypeRNG.nextInt(witherEffects.size())), true);
                break;
            default:
                itemMeta.setDisplayName("Error");
                break;
        }
        potion.setItemMeta(itemMeta); // Stupid bukkit cloning
        return potion;
    }
}
