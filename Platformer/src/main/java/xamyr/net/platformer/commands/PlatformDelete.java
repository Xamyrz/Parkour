package xamyr.net.platformer.commands;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import xamyr.net.platformer.Platformer;
import xamyr.net.platformer.platform.Platform;
import xamyr.net.platformer.platform.PlatformBlock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformDelete implements TabExecutor {
    private final Platformer plugin;

    public PlatformDelete(Platformer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                Platform p = plugin.platforms.get(args[0]);
                World w = Bukkit.getWorld(p.world);
                if(p != null){
                    for(PlatformBlock b: p.getPlatformBlocks()){
                        if(!p.newVersion) b.barrier.setType(Material.AIR);
                        Bukkit.getScheduler().cancelTask(b.schedulerId);
                        Chunk c = w.getChunkAt(b.fallingblock.getLocation());
                        c.setForceLoaded(false);
                        c.unload();
                    }
                    plugin.removeOldPlatform(Objects.requireNonNull(Bukkit.getWorld(p.world)), args[0]);
                    plugin.platforms.remove(args[0]);
                    plugin.deletePlatform(args[0]);
                    sender.sendMessage("Deleted Platform ("+args[0]+") successfully");
                    return true;
                }
                sender.sendMessage("Cannot find Platform ("+args[0]+") successfully");
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        List<String> platforms = new ArrayList<>();
        plugin.platforms.forEach((key, value) -> platforms.add(key));

        if(args.length == 1) return platforms;
        return null;
    }
}
