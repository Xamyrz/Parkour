package me.cmastudios.mcparkour.commands;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class SetJumpsCommand implements CommandExecutor {

    private final Parkour plugin;

    public SetJumpsCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (player.hasMetadata("setJumps")) {
            player.removeMetadata("setJumps", plugin);
            plugin.jumpBlocks.removeJumpBlockEntities();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Parkour.getString("course.jump.off")));
        } else {
            player.setMetadata("setJumps", new FixedMetadataValue(plugin, true));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Parkour.getString("course.jump.on")));
        }
        if (args.length == 1){
            if (Utils.isNumeric(args[0])) {
                int radius = (int) Double.parseDouble(args[0]);
                if(radius <= 70){
                    plugin.jumpBlocks.showJumpBlocks(player.getLocation(), radius);
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}
