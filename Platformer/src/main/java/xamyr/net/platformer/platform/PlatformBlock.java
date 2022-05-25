package xamyr.net.platformer.platform;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import xamyr.net.platformer.Platformer;

import java.awt.*;
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
    public int schedulerId = 0;
    public boolean newVersion;

    public PlatformBlock(Block block, Boolean newerVersion){
        World world = block.getWorld();
        xLocation = block.getX()+0.5;
        yLocation = block.getY()+0.03745;
        zLocation = block.getZ()+0.5;
        newVersion = newerVersion;
        if(newerVersion){
            initArmorstand(world);
            initShulker(world);
            initFallingblock(world, block);
            removeBlock(block);
        }else{
            initArmorstand(world);
            initFallingblock(world, block);
            initBarrierBlock(world, block);
        }
    }

    private void initArmorstand(World world){
        this.armorstand = (ArmorStand) world.spawnEntity(new Location(world, xLocation, yLocation -1.5187, zLocation), EntityType.ARMOR_STAND);
        armorstand.setGravity(false);
        armorstand.setInvulnerable(true);
        armorstand.setInvisible(true);
    }

    private void initShulker(World world){
        this.shulker = (Shulker) world.spawnEntity(new Location(world, xLocation, yLocation, zLocation), EntityType.SHULKER);
        shulker.setAI(false);
        shulker.setSilent(true);
        shulker.setGravity(false);
        shulker.setInvulnerable(true);
        shulker.setInvisible(true);
        shulker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000,1, false, false));
        armorstand.addPassenger(shulker);
    }

    private void initFallingblock(World world, Block block){
        this.fallingblock = world.spawnFallingBlock(new Location(world, xLocation, yLocation, zLocation), block.getType().createBlockData());
        fallingblock.setGravity(false);
        fallingblock.setVelocity(new Vector(0,0,0));
        NBTEditor.set(fallingblock, -1000000, "Time");
        armorstand.addPassenger(fallingblock);
    }

    private  void initBarrierBlock(World world, Block block){
        block.setType(Material.BARRIER);
        barrier = world.getBlockAt(block.getLocation());
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
        this.schedulerId = Bukkit.getScheduler().runTaskTimer(plugin, new PlatformBlock.MovePlatformBlock(this, direction, moveNoBlocks), 2L, 2L).getTaskId();
    }

    public static class MovePlatformBlock implements Runnable{
        private final String direction;
        private final double moveNoBlocks;
        private PlatformBlock block;
        private World w;

        public MovePlatformBlock(PlatformBlock block, String direction, Double moveNoBlocks){
            this.direction = direction;
            this.moveNoBlocks = moveNoBlocks;
            this.block = block;
            this.w = block.armorstand.getWorld();
        }

        @Override
        public void run() {
            Location location = null;
            Location armorLocation = block.armorstand.getLocation();

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
                    onBarrierCollision(b);
                }else{
                    block.barrier = b;
                    block.barrier.setType(Material.BARRIER);
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
                if(armorLocation.getBlockZ() != block.barrier.getZ()){
                    onBarrierCollision(b);
                }else{
                    block.barrier = b;
                    block.barrier.setType(Material.BARRIER);
                }
            }
        }

        private void onBarrierCollision(Block b) {
            if(b.getType() == Material.AIR || b.getType() == Material.BARRIER){
                block.barrier.setType(Material.AIR);
                block.barrier = b;
                block.barrier.setType(Material.BARRIER);
            }else{
                block.barrier.setType(Material.AIR);
            }
        }
    }

    private void removeBlock(Block block){
        block.setType(Material.AIR);
    }

    public double getxLocation() {return xLocation;}

    public double getyLocation() {return yLocation;}

    public double getzLocation() {return zLocation;}
}
