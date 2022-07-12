package xamyr.net.platformer.platform;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Entity;
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
    public double moveNoBlocks;
    public double speed = 0;
    public int waitTime= 0;
    public boolean newVersion;
    public boolean blockDiedRecently = false;
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
                b = new PlatformBlock(block, name, true, false, false);
            }else{
                boolean[] check = checkEdgeBlock(blocks, block);
                b = new PlatformBlock(block, name, false, check[0], check[1]);
            }
            if(!Bukkit.getWorld(world).getChunkAt(block).isForceLoaded()){
                blockChunk.setForceLoaded(true);
            }
            platformBlocks.add(b);
        }
    }

    private boolean[] checkEdgeBlock(List<Block> blocks, Block block) {
        boolean front = false;
        boolean back = false;

        Boolean x1 = !blocks.contains(w.getBlockAt(block.getX()+1, block.getY(), block.getZ()));
        Boolean x2 = !blocks.contains(w.getBlockAt(block.getX()-1, block.getY(), block.getZ()));

        Boolean z1 = !blocks.contains(w.getBlockAt(block.getX(), block.getY(), block.getZ()+1));
        Boolean z2 = !blocks.contains(w.getBlockAt(block.getX(), block.getY(), block.getZ()-1));

        if (Objects.equals(direction, "south") || Objects.equals(direction, "north")) {
            if (z2 && x2) {
                front = true;
            } else if (x1 && z2){
                front = true;
            } else if (x1 && z1) {
                back = true;
            } else if (x2 && z1) {
                back = true;
            } else if (z2) {
                front = true;
            } else if (z1) {
                back = true;
            }
        }

        if (Objects.equals(direction, "east") || Objects.equals(direction, "west")) {
            if (x1 && z1) {
                back = true;
            } else if (x1 && z2) {
                back = true;
            } else if (x2 && z1) {
                front = true;
            } else if (x2 && z2) {
                front = true;
            } else if (x2) {
                front = true;
            } else if (x1) {
                back = true;
            }
        }

        return new boolean[] {front, back};
    }

    public List<PlatformBlock> getPlatformBlocks(){return this.platformBlocks;}

    public void showPlatformName(){
        for(PlatformBlock b : platformBlocks){
            b.fallingblock.setCustomNameVisible(true);
        }
    }

    public void hidePlatformName(){
        for(PlatformBlock b : platformBlocks){
            b.fallingblock.setCustomNameVisible(false);;
        }
    }

    public void movePlatform(){

        for (PlatformBlock block: platformBlocks) {
            block.moveBlock(plugin, direction, moveNoBlocks, speed);
        }
    }

    public void savePlatform() throws SQLException {
        Connection conn = plugin.getDatabase();
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO `platforms` (`name`,`world`,`blocks`,`direction`,`movenoblocks`,`wait`,`speed`, `newversion`) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `world` = VALUES(`world`), `blocks` = VALUES(`blocks`), `direction` = VALUES(`direction`), `movenoblocks` = VALUES(`movenoblocks`), `wait` = VALUES(`wait`), `speed` = VALUES(`speed`), `newversion` = VALUES(`newversion`)")) {
            StringBuilder blocks = new StringBuilder();
            for(PlatformBlock b: platformBlocks){
                blocks.append(b.fallingblock.getBlockData().getAsString()).append("/").append(b.getxLocation()-0.5).append("/").append(b.getyLocation()-0.03745).append("/").append(b.getzLocation()-0.5).append("#");
            }
            blocks.deleteCharAt(blocks.lastIndexOf("#"));
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
