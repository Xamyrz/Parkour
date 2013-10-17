package me.cmastudios.mcparkour.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListCoursesCommand implements CommandExecutor {

    private final Parkour plugin;

    public ListCoursesCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2,
            String[] arg3) {
        StringBuilder courses = new StringBuilder(
                Parkour.getString("course.list"));
        try (PreparedStatement stmt = plugin.getCourseDatabase()
                .prepareStatement("SELECT * FROM courses ORDER BY id")) {
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    courses.append(' ').append(result.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        sender.sendMessage(courses.toString());
        return true;
    }

}
