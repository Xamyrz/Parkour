package xamyr.net.platformer.platform;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Shulker;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PlatformBlock {
    public ArmorStand armorstand;
    public Shulker shulker;
    public Block barrier;
    public FallingBlock fallingblock;
    private double xLocation;
    private double yLocation;
    private double zLocation;
    public double xMove = 0;
    public double yMove = 0;
    public double zMove = 0;

    public PlatformBlock(Block block, Boolean newerVersion){
        World world = block.getWorld();
        xLocation = block.getX()+0.5;
        yLocation = block.getY()+0.03745;
        zLocation = block.getZ()+0.5;
        if(newerVersion){
            initArmorstand(world);
            initShulker(world);
            initFallingblock(world, block);
        }
        removeBlock(block);
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

    private void removeBlock(Block block){
        block.setType(Material.AIR);
    }

    public double getxLocation() {return xLocation;}

    public double getyLocation() {return yLocation;}

    public double getzLocation() {return zLocation;}
}
