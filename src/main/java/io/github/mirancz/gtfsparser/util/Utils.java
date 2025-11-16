package io.github.mirancz.gtfsparser.util;

public class Utils {

    public static StopInfo parseStop(String stopUID) {
        int ind = stopUID.indexOf("Z");
        int stopId = Integer.parseInt(stopUID.substring(1, ind));
        int postId = Integer.parseInt(stopUID.substring(ind + 1));

        int mappedStopId = IdStorage.STOP.getId(stopId);

        if (mappedStopId > Short.MAX_VALUE || postId > Short.MAX_VALUE) {
            throw new IllegalStateException();
        }

        return new StopInfo((short) mappedStopId, (short) postId);
    }


}
