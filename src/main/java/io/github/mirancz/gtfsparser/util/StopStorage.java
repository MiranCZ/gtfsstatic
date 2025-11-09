package io.github.mirancz.gtfsparser.util;

import io.github.mirancz.gtfsparser.parsing.TripParser;

import java.util.HashMap;
import java.util.Map;

public class StopStorage {

    private static final Map<Integer, Integer> stopIdToIndex = new HashMap<>();
    private static int ID = 0;


    public static int getId(int stopId) {
        return stopIdToIndex.computeIfAbsent(stopId, k -> ID++);
    }

}
