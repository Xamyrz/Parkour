package xamyr.net.platformer.commands;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import xamyr.net.platformer.Platformer;
import xamyr.net.platformer.platform.Platform;
import xamyr.net.platformer.utils.Utils;
import xamyr.net.platformer.worldedit.BlocksSelection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformCreate implements TabExecutor {
    private final Platformer plugin;

    public PlatformCreate(Platformer plugin){this.plugin=plugin;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(args.length == 6){
                if(Objects.equals(args[0], ">1.8") || Objects.equals(args[0], "1.8")){
                    if(Objects.equals(args[1].toLowerCase(), "up") || Objects.equals(args[1], "down") || Objects.equals(args[1], "north") || Objects.equals(args[1], "south") || Objects.equals(args[1], "west") || Objects.equals(args[1], "east")){
                        if(Utils.isNumeric(args[2])){
                            if(args[3].matches("-?(0|[1-9]\\d*)")){
                                if(Utils.isNumeric(args[4])){
                                    if(plugin.platforms.get(args[5]) == null){
                                        BlocksSelection selection = new BlocksSelection(player);
                                        if(selection.selectionToList() == null){
                                            player.sendMessage("No blocks selected");
                                            return false;
                                        }else{

                                            plugin.platforms.put(args[5], new Platform(plugin, selection.selectionToList(), Objects.equals(args[0], ">1.8"), args[1], Double.parseDouble(args[2]), Integer.parseInt(args[3]), Double.parseDouble(args[4]), args[5]));
                                            Platform platform = plugin.platforms.get(args[5]);
                                            try {platform.savePlatform();} catch (SQLException e) {e.printStackTrace();}
                                            platform.movePlatform();
                                        }
                                    }
                                }else{
                                    player.sendMessage("speed is non numeric gives as 5th argument");
                                    return false;
                                }
                            }else{
                                player.sendMessage("WaitTime is non Integer given as 4th argument");
                                return false;
                            }
                        }else{
                            player.sendMessage("MoveNoBlocks is non numeric given as 3rd argument");
                            return false;
                        }
                    }else{
                        player.sendMessage("Invalid Direction as 2nd argument");
                        return false;
                    }
                }
            }else{
                player.sendMessage("Too little Arguments, needs 4");
                return false;
            }


        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
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

        if(args.length == 1) return options;
        else if(args.length == 2) return direction;
        else if(args.length == 3) return moveNoBlocks;
        else if(args.length == 4) return waitTime;
        else if(args.length == 5) return speed;
        return null;
    }
}
