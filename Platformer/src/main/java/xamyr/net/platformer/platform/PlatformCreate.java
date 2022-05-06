package xamyr.net.platformer.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import scala.Int;
import xamyr.net.platformer.Platformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformCreate {
    private final List<PlatformBlock> platformBlocks = new ArrayList<>();
    private final Platformer plugin;
    double xMove = 0;
    double yMove = 0;
    double zMove = 0;
    double moveByBlocks;
    double waitTime= 0;
    int schedulerId = 0;
    String direction = "";

    public PlatformCreate(Platformer plugin, List<Block> blocks){
        this.plugin = plugin;
        for (Block block : blocks) {
            platformBlocks.add(new PlatformBlock(block, true));
        }
    }

    public List<PlatformBlock> getPlatformBlocks(){return this.platformBlocks;}

    public void movePlatform(double xMove, double yMove, double zMove, String direction, double moveByBlocks, double waitTime){
        this.xMove = xMove;
        this.yMove = yMove;
        this.zMove = zMove;
        this.moveByBlocks = moveByBlocks;
        this.waitTime = waitTime;
        this.direction = direction;

        for (PlatformBlock block: platformBlocks) {
            block.xMove = xMove;
            block.yMove = yMove;
            block.zMove = zMove;
            schedulerId = Bukkit.getScheduler().runTaskTimer(plugin, new MovePlatformBlock(block), 2L, 2L).getTaskId();
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
                if(block.getxLocation() + moveByBlocks < armorLocation.getX() || block.getxLocation() > armorLocation.getX()){
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "west")) {
                if(block.getxLocation() - moveByBlocks > armorLocation.getX() || block.getxLocation() < armorLocation.getX()) {
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "up")){
                if(block.getyLocation() + moveByBlocks-1.5 < armorLocation.getY() || block.getyLocation() - 1.51870 > armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "down")){
                if(block.getyLocation() - moveByBlocks-1.5 > armorLocation.getY() || block.getyLocation()-1.5 < armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "north")){
                if(block.getzLocation() - moveByBlocks > armorLocation.getZ() || block.getzLocation() < armorLocation.getZ()) {
                    block.zMove *= -1;
                }
            }
            if(Objects.equals(direction, "south")){
                if(block.getzLocation() + moveByBlocks < armorLocation.getZ() || block.getzLocation() > armorLocation.getZ()){
                    block.zMove *= -1;
                }
            }
//            if((block.getxLocation()+moveByBlocks < armorLocation.getBlockX() && (Objects.equals(direction, "east")))
//                    || (block.getxLocation()-moveByBlocks > armorLocation.getBlockX() && Objects.equals(direction, "west"))
//                    || (block.getyLocation()+moveByBlocks < armorLocation.getBlockY() && Objects.equals(direction, "up"))
//                    || (block.getyLocation()-moveByBlocks > armorLocation.getBlockY() && Objects.equals(direction, "down"))
//                    || (block.getzLocation()+moveByBlocks < armorLocation.getBlockZ() && Objects.equals(direction, "north"))
//                    || (block.getzLocation()-moveByBlocks > armorLocation.getBlockZ() && Objects.equals(direction, "south"))){
//                block.xMove *= -1;
//                block.yMove *= -1;
//                block.zMove *= -1;
//            }
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
}
