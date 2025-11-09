package io.github.mirancz.gtfsparser;


import io.github.mirancz.gtfsparser.parsing.*;
import io.github.mirancz.gtfsparser.util.IdStorage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    private static final String GTFS_URL = "https://kordis-jmk.cz/gtfs/gtfs.zip";
    private static final List<Parser> parsers;

    static {
        parsers = List.of(new StopParser(), new LineInfoParser(), new TripParser(), new ApiParser(), new CalendarParser(), new TransfersParser());
    }

    public static void main(String[] args) throws IOException {
        URL url = new URL(GTFS_URL);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        InputStream stream = con.getInputStream();

        Function<String, DataOutputStream> outputProvider = s -> {
            return getDataOutStream(getFileFor(s, true));
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

        writeStopIdMaps();

        Files.writeString(getDataRoot().resolve("info"), generateInfoString());
    }

    private static DataOutputStream getDataOutStream(File file) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        BufferedOutputStream outputStream = new BufferedOutputStream(fos);

        return new DataOutputStream(outputStream);
    }

    private static void writeStopIdMaps() throws IOException {
        DataOutputStream os = getDataOutStream(getStopMapsFile());
        IdStorage.STOP.write(os);
        os.close();
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
    
    public static File getStopMapsFile() {
        return getFileFor("stop_maps", true);
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

    private static String getFileHash(File file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fs = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;

            while ((bytesCount = fs.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        byte[] bytes = digest.digest();

        StringBuilder str = new StringBuilder();
        for (byte b : bytes) {
            str.append(String.format("%02x", b));
        }

        return str.toString();
    }


}