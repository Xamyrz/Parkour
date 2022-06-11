/*
 * Copyright (C) 2014 Maciej Mionskowski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.cmastudios.mcparkour.commands;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.CustomCourse;
import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class CustomCourseCommand implements CommandExecutor {
    private final Parkour plugin;

    public CustomCourseCommand(Parkour instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }
        try {
            int parkourId = Integer.parseInt(args[1]);
            CustomCourse custom = CustomCourse.loadCourse(plugin.courses.get(parkourId), plugin.getCourseDatabase(), parkourId);
            ParkourCourse course = plugin.courses.get(parkourId);
            if (course == null) {
                sender.sendMessage(Parkour.getString("error.course404"));
                return true;
            }
            if (course.getMode() != ParkourCourse.CourseMode.CUSTOM) {
                sender.sendMessage(Parkour.getString("custom.error.notvalidcourse"));
                return true;
            }
            switch (args[0]) {
                case "create":
                    if (args.length < 3) {
                        return false;
                    }
                    String[] effects = Arrays.copyOfRange(args, 2, args.length);
                    ArrayList<PotionEffect> effs = new ArrayList<>();
                    for (String effect : effects) {
                        String[] parts = effect.split(":");
                        if (parts.length != 2) {
                            sender.sendMessage(Parkour.getString("custom.error.badeffectssyntax"));
                            return true;
                        }
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        if (type == null) {
                            sender.sendMessage(Parkour.getString("custom.error.effect404", parts[0]));
                            return true;
                        }
                        for (PotionEffect eff : effs) {
                            if (eff.getType() == type) {
                                sender.sendMessage(Parkour.getString("custom.error.duplicating", type.getName()));
                                return true;
                            }
                        }
                        effs.add(new PotionEffect(type, Integer.MAX_VALUE, Integer.parseInt(parts[1])));
                    }
                    if (custom != null) {
                        custom.setEffects(effs);
                        sender.sendMessage(Parkour.getString("custom.modified", parkourId));
                    } else {
                        custom = new CustomCourse(course, effs);
                        sender.sendMessage(Parkour.getString("custom.created", parkourId));
                    }
                    custom.save(plugin.getCourseDatabase());

                    break;
                case "delete":
                    PreparedStatement stmt = plugin.getCourseDatabase().prepareStatement("DELETE FROM custom WHERE id=?");
                    stmt.setInt(1,parkourId);
                    stmt.executeUpdate();
                    sender.sendMessage(Parkour.getString("custom.deleted",parkourId));
                    break;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Parkour.getString("error.invalidint"));
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Parkour.getString("event.admin.error.invalidtype"));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}