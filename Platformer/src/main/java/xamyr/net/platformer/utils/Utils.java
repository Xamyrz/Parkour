package xamyr.net.platformer.utils;

import xamyr.net.platformer.platform.PlatformCreate;
import xamyr.net.platformer.worldedit.BlocksSelection;

public class Utils {
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
