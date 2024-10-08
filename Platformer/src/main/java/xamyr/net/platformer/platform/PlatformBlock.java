package xamyr.net.platformer.platform;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import xamyr.net.platformer.Platformer;

import java.util.List;
import java.util.Objects;

public class PlatformBlock {
    public ArmorStand armorstand;
    public Shulker shulker;
    public Block barrier;
    public FallingBlock fallingblock;
    private final double xLocation;
    private final double yLocation;
    private final double zLocation;
    public double xMove = 0;
    public double yMove = 0;
    public double zMove = 0;
    private boolean front;
    private boolean back;
    public int schedulerId = 0;
    public boolean newVersion;
    public BlockData blockData;
    private String name;

    public PlatformBlock(Block block, String name, Boolean newerVersion, Boolean front, Boolean back){
        World world = block.getWorld();
        this.name = name;
        xLocation = block.getX()+0.5;
        yLocation = block.getY()+0.03745;
        zLocation = block.getZ()+0.5;
        newVersion = newerVersion;
        blockData = block.getBlockData();
        this.front = front;
        this.back = back;
        Location location = new Location(world, xLocation, yLocation, zLocation);
        if(newerVersion){
            initArmorstand(location);
            initShulker(location);
            initFallingblock(location);
            removeBlock(block);
        }else{
            initArmorstand(location);
            initFallingblock(location);
            initBarrierBlock(block);
        }
    }

    private void initArmorstand(Location location){
        location.setY(yLocation - 1.5187);
//        this.armorstand = (ArmorStand) world.spawnEntity(new Location(world, xLocation, yLocation -1.5187, zLocation), EntityType.ARMOR_STAND);
        this.armorstand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorstand.setCustomName(name);
        armorstand.setGravity(false);
        armorstand.setInvulnerable(true);
        armorstand.setInvisible(true);
    }

    private void initShulker(Location location){
        this.shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
        shulker.setCustomName(name);
        shulker.setAI(false);
        shulker.setSilent(true);
        shulker.setGravity(false);
        shulker.setInvulnerable(true);
        shulker.setInvisible(true);
        shulker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000,1, false, false));
        armorstand.addPassenger(shulker);
    }

    public void initFallingblock(Location location){
        this.fallingblock = location.getWorld().spawnFallingBlock(location, blockData);
        fallingblock.setCustomName(name);
        fallingblock.setGravity(false);
        fallingblock.setVelocity(new Vector(0,0,0));
        NBTEditor.set(fallingblock, -1000000000, "Time");
        armorstand.addPassenger(fallingblock);
    }

    private  void initBarrierBlock(Block block){
        block.setType(Material.BARRIER);
        barrier = block;
    }

    public void moveBlock(Platformer plugin, String direction, Double moveNoBlocks, double speed){
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
        this.schedulerId = Bukkit.getScheduler().runTaskTimer(plugin, new PlatformBlock.MovePlatformBlock(this, direction, moveNoBlocks, plugin.platforms.get(name)), 2L, 2L).getTaskId();
    }

    public static class MovePlatformBlock implements Runnable{
        private final String direction;
        private final double moveNoBlocks;
        private PlatformBlock block;
        private World w;

        //I shouldn't have to store it, but atm only solution
        //I can think of to get a boolean for blockDiedRecently
        private Platform platform;

        public MovePlatformBlock(PlatformBlock block, String direction, Double moveNoBlocks, Platform platform){
            this.direction = direction;
            this.moveNoBlocks = moveNoBlocks;
            this.block = block;
            this.w = block.armorstand.getWorld();
            this.platform = platform;
        }

        @Override
        public void run() {
            Location location = null;
            Location armorLocation = block.armorstand.getLocation();

            //must be better solution for this...
            if(block.fallingblock.isDead() && !platform.blockDiedRecently){
                platform.blockDiedRecently = true;
                for (PlatformBlock b: platform.getPlatformBlocks()) {
                    b.fallingblock.remove();
                    b.initFallingblock(b.fallingblock.getLocation());
                }
                platform.blockDiedRecently = false;
            }

            if(Objects.equals(direction, "east")){
                barrierMoveX(armorLocation);
                if(block.getxLocation() + moveNoBlocks < armorLocation.getX() || block.getxLocation() > armorLocation.getX()){
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "west")) {
                barrierMoveX(armorLocation);
                if(block.getxLocation() - moveNoBlocks > armorLocation.getX() || block.getxLocation() < armorLocation.getX()) {
                    block.xMove *= -1;
                }
            }
            if(Objects.equals(direction, "up")){
                barrierMoveY(armorLocation);
                if(block.getyLocation() + moveNoBlocks -1.5 < armorLocation.getY() || block.getyLocation() - 1.51870 > armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "down")){
                barrierMoveY(armorLocation);
                if(block.getyLocation() - moveNoBlocks -1.5 > armorLocation.getY() || block.getyLocation()-1.5 < armorLocation.getY()) {
                    block.yMove *= -1;
                }
            }
            if(Objects.equals(direction, "north")){
                barrierMoveZ(armorLocation);
                if(block.getzLocation() - moveNoBlocks > armorLocation.getZ() || block.getzLocation() < armorLocation.getZ()) {
                    block.zMove *= -1;
                }
            }
            if(Objects.equals(direction, "south")){
                barrierMoveZ(armorLocation);
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

        private void barrierMoveX(Location armorLocation) {
            if(!block.newVersion){
                Block b = w.getBlockAt(armorLocation.getBlockX(), block.barrier.getY(), block.barrier.getZ());
                if(armorLocation.getBlockX() != block.barrier.getX()){
                    if(block.xMove > 0) {
                        if (block.front) {
                            removeBarrier(b);
                        }
                        if(block.back) {
                            setBarrier(b);
                        }
                    }
                    if(block.xMove < 0) {
                        if (block.back) {
                            removeBarrier(b);
                        }
                        if(block.front) {
                            setBarrier(b);
                        }
                    }
                }
            }
        }

        private void barrierMoveY(Location armorLocation) {
            if(!block.newVersion){
                Block b = w.getBlockAt(block.barrier.getX(), armorLocation.getBlockY()+2, block.barrier.getZ());
                if(armorLocation.getBlockY()+2 != block.barrier.getY()){
                    onBarrierCollision(b);
                }else{
                    block.barrier = b;
                    block.barrier.setType(Material.BARRIER);
                }
            }
        }

        private void barrierMoveZ(Location armorLocation) {
            if(!block.newVersion){
                Block b = w.getBlockAt(block.barrier.getX(), block.barrier.getY(), armorLocation.getBlockZ());
                if(armorLocation.getBlockZ() != block.barrier.getZ()) {
                    if(block.zMove > 0) {
                        if (block.front) {
                            removeBarrier(b);
                        }
                        if(block.back) {
                            setBarrier(b);
                        }
                    }
                    if(block.zMove < 0) {
                        if (block.back) {
                            removeBarrier(b);
                        }
                        if(block.front) {
                            setBarrier(b);
                        }
                    }
                }
            }
        }

        private void removeBarrier(Block b) {
            if (b.getType() == Material.BARRIER) {
                block.barrier.setType(Material.AIR);
                block.barrier = b;
            } else {
                block.barrier.setType(Material.AIR);
            }
        }

        private void setBarrier(Block b) {
            if (b.getType() == Material.AIR) {
                b.setType(Material.BARRIER);
                block.barrier = b;
            }
        }

        private void onBarrierCollision(Block b) {
            if (b.getType() == Material.AIR) {
                b.setType(Material.BARRIER);
            } else if (b.getType() == Material.BARRIER) {
                block.barrier.setType(Material.AIR);
            }
            block.barrier = b;
        }
    }

    private void removeBlock(Block block){
        block.setType(Material.AIR);
    }

    public double getxLocation() {return xLocation;}

    public double getyLocation() {return yLocation;}

    public double getzLocation() {return zLocation;}
}
