package me.cmastudios.mcparkour.commands;

import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeleteCourseCommand implements CommandExecutor {

    private final Parkour plugin;

    public DeleteCourseCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2,
            String[] args) {
        if (args.length < 1) {
            return false;
        }
        try {
            int id = Integer.parseInt(args[0]);
            ParkourCourse course = ParkourCourse.loadCourse(
                    plugin.getCourseDatabase(), id);
            if (course != null) {
                course.delete(plugin.getCourseDatabase());
            } else {
                sender.sendMessage(Parkour.getString("error.course404"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Parkour.getString("error.invalidint"));
        }
        return true;
    }

}
