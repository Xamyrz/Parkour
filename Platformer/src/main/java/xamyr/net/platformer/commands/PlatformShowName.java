package xamyr.net.platformer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import xamyr.net.platformer.Platformer;
import xamyr.net.platformer.platform.Platform;
import xamyr.net.platformer.utils.Utils;
import xamyr.net.platformer.worldedit.BlocksSelection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformShowName implements TabExecutor {
    private final Platformer plugin;

    public PlatformShowName(Platformer plugin){this.plugin=plugin;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(args.length == 1){
                if(Objects.equals(args[0], "true")){
                    plugin.platforms.forEach((key, value) -> value.showPlatformName());
                    return true;
                }
                if(Objects.equals(args[0], "false")){
                    plugin.platforms.forEach((key, value) -> value.hidePlatformName());
                    return false;
                }
            }

        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        options.add("true");
        options.add("false");


        if(args.length == 1) return options;
        return null;
    }
}
