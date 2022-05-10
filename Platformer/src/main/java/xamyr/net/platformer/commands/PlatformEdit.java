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
import xamyr.net.platformer.utils.Utils;
import xamyr.net.platformer.worldedit.BlocksSelection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformEdit implements TabExecutor {
    private final Platformer plugin;

    public PlatformEdit(Platformer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            if (args.length == 6) {
                Platform p = plugin.platforms.get(args[0]);
                if (p != null) {
                    if (Objects.equals(args[1], ">1.8") || Objects.equals(args[1], "1.8")) {
                        if (Objects.equals(args[2].toLowerCase(), "up") || Objects.equals(args[2], "down") || Objects.equals(args[2], "north") || Objects.equals(args[2], "south") || Objects.equals(args[2], "west") || Objects.equals(args[2], "east")) {
                            if (Utils.isNumeric(args[3])) {
                                if (args[4].matches("-?(0|[1-9]\\d*)")) {
                                    if (Utils.isNumeric(args[5])) {
                                        p.getPlatformBlocks().forEach(platformBlock -> {
                                            Bukkit.getScheduler().cancelTask(platformBlock.schedulerId);
                                        });
                                        try {p.savePlatform();} catch (SQLException e) {e.printStackTrace();}
                                        for(PlatformBlock b: p.getPlatformBlocks()){
                                            if(!p.newVersion) b.barrier.setType(Material.AIR);
                                            Bukkit.getScheduler().cancelTask(b.schedulerId);
                                        }
                                        p.newVersion = Objects.equals(args[1], ">1.8");
                                        p.direction = args[2].toLowerCase();
                                        p.moveNoBlocks = Double.parseDouble(args[3]);
                                        p.waitTime = Integer.parseInt(args[4]);
                                        p.speed = Double.parseDouble(args[5]);
                                        plugin.removeOldPlatform(Bukkit.getWorld(p.world), args[0]);
                                        try {p.savePlatform();} catch (SQLException e) {e.printStackTrace();}
                                        plugin.platforms.remove(args[0]);
                                        plugin.loadPlatform(args[0]);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                sender.sendMessage("Too little Arguments, needs 6");
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        List<String> platforms = new ArrayList<>();
        plugin.platforms.forEach((key, value) -> platforms.add(key));

        List<String> options = new ArrayList<>();
        options.add("1.8");
        options.add(">1.8");

        List<String> direction = new ArrayList<>();
        direction.add("up");
        direction.add("down");
        direction.add("north");
        direction.add("south");
        direction.add("west");
        direction.add("east");

        List<String> moveNoBlocks = new ArrayList<>();
        moveNoBlocks.add("5");
        moveNoBlocks.add("4");
        moveNoBlocks.add("3");
        moveNoBlocks.add("2");
        moveNoBlocks.add("1");

        List<String> waitTime = new ArrayList<>();
        waitTime.add("1000");
        waitTime.add("500");

        List<String> speed = new ArrayList<>();
        speed.add("0.05");

        if (args.length == 1) return platforms;
        else if(args.length == 2) return options;
        else if(args.length == 3) return direction;
        else if(args.length == 4) return moveNoBlocks;
        else if(args.length == 5) return waitTime;
        else if(args.length == 6) return speed;
        return null;
    }
}
