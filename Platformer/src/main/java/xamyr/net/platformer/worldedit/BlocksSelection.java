package xamyr.net.platformer.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BlocksSelection {
    private Player player;
    public BlocksSelection(Player player){
        this.player = player;
    }
    public List<Block> selectionToList(){
        List<Block> platform = new ArrayList<>();
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        try {
            BlockVector3 pos1 = session.getSelection(session.getSelectionWorld()).getBoundingBox().getPos1();
            BlockVector3 pos2 = session.getSelection(session.getSelectionWorld()).getBoundingBox().getPos2();
            World w = Bukkit.getWorld(session.getSelectionWorld().getName());
            int xMin = Math.min(pos1.getX(),pos2.getX());
            int xMax = Math.max(pos1.getX(),pos2.getX());
            int yMin = Math.min(pos1.getY(), pos2.getY());
            int yMax = Math.max(pos1.getY(), pos2.getY());
            int zMin = Math.min(pos1.getZ(), pos2.getZ());
            int zMax = Math.max(pos1.getZ(), pos2.getZ());
            for(int x = xMin; x <= xMax; x++){
                for(int y = yMin; y <= yMax; y++){
                    for(int z = zMin; z <= zMax; z++){
                        if(w.getBlockAt(x, y, z).getType().name().equals("AIR")) continue;
                        platform.add(w.getBlockAt(x, y, z));
                    }
                }
            }
        } catch (IncompleteRegionException e) {
            Bukkit.getLogger().info("some fked up shit happened");
            e.printStackTrace();
        }
        if(platform.size() == 0) return null;
        return platform;
    }
}
