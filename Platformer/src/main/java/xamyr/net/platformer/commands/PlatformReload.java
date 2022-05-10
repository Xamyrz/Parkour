package xamyr.net.platformer.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import xamyr.net.platformer.Platformer;
import xamyr.net.platformer.platform.Platform;
import xamyr.net.platformer.platform.PlatformBlock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformReload implements TabExecutor {
    private final Platformer plugin;

    public PlatformReload(Platformer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                if (Objects.equals(args[0], "all")) {
                    plugin.platforms.forEach((key, value) -> {
                        for(PlatformBlock b : value.getPlatformBlocks()){
                            Bukkit.getScheduler().cancelTask(b.schedulerId);
                        }
                    });
                    try {
                        plugin.loadPlatforms();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    sender.sendMessage("Reloaded all platforms");
                    return true;
                }
            }
            if (args.length == 2) {
                if (Objects.equals(args[0], "name")) {
                    Platform p = plugin.platforms.get(args[1]);
                    if(p != null){
                        for(PlatformBlock b: p.getPlatformBlocks()){
                            Bukkit.getScheduler().cancelTask(b.schedulerId);
                        }
                        plugin.removeOldPlatform(Bukkit.getWorld(p.world), args[1]);
                        plugin.platforms.remove(args[1]);
                        plugin.loadPlatform(args[1]);
                        sender.sendMessage("Reloaded ("+args[1]+") platform");
                        return true;
                    }
                }
            }

        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        options.add("name");
        options.add("all");

        List<String> platforms = new ArrayList<>();
        plugin.platforms.forEach((key, value) -> platforms.add(key));


        if (args.length == 1) return options;
        else if(args.length == 2) return platforms;
        return null;
    }
}
