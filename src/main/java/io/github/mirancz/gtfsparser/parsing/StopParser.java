package io.github.mirancz.gtfsparser.parsing;

import io.github.mirancz.gtfsparser.util.CheckedOutputStream;
import io.github.mirancz.gtfsparser.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;

public class StopParser extends Parser {

    private final HashSet<Integer> processed = new HashSet<>();

    public StopParser() {
        subscribeTransformer("stops.txt", "stops", this::parseAndWrite);
    }

    public void parseAndWrite(InputStream input, CheckedOutputStream output) throws IOException {
        Csv stops = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = stops.getLines();

        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();
            int stopId = Utils.parseStop(line.get("stop_id")).stopId();
            if (processed.contains(stopId)) continue;
            processed.add(stopId);

            output.writeBoolean(true);
            output.writeInt(stopId);
            output.writeString(line.get("stop_name"));
            output.writeString(line.get("parent_station"));
            output.writeDouble(line.getDouble("stop_lat"));
            output.writeDouble(line.getDouble("stop_lon"));
        }
        output.writeBoolean(false);
    }

}
