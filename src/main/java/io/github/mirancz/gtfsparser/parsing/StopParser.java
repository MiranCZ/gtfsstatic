package io.github.mirancz.gtfsparser.parsing;



import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;

public class StopParser extends Parser {

    private final HashSet<Integer> processed = new HashSet<>();

    @Override
    public void parseAndWrite(InputStream input, DataOutputStream output) throws IOException {
        Csv stops = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = stops.getLines();

        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();
            int stopId = parseStopId(line.get("stop_id"));
            if (processed.contains(stopId)) continue;
            processed.add(stopId);

            output.writeBoolean(true);

            String stopName = line.get("stop_name");

            output.writeInt(stopId);

            byte[] bytes = stopName.getBytes(StandardCharsets.UTF_8);
            output.writeInt(bytes.length);
            output.write(bytes);

            output.writeDouble(line.getDouble("stop_lat"));
            output.writeDouble(line.getDouble("stop_lon"));
        }
        output.writeBoolean(false);
    }

    private static int parseStopId(String stopId) {
        if (!stopId.startsWith("U")) {
            System.out.println("[WARN] Invalid stop "+stopId);
            return -1;
        }
        stopId = stopId.substring(1);
        int ind = Math.max(stopId.indexOf("Z"), stopId.indexOf("N"));
        if (ind == -1) {
            System.out.println("[WARN] Invalid stop  "+stopId);
            return -1;
        }

        return Integer.parseInt(stopId.substring(0, ind));
    }

}
