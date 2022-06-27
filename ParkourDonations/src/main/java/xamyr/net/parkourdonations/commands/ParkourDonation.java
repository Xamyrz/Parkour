package xamyr.net.parkourdonations.commands;

import me.cmastudios.mcparkour.data.ParkourCourse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import xamyr.net.parkourdonations.Donations.MapDonation;
import xamyr.net.parkourdonations.ParkourDonations;
import xamyr.net.parkourdonations.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ParkourDonation implements TabExecutor {
    private final ParkourDonations plugin;
    public ParkourDonation(ParkourDonations plugin) {this.plugin = plugin;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 4) {
            if (args[0].equals("map")) {
               if (Utils.isNumeric(args[1])) {
                   if (Utils.isNumeric(args[2])) {
                       MapDonation map = plugin.mapDonations.get(Integer.parseInt(args[1]));
                       if (plugin.parkour.courses.get(Integer.parseInt(args[1])) != null) {
                           plugin.parkour.courses.get(Integer.parseInt(args[1])).setMode(ParkourCourse.CourseMode.DONATION);
                           if (map != null) {
                               map.setMinutes(Long.parseLong(args[2]));
                               map.donationUpdate();
                               map.insertDonation();
                               map.insertDonationHistory();
                               sender.sendMessage("Map donation from " + args[3] + " extends " + args[1] + " parkour by another " + Integer.parseInt(args[2]) + "minutes");
                           } else {
                               MapDonation newMap = new MapDonation(args[3], Integer.parseInt(args[1]), Integer.parseInt(args[2]), plugin);
                               newMap.insertDonation();
                               newMap.insertDonationHistory();
                               plugin.mapDonations.put(Integer.parseInt(args[1]), newMap);
                               sender.sendMessage("Map donation from " + args[3] + " for parkour " + args[1] + " for " + Integer.parseInt(args[2]) + "minutes");
                           }
                       } else {
                           sender.sendMessage("Parkour not found");
                       }
                   } else { return false; }
               } else { return false; }
            } else { return false; }
        } else { return false; }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        List<String> type = new ArrayList<>();
        type.add("map");

        List<String> parkourId = new ArrayList<>();
        parkourId.add("5");
        parkourId.add("4");
        parkourId.add("3");
        parkourId.add("2");
        parkourId.add("1");

        List<String> time = new ArrayList<>();
        time.add("5");
        time.add("10");
        time.add("15");

        if(args.length == 1) return type;
        if(args.length == 2) return parkourId;
        else if(args.length == 3) return time;
        return null;
    }
}
