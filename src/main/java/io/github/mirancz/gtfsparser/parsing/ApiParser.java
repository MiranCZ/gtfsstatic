package io.github.mirancz.gtfsparser.parsing;

import io.github.mirancz.gtfsparser.util.Pair;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ApiParser extends Parser {

    private static final String START_TEXT = "Linka/CVlaku = trip_id: ";

    public ApiParser(){
        subscribeTransformer("api.txt", "api", this::parseAndWrite);
    }

    protected void parseAndWrite(InputStream input, DataOutputStream output) throws Exception {
        Scanner scanner = new Scanner(input);


        List<Pair<Integer, Integer>> result = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (!line.startsWith(START_TEXT)) {
                System.out.println("Invalid line: "+line);
                continue;
            }

            line = line.substring(START_TEXT.length());

            String[] parts = line.split("=");

            String[] lineIdParts = parts[0].split("/");

            int lineId = Integer.parseInt(lineIdParts[0].strip());
            int routeId = Integer.parseInt(lineIdParts[1].strip());

            int tripId = Integer.parseInt(parts[1].strip());

            if (lineId > Short.MAX_VALUE || routeId > Short.MAX_VALUE) {
                throw new IllegalArgumentException("OOPS we got an overflow... "+lineId + " "+routeId);
            }

            result.add(new Pair<>(tripId, ((lineId<<16) | routeId)));
        }

        output.writeInt(result.size());
        for (Pair<Integer, Integer> pair : result) {
            output.writeInt(pair.left());
            output.writeInt(pair.right());
        }
    }

}
