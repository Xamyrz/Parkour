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
import me.cmastudios.mcparkour.Utils;
import me.cmastudios.mcparkour.event.*;
import me.cmastudios.mcparkour.data.ParkourCourse;
import me.cmastudios.mcparkour.event.modes.DistanceRushParkourEvent;
import me.cmastudios.mcparkour.event.modes.PlaysRushParkourEvent;
import me.cmastudios.mcparkour.event.modes.TimeRushParkourEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class EventCommand implements CommandExecutor {
    private final Parkour plugin;

    public EventCommand(Parkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "create":
                    if (!sender.hasPermission("parkour.set")) {
                        sender.sendMessage(Parkour.getString("error.permission"));
                        return true;
                    }
                    if (args.length < 3) {
                        return false;
                    }
                    try {
                        int pkid = Integer.parseInt(args[1]);
                        EventCourse.EventType type = EventCourse.EventType.valueOf(args[2]);
                        EventCourse course = EventCourse.loadCourse(plugin.getCourseDatabase(), pkid);

                        if (course != null) {
                            course.setType(type);
                            course.save(plugin.getCourseDatabase());
                            sender.sendMessage(Parkour.getString("event.admin.modified", pkid));
                            return true;
                        }
                        ParkourCourse parkour = ParkourCourse.loadCourse(plugin.getCourseDatabase(), pkid);
                        if (parkour == null) {
                            sender.sendMessage(Parkour.getString("event.admin.error.invalidcourse"));
                            return true;
                        }
                        course = new EventCourse(type, parkour);
                        course.save(plugin.getCourseDatabase());
                        sender.sendMessage(Parkour.getString("event.admin.set"));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(Parkour.getString("event.admin.error.invalidtype"));
                        return true;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    break;
                case "start":
                    if (!sender.hasPermission("parkour.set")) {
                        sender.sendMessage(Parkour.getString("error.permission"));
                        return true;
                    }
                    if (plugin.getEvent() != null) {
                        sender.sendMessage(Parkour.getString("event.admin.error.alreadystarted"));
                        return true;
                    }
                    EventCourse pk;
                    ParkourEvent course = null;
                    try {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("random")) {
                                pk = EventCourse.getRandomCourse(plugin.getCourseDatabase());
                                String[] interval = plugin.getConfig().getString("events."+pk.getType().key+".preferredTimeInterval").split("-");
                                if(interval.length!=2) {
                                    sender.sendMessage(Parkour.getString("event.admin.error.badlyconfigured"));
                                    return true;
                                }
                                if (pk == null) {
                                    sender.sendMessage(Parkour.getString("event.admin.error.invalidcourse"));
                                    return true;
                                }
                                course = switch (pk.getType()) {
                                    case DISTANCE_RUSH -> new DistanceRushParkourEvent(pk, plugin, getRandomFromRange(Integer.parseInt(interval[0]), Integer.parseInt(interval[1])));
                                    case PLAYS_RUSH -> new PlaysRushParkourEvent(pk, plugin, getRandomFromRange(Integer.parseInt(interval[0]), Integer.parseInt(interval[1])));
                                    case TIME_RUSH -> new TimeRushParkourEvent(pk, plugin, getRandomFromRange(Integer.parseInt(interval[0]), Integer.parseInt(interval[1])));
                                };
                            } else {
                                return false;
                            }
                        } else if (args.length == 3) {
                            int length = Integer.parseInt(args[1]);
                            int parkourId = Integer.parseInt(args[2]);
                            pk = EventCourse.loadCourse(plugin.getCourseDatabase(), parkourId);
                            if (pk == null) {
                                sender.sendMessage(Parkour.getString("event.admin.error.invalidcourse"));
                                return true;
                            }
                            course = switch (pk.getType()) {
                                case DISTANCE_RUSH -> new DistanceRushParkourEvent(pk, plugin, length);
                                case PLAYS_RUSH -> new PlaysRushParkourEvent(pk, plugin, length);
                                case TIME_RUSH -> new TimeRushParkourEvent(pk, plugin, length);
                            };
                        } else {
                            return false;
                        }
                        if (pk == null) {
                            sender.sendMessage(Parkour.getString("event.admin.error.invalidcourse"));
                            return true;
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

                    if (course == null) {
                        sender.sendMessage(Parkour.getString("event.admin.error.invalid"));
                        return true;
                    }
                    plugin.setEvent(course);
                    course.prepare();
                    break;
                case "end":
                    if (!sender.hasPermission("parkour.set")) {
                        sender.sendMessage(Parkour.getString("error.permission"));
                        return true;
                    }
                    if (plugin.getEvent() != null) {
                        plugin.getEvent().end();
                        sender.sendMessage(Parkour.getString("event.ended"));
                        return true;
                    }
                    sender.sendMessage(Parkour.getString("event.notrunning"));
                    break;
                default:
                    return false;
            }

            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Parkour.getString("error.playerreq"));
            return true;
        }
        if (plugin.getEvent() != null) {
            ((Player) sender).teleport(plugin.getEvent().getCourse().getCourse().getTeleport());
        } else {
            sender.sendMessage(Parkour.getString("event.notrunning"));
        }
        return true;
    }

    private int getRandomFromRange(int num1, int num2) {
        int min = Math.min(num1,num2);
        return Utils.RANDOM.nextInt(((min == num2 ? num1 : num2) - min) + 1) + min;
    }
}
