package io.github.mirancz.gtfsparser.parsing;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class TripParser extends Parser {

    private List<Trip> trips = null;
    private List<Route> routes = null;
    private Map<String, Integer> headsignPool;

    public TripParser() {
    }

    private static void writeTripToRoute(DataOutputStream os, List<RouteStop> routeStops) throws IOException {
        for (RouteStop routeStop : routeStops) {
            os.writeInt(routeStop.stopId);
            os.writeInt(routeStop.tripId);
            os.writeShort(routeStop.postId);
            os.writeShort(routeStop.sequence);

            writeTime(os, routeStop.arrival);
            writeTime(os, routeStop.departure);
        }
    }

    private static void writeTime(DataOutputStream os, Time time) throws IOException {
        if (time.hours > Byte.MAX_VALUE) {
            throw new IllegalStateException("Possible overflow for time "+time);
        }

        os.write(time.hours);
        os.write(time.minutes);

        if (time.second != 0) {
            throw new IllegalStateException("Wrongly assumed seconds would be zero!");
        }
    }

    private static StopInfo parseStop(String stopUID) {
        int ind = stopUID.indexOf("Z");
        int stopId = Integer.parseInt(stopUID.substring(1, ind));
        int postId = Integer.parseInt(stopUID.substring(ind + 1));

        return new StopInfo(stopId, postId);
    }

    @Override
    protected void onFileInternal(String name, InputStream input, Function<String, DataOutputStream> outputProvider) throws Exception {
        if (name.equals("trips.txt")) {
            trips = parseTrips(input);
        }

        if (name.equals("stop_times.txt")) {
            routes = parseStopTimes(input, outputProvider.apply("stop_times"), outputProvider.apply("route_stops"));
        }

        if (trips != null && routes != null) {
            writeTrips(outputProvider.apply("trips"));
        }
    }

    private void writeTrips(DataOutputStream os) throws IOException {
        Map<Integer, Route> tripIdToRoute = new HashMap<>();
        for (Route route : routes) {
            tripIdToRoute.put(route.tripId(), route);
        }

        os.writeInt(headsignPool.size());
        for (Map.Entry<String, Integer> entry : headsignPool.entrySet()) {
            os.writeInt(entry.getValue());

            String name = entry.getKey();
            byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
            os.writeInt(bytes.length);

            os.write(bytes);
        }

        os.writeInt(trips.size());
        for (Trip trip : trips) {
            if (trip.serviceId > Short.MAX_VALUE || trip.lineId > Short.MAX_VALUE || trip.blockId > Short.MAX_VALUE) {
                throw new IllegalStateException();
            }

            os.writeShort(trip.serviceId);
            os.writeShort(trip.lineId);
            os.writeInt(trip.headsignId);
            os.writeShort(trip.blockId);
            os.write(trip.data);

            Route route = tripIdToRoute.get(trip.id);
            os.writeInt(route.startPos());
            int length = route.length;
            if (length > Byte.MAX_VALUE) {
                throw new IllegalStateException("Overflow for length "+length);
            }
            os.write(length);
        }
    }

    private List<Route> parseStopTimes(InputStream input, DataOutputStream os, DataOutputStream routes) throws Exception {
        Csv tripsCsv = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = tripsCsv.getLines();

        Route currentRoute = null;
        int prevSequence = -1;

        Map<Integer, List<Integer>> stopIdToRoute = new HashMap<>();
        List<RouteStop> routeStops = new ArrayList<>();

        int routeStopIndex = 0;
        int lineNumber = 0;

        List<Route> result = new ArrayList<>();

        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();

            int tripId = line.getInt("trip_id")-1;

            var stopInfo = parseStop(line.get("stop_id"));

            int sequence = line.getInt("stop_sequence");


            if (currentRoute == null) {
                currentRoute = new Route(tripId, lineNumber);
                prevSequence = sequence - 1;
            }


            // according to docs sequence must increase but can do so by arbitrary amount
            if (sequence > prevSequence) {
                if (currentRoute.tripId != tripId) {
                    throw new IllegalStateException(currentRoute + " ; " + tripId + " ; " + sequence);
                }

                RouteStop routeStop = new RouteStop(currentRoute.tripId, stopInfo.stopId, stopInfo.postId, sequence,
                        Time.parse(line.get("arrival_time")), Time.parse(line.get("departure_time"))
                );

                stopIdToRoute.computeIfAbsent(routeStop.stopId(), k -> new ArrayList<>()).add(routeStopIndex++);
                routeStops.add(routeStop);
            } else {
                currentRoute.length = lineNumber-currentRoute.startPos();
                result.add(currentRoute);
                currentRoute = new Route(tripId, lineNumber);

                RouteStop routeStop = new RouteStop(currentRoute.tripId(), stopInfo.stopId, stopInfo.postId, sequence,
                        Time.parse(line.get("arrival_time")), Time.parse(line.get("departure_time"))
                );
                stopIdToRoute.computeIfAbsent(routeStop.stopId(), k -> new ArrayList<>()).add(routeStopIndex++);
                routeStops.add(routeStop);
            }

            prevSequence = sequence;
            lineNumber++;

        }
        result.add(currentRoute);

        // stopId -> [tripId]
        // tripId -> [routeStop]

        os.writeInt(stopIdToRoute.size());

        for (Map.Entry<Integer, List<Integer>> entry : stopIdToRoute.entrySet()) {
            int stopId = entry.getKey();
            os.writeInt(stopId);

            var list = entry.getValue();
            os.writeInt(list.size());
            for (Integer routeStopId : list) {
                os.writeInt(routeStopId);
            }
        }

        writeTripToRoute(routes, routeStops);

        return result;
    }

    record Trip(int id, int serviceId, int lineId, int headsignId, int blockId, byte data) {
    }

    private List<Trip> parseTrips(InputStream input) throws IOException {
        Csv tripsCsv = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = tripsCsv.getLines();

        headsignPool = new HashMap<>();
        int poolId = 0;

        List<Trip> trips = new ArrayList<>();

        int expectedId = 0;
        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();

            int id = line.getInt("trip_id")-1;
            if (id != (expectedId++)) {
                throw new IllegalStateException();
            }

            String routeId = line.get("route_id");
            int serviceId = line.getInt("service_id");

            int lineId = Integer.parseInt(routeId.substring(1, routeId.indexOf("D")));

            String headSign = line.get("trip_headsign");

            int headsignId;
            if (headsignPool.containsKey(headSign)) {
                headsignId = headsignPool.get(headSign);
            } else {
                headsignPool.put(headSign, (headsignId = (poolId++)));
            }

            int blockId = line.getIntOrDefault("block_id", -1);
            boolean bikesAllowed = line.getBoolean("wheelchair_accessible");

            byte data = 0;
            if (bikesAllowed) data = 1;
            trips.add(new Trip(id, serviceId, lineId, headsignId, blockId, data));
        }


        return trips;
    }

    public static final class Route {
        private final int tripId;
        private final int startPos;
        private int length = -1;

        public Route(int tripId, int startPos) {
            this.tripId = tripId;
            this.startPos = startPos;
        }

        public int tripId() {
            return tripId;
        }

        public int startPos() {
            return startPos;
        }

        public int length() {
            return length;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Route) obj;
            return this.tripId == that.tripId &&
                    this.startPos == that.startPos &&
                    this.length == that.length;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tripId, startPos, length);
        }

        @Override
        public String toString() {
            return "Route[" +
                    "tripId=" + tripId + ", " +
                    "startPos=" + startPos + ", " +
                    "length=" + length + ']';
        }

        }

    private record StopInfo(int stopId, int postId) {
    }

    private record RouteStop(int tripId, int stopId, int postId, int sequence,
                             Time arrival, Time departure) {
    }

    public record Time(int hours, int minutes, int second) {

        public static Time parse(String time) {
            String[] parts = time.split(":");

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int second = Integer.parseInt(parts[2]);

            return new Time(hours, minutes, second);
        }


    }

}
