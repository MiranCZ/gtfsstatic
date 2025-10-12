package io.github.mirancz.gtfsparser.parsing;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class Csv {



    public static Csv parse(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream);

        return new Csv(parseCsvLine(scanner.nextLine()), scanner);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();

        while (!line.isEmpty()) {
            int nextDelim = line.indexOf(",");
            int nextQuote = line.indexOf("\"");
            if (nextQuote < nextDelim && nextQuote != -1) {
                int endQuote = line.indexOf("\"", nextQuote+1);

                result.add(line.substring(1, endQuote));
                line = line.substring(endQuote+1);
                if (line.startsWith(",")) {
                    line = line.substring(1);
                }
                continue;
            }
            if (nextDelim == -1) {
                result.add(line);
                break;
            }

            result.add(line.substring(0, nextDelim));
            line = line.substring(nextDelim+1);
        }

        return result;
    }


    private final Map<String, Integer> descriptorMap;
    private final Scanner scanner;

    private Csv(List<String> descriptors, Scanner scanner) {
        this.scanner = scanner;

        descriptorMap = new HashMap<>();
        for (int i = 0; i < descriptors.size(); i++) {
            String desc = descriptors.get(i);

            // idfk what this is
            if (((int)desc.charAt(0)) == 65279) {
                desc = desc.substring(1);
            }
            descriptorMap.put(desc, i);
        }
    }

    public Iterator<CsvLine> getLines() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return scanner.hasNextLine();
            }

            @Override
            public CsvLine next() {
                return new CsvLine(parseCsvLine(scanner.nextLine()));
            }
        };
    }


    public class CsvLine {
        public final List<String> line;

        private CsvLine(List<String> line) {
            this.line = line;
        }


        public String getOrDefault(String name, String defaultValue) {
            String result = get(name);
            if (result == null || result.isBlank()) return defaultValue;
            return result;
        }

        public String get(String name) {
            int id = Csv.this.descriptorMap.getOrDefault(name, -1);

            if (id == -1) return null;
            if (id >= line.size()) {
                return null;
            }
            return line.get(id);
        }

        public Boolean getBoolean(String name) {
            return getT(name, CsvLine::parseBool);
        }

        private static boolean parseBool(String str) {
            str = str.strip().toLowerCase();

            return str.equals("true") || str.equals("1");
        }

        public int getIntOrDefault(String name, int defaultValue) {
            Integer result = getInt(name);
            if (result == null) return defaultValue;

            return result;
        }

        public Integer getInt(String name) {
            try {
                return getT(name, Integer::parseInt);
            } catch (Exception e) {
                return null;
            }
        }

        public Double getDouble(String name) {
            return getT(name, Double::parseDouble);
        }

        public <T> T getT(String name, Function<String, T> mapper) {
            String value = get(name);
            if (value == null) return null;
            return mapper.apply(value);
        }

        @Override
        public String toString() {
            return "CsvLine{" +
                    "line=" + line +
                    '}';
        }
    }

}
