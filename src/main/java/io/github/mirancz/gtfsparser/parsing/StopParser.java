package io.github.mirancz.gtfsparser.parsing;


import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StopParser extends Parser {


    @Override
    public void parseLine(Csv.CsvLine line, DataOutputStream output) throws IOException {
        int stopId = parseStopId(line.get("stop_id"));

        String stopName = line.get("stop_name");

        output.writeInt(stopId);

        byte[] bytes = stopName.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);

        output.writeDouble(line.getDouble("stop_lat"));
        output.writeDouble(line.getDouble("stop_lon"));
    }

    private static int parseStopId(String stopId) {
        if (!stopId.startsWith("U")) {
            System.out.println("[WARN] Invalid stop " + stopId);
            return -1;
        }
        stopId = stopId.substring(1);
        int ind = Math.max(stopId.indexOf("Z"), stopId.indexOf("N"));
        if (ind == -1) {
            System.out.println("[WARN] Invalid stop  " + stopId);
            return -1;
        }

        return Integer.parseInt(stopId.substring(0, ind));
    }

}
