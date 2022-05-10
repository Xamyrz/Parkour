package xamyr.net.platformer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class PlatformerEvents implements Listener {
    private Platformer plugin;

    public PlatformerEvents(Platformer plugin){
        this.plugin = plugin;
    }

//    @EventHandler
//    public void ChunkUnloadEvent(ChunkUnloadEvent chunk){
//        plugin.platforms.forEach((key, value) -> {
//            for(Chunk c : value.platformChunks){
//                if(c == chunk.getChunk()){
//                    Bukkit.getLogger().info(c.toString());
//                    chunk.getChunk().load(true);
//                    chunk.getChunk().setForceLoaded(true);
//                    return;
//                }
//            }
//        });
//    }
}
