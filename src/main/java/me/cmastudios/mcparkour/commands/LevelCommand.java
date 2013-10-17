package me.cmastudios.mcparkour.commands;

import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.PlayerExperience;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LevelCommand implements CommandExecutor {

    private final Parkour plugin;

    public LevelCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(sender.getName());
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        }
        try {
            PlayerExperience xp = PlayerExperience.loadExperience(
                    plugin.getCourseDatabase(), target);
            int experience = xp.getExperience();
            sender.sendMessage(Parkour.getString("xp.has", target.getName(),
                    plugin.getLevel(experience), experience, plugin.getNextLevelRequiredXp(experience)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
