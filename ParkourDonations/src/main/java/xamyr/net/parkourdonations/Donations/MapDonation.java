package xamyr.net.parkourdonations.Donations;

import org.bukkit.Bukkit;
import xamyr.net.parkourdonations.ParkourDonations;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

public class MapDonation {
    private ParkourDonations plugin;
    private int parkourId;
    private long minutes;
    private String playerName;
    private Timestamp startTime;
    private Timestamp endTime;
    public MapDonation(String playerName, int parkourId, int minutes, ParkourDonations plugin) {
        this.plugin = plugin;
        this.parkourId = parkourId;
        this.minutes = minutes;
        this.playerName = playerName;
        Date date = new Date();
        this.startTime = new Timestamp(date.getTime());
        this.endTime = startTime;
        this.endTime.setTime(endTime.getTime() + ((this.minutes * 60) * 1000));
    }

    public void donationUpdate() {
        Connection conn = plugin.getDatabase();
        try (PreparedStatement statement = conn.prepareStatement("SELECT * from donationpk WHERE `pkid` = ?")) {
            statement.setInt(1, parkourId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    startTime = result.getTimestamp("start");
                    endTime = result.getTimestamp("end");
                    endTime.setTime(endTime.getTime() + ((minutes * 60) * 1000));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger("error finding donationpk in DB....").log(Level.SEVERE, null, ex);
        }
    }

    public void insertDonation() {
        Connection conn = plugin.getDatabase();
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO `donationpk` (`pkid`,`start`,`end`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `end` = VALUES(`end`)")) {
            statement.setInt(1, parkourId);
            statement.setTimestamp(2, startTime);
            statement.setTimestamp(3, endTime);
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger("error inserting donationpk to DB....").log(Level.SEVERE, null, ex);
        }
    }

    public void insertDonationHistory() {
        Connection conn = plugin.getDatabase();
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO `donationhistorypk` (`player`,`pkid`,`date`,`minutes`) VALUES (?,?,?,?)")) {
            statement.setString(1, playerName);
            statement.setInt(2, parkourId);
            statement.setTimestamp(3, startTime);
            statement.setInt(4, (int) minutes);
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger("error inserting donationpk to DB....").log(Level.SEVERE, null, ex);
        }
    }

    public void expire(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM `donationpk` WHERE pkid = ?")) {
            stmt.setInt(1, parkourId);
            stmt.executeUpdate();
        }
    }

    public long getMinutes() {
        return minutes;
    }

    public void setMinutes(long minutes) {
        this.minutes = minutes;
    }

    public Timestamp getEndTime() {
        return endTime;
    }
}
