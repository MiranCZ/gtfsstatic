package io.github.mirancz.gtfsparser;


import io.github.mirancz.gtfsparser.parsing.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    private static final String GTFS_URL = "https://kordis-jmk.cz/gtfs/gtfs.zip";
    private static final List<Parser> parsers;

    static {
        parsers = List.of(new StopParser(), new LineInfoParser(), new TripParser(), new ApiParser(), new CalendarParser());
    }

    public static void main(String[] args) throws IOException {
        URL url = new URL(GTFS_URL);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        InputStream stream = con.getInputStream();

        Function<String, DataOutputStream> outputProvider = s -> {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(getFileFor(s, true));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            BufferedOutputStream outputStream = new BufferedOutputStream(fos);

            return new DataOutputStream(outputStream);
        };
        ZipInputStream zip = new ZipInputStream(stream);

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            BufferedInputStream bif = new BufferedInputStream(zip);
            bif.mark(Integer.MAX_VALUE);

            for (Parser parser : parsers) {
                parser.onFile(name, bif, outputProvider);
                bif.reset();
            }

            Files.write(getFileFor(entry.getName(), false).toPath(), bif.readAllBytes());
        }

        for (Parser parser : parsers) {
            parser.onFinish(outputProvider);
        }

        zip.close();

        Files.writeString(getDataRoot().resolve("info"), generateInfoString());
    }

    private static String generateInfoString() {
        return "{" +
                "\"lastUpdated\":"+System.currentTimeMillis()+
                "}";
    }

    private static Path getDataRoot() {
        File file = new File("docs/");
        if (!file.exists()) {
            file.mkdir();
        }

        return file.toPath();
    }

    private static File getFileFor(String name, boolean generated) {
        Path root = getDataRoot();
        if (generated) {
            root = root.resolve("parsed");
            if (!Files.exists(root)) {
                root.toFile().mkdir();
            }
        } else {
            root = root.resolve("unzipped");
            if (!Files.exists(root)) {
                root.toFile().mkdir();
            }
        }
        if (name.contains(".")) {
           name = name.substring(0, name.lastIndexOf('.'));
        }

        return root.resolve(name).toFile();
    }


}