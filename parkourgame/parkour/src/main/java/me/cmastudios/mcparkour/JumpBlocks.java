package me.cmastudios.mcparkour;

import com.jeff_media.customblockdata.CustomBlockData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Objects;

public class JumpBlocks {
    private Parkour plugin;
    private Team team;

    public JumpBlocks(Parkour plugin){
        this.plugin = plugin;
        if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("setJumps") == null){
            team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("setJumps");
        }
        this.team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("setJumps");
        team.setColor(ChatColor.GOLD);

    }
    public void showJumpBlocks(Location location, int radius) {
        for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
            for (int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
                for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    PersistentDataContainer jumpBlock = new CustomBlockData(block, plugin);
                    BoundingBox bbox = new BoundingBox(block.getX()+0.1, block.getY()+0.8, block.getZ()+0.1, block.getX()+0.2, block.getY()+0.8, block.getZ()+0.2);
                    List<Entity> magmas = (List<Entity>) location.getWorld().getNearbyEntities(bbox);
                    if (magmas.size() == 0){
                        if(jumpBlock.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)) {
                            spawnJumpBlockEntity(jumpBlock, block.getLocation());
                        }
                    }
                }
            }
        }
    }

    public void removeJumpBlockEntities() {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("setJumps");
        Bukkit.getWorlds().forEach((world -> {
            world.getEntities().forEach((entity -> {
                if(Objects.equals(entity.getCustomName(), "magma")){
                    team.removeEntry(entity.getUniqueId().toString());
                    entity.remove();
                }
            }));
        }));
    }


    public void editJumpBlocks(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        Block block = player.getTargetBlock(null, 70);
        final PersistentDataContainer jumpBlock = new CustomBlockData(block, plugin);
        Location location = block.getLocation();
        BoundingBox bbox = new BoundingBox(location.getX()+0.1, location.getY()+0.8, location.getZ()+0.1, location.getX()+0.2, location.getY()+0.8, location.getZ()+0.2);
        List<Entity> magmas = (List<Entity>) location.getWorld().getNearbyEntities(bbox);

        if (block.getType().isAir()) {
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if(jumpBlock.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)){
                return;
            }
            spawnJumpBlockEntity(jumpBlock, location);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Parkour.getString("course.jump.set", block.getType())));
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if(deleteJumpBlockEntity(jumpBlock, magmas)){
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Parkour.getString("course.jump.remove", block.getType())));
            }
        }
    }

    private boolean deleteJumpBlockEntity(PersistentDataContainer jumpBlock, List<Entity> magmas) {
        if(jumpBlock.has(plugin.jumpBlockKey, PersistentDataType.INTEGER)){
            jumpBlock.remove(plugin.jumpBlockKey);
            for (Entity e : magmas) {
                team.removeEntry(e.getUniqueId().toString());
                e.remove();
            }
            return true;
        }
        return false;
    }

    private void spawnJumpBlockEntity(PersistentDataContainer jumpBlock, Location location) {
        location.setX(location.getX() + 0.5);
        location.setZ(location.getZ() + 0.5);
        MagmaCube magma = (MagmaCube) location.getWorld().spawnEntity(location, EntityType.MAGMA_CUBE);
        magma.setAI(false);
        magma.setInvisible(true);
        magma.setInvulnerable(true);
        magma.setGlowing(true);
        magma.setSize(2);
        magma.setCustomName("magma");
        team.addEntry(magma.getUniqueId().toString());

        jumpBlock.set(plugin.jumpBlockKey, PersistentDataType.INTEGER, 1);
    }
}
