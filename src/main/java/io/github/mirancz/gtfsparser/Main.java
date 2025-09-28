package io.github.mirancz.gtfsparser;


import io.github.mirancz.gtfsparser.parsing.LineInfoParser;
import io.github.mirancz.gtfsparser.parsing.Parser;
import io.github.mirancz.gtfsparser.parsing.StopParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    private static final String GTFS_URL = "https://kordis-jmk.cz/gtfs/gtfs.zip";
    private static final Map<String, Parser> parsers;

    static {
        parsers = Map.of(
                "stops.txt", new StopParser(),
                "routes.txt", new LineInfoParser()
        );
    }

    public static void main(String[] args) throws IOException {
        URL url = new URL(GTFS_URL);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        InputStream stream = con.getInputStream();

        ZipInputStream zip = new ZipInputStream(stream);

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();

            if (parsers.containsKey(name)) {
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(getFileFor(entry.getName(), true)));

                parsers.get(name).parse(new BufferedInputStream(zip), new DataOutputStream(outputStream));
                outputStream.close();
            }

            Files.write(getFileFor(entry.getName(), false).toPath(), zip.readAllBytes());
        }

        zip.close();
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

        return root.resolve(name.substring(0, name.lastIndexOf('.'))).toFile();
    }



}