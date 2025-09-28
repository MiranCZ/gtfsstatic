package io.github.mirancz.gtfsparser.parsing;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class LineInfoParser extends Parser {

    @Override
    protected void parseAndWrite(InputStream input, DataOutputStream output) throws Exception {
        Csv stops = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = stops.getLines();

        while (lines.hasNext()) {
            output.writeBoolean(true);
            Csv.CsvLine line = lines.next();

            int routeId = parseRoute(line.get("route_id"));
            String name = line.get("route_short_name");
            Color backgroundColor = parseColor(line.get("route_color"));
            Color textColor = parseColor(line.getOrDefault("route_text_color", "000000"));


            output.writeInt(routeId);

            byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
            output.writeInt(bytes.length);
            output.write(bytes);

            writeColor(output, backgroundColor);
            writeColor(output, textColor);
        }
        output.writeBoolean(false);
    }

    private static void writeColor(DataOutputStream output, Color backgroundColor) throws IOException {
        output.writeInt(backgroundColor.getRed());
        output.writeInt(backgroundColor.getGreen());
        output.writeInt(backgroundColor.getBlue());
    }

    private static Color parseColor(String color) {
        if (color.length() != 6) {
            throw new IllegalArgumentException("Unexpected color argument: "+color);
        }

        return new Color(
                Integer.parseInt(color.substring(0,2), 16),
                Integer.parseInt(color.substring(2,4), 16),
                Integer.parseInt(color.substring(4,6), 16)
                );
    }

    private static int parseRoute(String routeId) {
        if (!routeId.startsWith("L")) {
            return -1;
        }
        routeId = routeId.substring(1);
        int zInd = routeId.indexOf("D");
        if (zInd == -1) return -1;

        return Integer.parseInt(routeId.substring(0, zInd));
    }


}
