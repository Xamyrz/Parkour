package xamyr.net.platformer.platform;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import xamyr.net.platformer.Platformer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Platform {
    private final List<PlatformBlock> platformBlocks = new ArrayList<>();
    private final Platformer plugin;
    public double xMove = 0;
    public double yMove = 0;
    public double zMove = 0;
    public double moveNoBlocks;
    public double speed = 0;
    public int waitTime= 0;
    public boolean newVersion;
    public String direction = "";
    public String name = "";
    public String world = "";
    public World w;

    public Platform(Platformer plugin, List<Block> blocks, Boolean newVersion, String direction, double moveNoBlocks, int waitTime, double speed, String name){
        this.plugin = plugin;
        this.world = blocks.get(0).getWorld().getName();
        this.w = blocks.get(0).getWorld();
        this.newVersion = newVersion;
        this.direction = direction;
        this.moveNoBlocks = moveNoBlocks;
        this.waitTime = waitTime;
        this.speed = speed;
        this.name = name;
        for (Block block : blocks) {
            PlatformBlock b;
            Chunk blockChunk = Bukkit.getWorld(world).getChunkAt(block);

            if(newVersion){
                b = new PlatformBlock(block, true);
                b.armorstand.setCustomName(name);
                b.fallingblock.setCustomName(name);
                b.shulker.setCustomName(name);
            }else{
                b = new PlatformBlock(block, false);
                b.armorstand.setCustomName(name);
                b.fallingblock.setCustomName(name);
            }
            if(!Bukkit.getWorld(world).getChunkAt(block).isForceLoaded()){
                blockChunk.setForceLoaded(true);
            }
            platformBlocks.add(b);
        }
    }

    public List<PlatformBlock> getPlatformBlocks(){return this.platformBlocks;}

    public void showPlatformName(){
        for(PlatformBlock b : platformBlocks){
            b.fallingblock.setCustomNameVisible(true);
        }
    }

    public void hidePlatformName(){
        for(PlatformBlock b : platformBlocks){
            b.fallingblock.setCustomNameVisible(false);
        }
    }

    public void movePlatform(){
        if (Objects.equals(direction, "up"))
            this.yMove = speed;
        if (Objects.equals(direction, "down"))
            this.yMove = speed * -1;
        if (Objects.equals(direction, "north"))
            this.zMove = speed * -1;
        if (Objects.equals(direction, "south"))
            this.zMove = speed;
        if (Objects.equals(direction, "west"))
            this.xMove = speed * -1;
        if (Objects.equals(direction, "east"))
            this.xMove = speed;

        for (PlatformBlock block: platformBlocks) {
            block.xMove = this.xMove;
            block.yMove = this.yMove;
            block.zMove = this.zMove;
            block.schedulerId = Bukkit.getScheduler().runTaskTimer(plugin, new MovePlatformBlock(block), 2L, 2L).getTaskId();
        }
    }

    public class MovePlatformBlock implements Runnable{

        private final PlatformBlock block;

        public MovePlatformBlock(PlatformBlock block){this.block = block;}

        @Override
        public void run() {
            Location location = null;
            Location armorLocation = block.armorstand.getLocation();

            if(Objects.equals(direction, "east")){
                if(!newVersion){
                    if(armorLocation.getBlockX() != block.barrier.getX()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(armorLocation.getBlockX(), block.barrier.getY(), block.barrier.getZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getxLocation() + moveNoBlocks < armorLocation.getX() || block.getxLocation() > armorLocation.getX()){
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "west")) {
                if(!newVersion){
                    if(armorLocation.getBlockX() != block.barrier.getX()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(armorLocation.getBlockX(), block.barrier.getY(), block.barrier.getZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getxLocation() - moveNoBlocks > armorLocation.getX() || block.getxLocation() < armorLocation.getX()) {
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "up")){
                if(!newVersion){
                    if(armorLocation.getBlockY()+2 != block.barrier.getY()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(block.barrier.getX(), armorLocation.getBlockY()+2, block.barrier.getZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getyLocation() + moveNoBlocks -1.5 < armorLocation.getY() || block.getyLocation() - 1.51870 > armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "down")){
                if(!newVersion){
                    if(armorLocation.getBlockY()+2 != block.barrier.getY()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(block.barrier.getX(), armorLocation.getBlockY()+2, block.barrier.getZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getyLocation() - moveNoBlocks -1.5 > armorLocation.getY() || block.getyLocation()-1.5 < armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "north")){
                if(!newVersion){
                    if(armorLocation.getBlockZ() != block.barrier.getZ()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(block.barrier.getX(), block.barrier.getY(), armorLocation.getBlockZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getzLocation() - moveNoBlocks > armorLocation.getZ() || block.getzLocation() < armorLocation.getZ()) {
                    block.zMove *= -1;
                }
            }
            if(Objects.equals(direction, "south")){
                if(!newVersion){
                    if(armorLocation.getBlockZ() != block.barrier.getZ()){
                        block.barrier.setType(Material.AIR);
                        block.barrier = w.getBlockAt(block.barrier.getX(), block.barrier.getY(), armorLocation.getBlockZ());
                        block.barrier.setType(Material.BARRIER);
                    }
                }
                if(block.getzLocation() + moveNoBlocks < armorLocation.getZ() || block.getzLocation() > armorLocation.getZ()){
                    block.zMove *= -1;
                }
            }
            if(block.xMove > 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX()+block.xMove,block.armorstand.getLocation().getY(),block.armorstand.getLocation().getZ());
            if(block.xMove < 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX()+block.xMove,block.armorstand.getLocation().getY(),block.armorstand.getLocation().getZ());
            if(block.yMove > 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX(),block.armorstand.getLocation().getY()+block.yMove,block.armorstand.getLocation().getZ());
            if(block.yMove < 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX(),block.armorstand.getLocation().getY()+block.yMove,block.armorstand.getLocation().getZ());
            if(block.zMove > 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX(),block.armorstand.getLocation().getY(),block.armorstand.getLocation().getZ()+block.zMove);
            if(block.zMove < 0) location = new Location(block.armorstand.getWorld(),block.armorstand.getLocation().getX(),block.armorstand.getLocation().getY(),block.armorstand.getLocation().getZ()+block.zMove);

            List<Entity> entities = block.armorstand.getPassengers();
            block.armorstand.eject();
            block.armorstand.teleport(location);
            for (Entity e: entities) {
                block.armorstand.addPassenger(e);
            }
        }
    }

    public void savePlatform() throws SQLException {
        Connection conn = plugin.getDatabase();
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO `platforms` (`name`,`world`,`blocks`,`direction`,`movenoblocks`,`wait`,`speed`, `newversion`) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `world` = VALUES(`world`), `blocks` = VALUES(`blocks`), `direction` = VALUES(`direction`), `movenoblocks` = VALUES(`movenoblocks`), `wait` = VALUES(`wait`), `speed` = VALUES(`speed`), `newversion` = VALUES(`newversion`)")) {
            StringBuilder blocks = new StringBuilder();
            for(PlatformBlock b: platformBlocks){
                blocks.append(b.fallingblock.getBlockData().getMaterial().name()).append("/").append(b.getxLocation()-0.5).append("/").append(b.getyLocation()-0.03745).append("/").append(b.getzLocation()-0.5).append(",");
            }
            blocks.deleteCharAt(blocks.lastIndexOf(","));
            statement.setString(1, name);
            statement.setString(2, world);
            statement.setString(3, blocks.toString());
            statement.setString(4, direction);
            statement.setDouble(5, moveNoBlocks);
            statement.setInt(6, waitTime);
            statement.setDouble(7, speed);
            statement.setBoolean(8, newVersion);
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger("error saving block to DB....").log(Level.SEVERE, null, ex);
        }
    }
}
