package io.github.mirancz.gtfsparser.parsing;

import java.io.*;
import java.util.Iterator;

public abstract class Parser {

    public final void parse(InputStream input, DataOutputStream output) {
        try {
            Csv stops = Csv.parse(input);

            Iterator<Csv.CsvLine> lines = stops.getLines();

            while (lines.hasNext()) {
                output.writeBoolean(true);
                Csv.CsvLine line = lines.next();
                parseLine(line, output);
            }
            output.writeBoolean(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void parseLine(Csv.CsvLine line, DataOutputStream output) throws Exception;

}
