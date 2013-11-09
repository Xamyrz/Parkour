package me.cmastudios.mcparkour.commands;

import java.sql.SQLException;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.AdventureCourse;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.data.PlayerHighScore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdventureCommand implements CommandExecutor {

    private final Parkour plugin;

    public AdventureCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args.length < 1) return false;
        String advName = args[0];
        try {
            AdventureCourse course = AdventureCourse.loadAdventure(plugin.getCourseDatabase(), advName);
            if (args.length == 3) {
                switch (args[1]) {
                case "add":
                    if (sender.hasPermission("parkour.set")) {
                        ParkourCourse chap = ParkourCourse.loadCourse(plugin.getCourseDatabase(), Integer.parseInt(args[2]));
                        if (chap == null) {
                            sender.sendMessage(Parkour.getString("error.course404"));
                        } else {
                            if (course == null) {
                                course = new AdventureCourse(advName);
                            }
                            course.addCourse(chap);
                            course.save(plugin.getCourseDatabase());
                            int pos = course.getCourses().indexOf(course) + 1;
                            sender.sendMessage(Parkour.getString("adv.add", chap.getId(), course.getName(), pos));
                        }
                    } else {
                        sender.sendMessage(Parkour.getString("error.permission"));
                    }
                    break;
                }
            } else if (course == null) {
                sender.sendMessage(Parkour.getString("error.course404"));
            } else if (!(sender instanceof Player)) {
                sender.sendMessage(Parkour.getString("error.playerreq"));
            } else if (args.length == 2) {
                int chapter = Integer.parseInt(args[1]);
                ParkourCourse chap = course.getCourses().get(chapter - 1);
                if (chap == null) {
                    sender.sendMessage(Parkour.getString("error.course404"));
                } else if (chapter > 1) {
                    ParkourCourse parent = course.getCourses().get(chapter - 2);
                    PlayerHighScore score = PlayerHighScore.loadHighScore(plugin.getCourseDatabase(), (Player) sender, parent.getId());
                    if (score.getTime() == Long.MAX_VALUE) {
                        sender.sendMessage(Parkour.getString("adv.notplayed"));
                    } else {
                        ((Player) sender).teleport(chap.getTeleport());
                        sender.sendMessage(Parkour.getString("adv.tp", chapter, course.getName()));
                    }
                } else {
                    ((Player) sender).teleport(chap.getTeleport());
                    sender.sendMessage(Parkour.getString("adv.tp", chapter, course.getName()));
                }
            } else {
                ((Player) sender).teleport(course.getCourses().get(0).getTeleport());
                sender.sendMessage(Parkour.getString("adv.tp", 1, course.getName()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            sender.sendMessage(Parkour.getString("error.invalidint"));
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage(Parkour.getString("adv.chap404"));
        }
        return true;
    }

}
