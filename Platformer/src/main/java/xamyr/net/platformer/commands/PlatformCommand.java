package xamyr.net.platformer.commands;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import xamyr.net.platformer.Platformer;
import xamyr.net.platformer.platform.PlatformCreate;
import xamyr.net.platformer.utils.Utils;
import xamyr.net.platformer.worldedit.BlocksSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformCommand implements TabExecutor {
    private final Platformer plugin;

    public PlatformCommand(Platformer plugin){this.plugin=plugin;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(args.length == 5){
                if(Objects.equals(args[0], ">1.8")){
                    if(Objects.equals(args[1], "up") || Objects.equals(args[1], "down") || Objects.equals(args[1], "north") || Objects.equals(args[1], "south") || Objects.equals(args[1], "west") || Objects.equals(args[1], "east")){
                        if(Utils.isNumeric(args[2])){
                            if(Utils.isNumeric(args[3])){
                                if(Utils.isNumeric(args[4])){
                                    BlocksSelection selection = new BlocksSelection(player);
                                    PlatformCreate platform = new PlatformCreate(plugin, selection.selectionToList());
                                    if(Double.parseDouble(args[4]) == 0){
                                        player.sendMessage("speed can't be 0, 5th argument");
                                        return false;
                                    }
                                    if(Objects.equals(args[1], "up")) platform.movePlatform(0, Double.parseDouble(args[4]), 0, args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                    if(Objects.equals(args[1], "down")) platform.movePlatform(0, Double.parseDouble(args[4])*-1, 0, args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                    if(Objects.equals(args[1], "north")) platform.movePlatform(0, 0, Double.parseDouble(args[4])*-1, args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                    if(Objects.equals(args[1], "south")) platform.movePlatform(0, 0, Double.parseDouble(args[4]), args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                    if(Objects.equals(args[1], "west")) platform.movePlatform(Double.parseDouble(args[4])*-1, 0, 0, args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                    if(Objects.equals(args[1], "east")) platform.movePlatform(Double.parseDouble(args[4]), 0, 0, args[1], Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                }else{
                                    player.sendMessage("speed is non numeric gives as 5th argument");
                                }
                            }else{
                                player.sendMessage("WaitTime is non numeric given as 4th argument");
                            }
                        }else{
                            player.sendMessage("MoveNoBlocks is non numeric given as 3rd argument");
                        }
                    }else{
                        player.sendMessage("Invalid Direction as 2nd argument");
                    }
                }
            }else{
                player.sendMessage("Too little Arguments, needs 4");
            }


        }

        return false;
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
        waitTime.add("1");
        waitTime.add("0.5");

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
