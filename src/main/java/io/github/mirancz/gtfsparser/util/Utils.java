package io.github.mirancz.gtfsparser.util;

public class Utils {

    public static StopInfo parseStop(String stopUID) {
        // format is U{stopId}Z{postId} or U{stopId}N{postId}

        int zInd = stopUID.indexOf("Z");
        int nInd = stopUID.indexOf("N");
        int ind = Math.max(zInd, nInd);

        if (ind == -1) throw new IllegalArgumentException("Invalid stop UID: " + stopUID);

        int stopId = Integer.parseInt(stopUID.substring(1, ind));
        int postId = Integer.parseInt(stopUID.substring(ind + 1));

        int mappedStopId = IdStorage.STOP.getId(stopId);

        if (mappedStopId > Short.MAX_VALUE || postId > Short.MAX_VALUE) {
            throw new IllegalStateException();
        }

        return new StopInfo((short) mappedStopId, (short) postId);
    }

}
