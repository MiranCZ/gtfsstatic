package io.github.mirancz.gtfsparser.parsing;

import io.github.mirancz.gtfsparser.util.CheckedOutputStream;
import io.github.mirancz.gtfsparser.util.IdStorage;
import io.github.mirancz.gtfsparser.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class TripParser extends Parser {

    private List<Trip> trips = null;
    private List<Route> routes = null;
    private Map<String, Integer> headsignPool;
    private boolean wrote = false;

    public TripParser() {
    }

    private static void writeTripToRoute(CheckedOutputStream os, List<RouteStop> routeStops) throws IOException {
        for (int i = 0; i < routeStops.size(); i++) {
            RouteStop routeStop = routeStops.get(i);

            if (i != routeStop.id) throw new IllegalStateException();

            os.writeShort(routeStop.stopId);
            os.writeInt(routeStop.tripId);
            os.writeShort(routeStop.postId);
            os.writeShort(routeStop.sequence);

            writeTime(os, routeStop.arrival);
            writeTime(os, routeStop.departure);
        }
    }

    private static void writeTime(CheckedOutputStream os, Time time) throws IOException {
        os.writeByte(time.hours);
        os.writeByte(time.minutes);

        if (time.second != 0) {
            throw new IllegalStateException("Wrongly assumed seconds would be zero!");
        }
    }


    @Override
    protected void onFileInternal(String name, InputStream input, Function<String, CheckedOutputStream> outputProvider) throws Exception {
        if (name.equals("trips.txt")) {
            trips = parseTrips(input);
        }

        if (name.equals("stop_times.txt")) {
            routes = parseStopTimes(input, outputProvider.apply("stop_times"), outputProvider.apply("route_stops"));
        }

        if (trips != null && routes != null && !wrote) {
            writeTrips(outputProvider.apply("trips"));
            writeStopIdToRoute(outputProvider.apply("stop_to_route"));
            wrote = true;
        }
    }

    private void writeTrips(CheckedOutputStream os) throws IOException {
        Map<Integer, Route> tripIdToRoute = new HashMap<>();
        for (Route route : routes) {
            tripIdToRoute.put(route.tripId(), route);
        }

        os.writeInt(headsignPool.size());
        for (Map.Entry<String, Integer> entry : headsignPool.entrySet()) {
            os.writeInt(entry.getValue());

            String name = entry.getKey();
            
            os.writeString(name); 
        }

        os.writeInt(trips.size());
        for (Trip trip : trips) {
            os.writeShort(trip.serviceId);
            os.writeShort(trip.lineId);
            os.writeInt(trip.headsignId);
            os.writeShort(trip.blockId);
            os.writeByte(trip.data);

            Route route = tripIdToRoute.get(trip.id);
            os.writeInt(route.startPos());
            int length = route.length;

            os.writeByte(length);
        }
    }

    private List<Route> parseStopTimes(InputStream input, CheckedOutputStream os, CheckedOutputStream routes) throws Exception {
        Csv tripsCsv = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = tripsCsv.getLines();

        Route currentRoute = null;
        int prevSequence = -1;

        Map<Short, List<Integer>> stopIdToRoute = new HashMap<>();
        List<RouteStop> routeStops = new ArrayList<>();

        int routeStopIndex = 0;
        int lineNumber = 0;

        List<Route> result = new ArrayList<>();

        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();

            int tripId = IdStorage.TRIP.getId(line.getInt("trip_id"));

            var stopInfo = Utils.parseStop(line.get("stop_id"));

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

                RouteStop routeStop = new RouteStop(routeStops.size(), currentRoute.tripId, stopInfo.stopId(), stopInfo.postId(), sequence,
                        Time.parse(line.get("arrival_time")), Time.parse(line.get("departure_time"))
                );

                stopIdToRoute.computeIfAbsent(routeStop.stopId(), k -> new ArrayList<>()).add(routeStopIndex++);
                routeStops.add(routeStop);
                currentRoute.stops.add(routeStop);
            } else {
                currentRoute.length = lineNumber-currentRoute.startPos();
                result.add(currentRoute);
                currentRoute = new Route(tripId, lineNumber);

                RouteStop routeStop = new RouteStop(routeStops.size(), currentRoute.tripId(), stopInfo.stopId(), stopInfo.postId(), sequence,
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

        for (Map.Entry<Short, List<Integer>> entry : stopIdToRoute.entrySet()) {
            short stopId = entry.getKey();
            os.writeShort(stopId);

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

            int id = IdStorage.TRIP.getId(line.getInt("trip_id"));
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

    private void writeStopIdToRoute(CheckedOutputStream os) throws IOException {
        List<RoutesContainer> routeContainers = parseContainers();

        int max = 0;
        for (RoutesContainer cont : routeContainers) {
            for (RouteStopsContainer stop : cont.stops()) {
                int id = stop.stopId();
                max = Math.max(max, id);
            }
        }

        List<RouteStopsContainer>[] map = new List[max+1];

        for (RoutesContainer cont : routeContainers) {
            for (RouteStopsContainer stop : cont.stops()) {
                int id = stop.stopId();

                if (map[id] == null) {
                    map[id] = new ArrayList<>();
                }
                map[id].add(stop);
            }
        }

        os.writeInt(map.length);

        for (List<RouteStopsContainer> routeStopsContainers : map) {
            if (routeStopsContainers == null) {
                os.writeInt(0);
                continue;
            }
            os.writeInt(routeStopsContainers.size());
            for (RouteStopsContainer container : routeStopsContainers) {
                os.writeShort(container.stopId); // FIXME not needed, just equal to the index
                os.writeShort(container.postId);
                os.writeShort(container.serviceId);
                writeTime(os, container.startTime);

                os.writeShort(container.stops.length);

                for (long stop : container.stops) {
                    os.writeLong(stop);
                }
            }
        }
    }

    private List<RoutesContainer> parseContainers() {
        record Entry(List<Long> stops, int serviceId) {


            static Entry of(List<RouteStop> rs, int serviceId) {
                return new Entry(rs.stream().map(s -> ((long)s.stopId()<<32) | (s.postId() & 0xFFFFFFL)).toList(), serviceId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(stops, serviceId);
            }

            @Override
            public boolean equals(Object object) {
                if (!(object instanceof Entry(List<Long> otherStops, int id))) return false;
                return Objects.equals(stops, otherStops) && this.serviceId == id;
            }
        }

        Map<Entry, List<List<RouteStop>>> map = new HashMap<>();

        for (Route route : routes) {
            Entry entry = Entry.of(route.stops, trips.get(route.tripId).serviceId);

            if (map.containsKey(entry)) {
                map.get(entry).add(route.stops);
            } else {
                map.put(entry, new ArrayList<>(List.of(route.stops)));
            }
        }

        List<RoutesContainer> routeContainers = new ArrayList<>();

        int routeID = 0;

        for (List<List<RouteStop>> value : map.values()) {
            value.sort(Comparator.comparing(l -> l.getFirst().departure()));

            List<RouteStopsContainer> containers = new ArrayList<>();
            for (int i = 0; i < value.getFirst().size(); i++) {
                short stopId = value.getFirst().get(i).stopId();
                short postId = value.getFirst().get(i).postId();
                int serviceId = trips.get(value.getFirst().get(i).tripId()).serviceId();

                Time startTime = value.getFirst().get(i).departure();
                long[] stops = new long[value.size()-1];

                for (int j = 1; j < value.size(); j++) {
                    List<RouteStop> routeStops = value.get(j);
                    RouteStop stop = routeStops.get(i);

                    if (stop.stopId() != stopId || stop.postId() != postId) {
                        for (List<RouteStop> r : value) {
                            System.out.println(r.stream().map(s -> (s.stopId()+":"+s.postId())).toList());
                        }
                        throw new IllegalStateException();
                    }

                    long data = ((long) stop.id()<<32) | (stop.departure().getMinsDiff(startTime) & 0xFFFFFFFFL);

                    stops[j-1] = data;
                }

                RouteStopsContainer container = new RouteStopsContainer(stopId, postId, (short) serviceId, startTime, stops);
                containers.add(container);
            }

            routeContainers.add(new RoutesContainer(routeID++, containers));
        }

        return routeContainers;
    }

    public static final class Route {
        private final int tripId;
        private final int startPos;
        private final List<RouteStop> stops;
        private int length = -1;

        public Route(int tripId, int startPos) {
            this.tripId = tripId;
            this.startPos = startPos;
            this.stops = new ArrayList<>();
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



    private record RouteStop(int id, int tripId, short stopId, short postId, int sequence,
                             Time arrival, Time departure) {
    }

    public record Time(int hours, int minutes, int second) implements Comparable<Time> {

        public static Time parse(String time) {
            String[] parts = time.split(":");

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int second = Integer.parseInt(parts[2]);

            return new Time(hours, minutes, second);
        }


        public int getMinsDiff(Time other) {
            return (hours - other.hours) * 60 + (minutes - other.minutes);
        }

        @Override
        public int compareTo(Time o) {
            if (hours == o.hours) {
                return minutes - o.minutes;
            }
            return hours - o.hours;
        }
    }

    public record RoutesContainer(int id, List<RouteStopsContainer> stops) {
    }

    /**
     * @param stops An array of packed ints in the form of {@code (routeStopId<<32 | minOffset)}
     */
    public record RouteStopsContainer(short stopId, short postId, short serviceId, Time startTime, long[] stops) {
    }


}
