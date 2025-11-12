package io.github.mirancz.gtfsparser;


import io.github.mirancz.gtfsparser.parsing.*;
import io.github.mirancz.gtfsparser.util.CheckedOutputStream;
import io.github.mirancz.gtfsparser.util.IdStorage;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        parsers = List.of(new StopParser(), new LineInfoParser(), new TripParser(), new ApiParser(), new CalendarParser(), new TransfersParser());
    }

    public static void main(String[] args) throws IOException {
        URL url = new URL(GTFS_URL);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        InputStream stream = con.getInputStream();

        XZOutputStream compressedOutput = new XZOutputStream(new FileOutputStream(getDataRoot().resolve("data").toFile()), new LZMA2Options());
        DataOutputStream output = new DataOutputStream(compressedOutput);

        Function<String, CheckedOutputStream> outputProvider = s -> {
            return getWrappedDataOutputStream(s, output);
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
        }

        for (Parser parser : parsers) {
            parser.onFinish(outputProvider);
        }

        zip.close();

        writeStopIdMaps(outputProvider);
        writePosts(outputProvider);

        output.writeBoolean(false);
        output.close();

        Files.writeString(getDataRoot().resolve("info"), generateInfoString());
    }

    private static void writePosts(Function<String, CheckedOutputStream> outputProvider) throws IOException {
        CheckedOutputStream os = outputProvider.apply("posts");

        os.write(Files.readAllBytes(getDataRoot().resolve("storage").resolve("posts")));

        os.close();
    }

    private static CheckedOutputStream getWrappedDataOutputStream(String s, DataOutputStream output) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        return new CheckedOutputStream(os) {
            @Override
            public void close() throws IOException {
                byte[] data = os.toByteArray();

                output.writeBoolean(true);

                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                output.writeInt(bytes.length);
                output.write(bytes);

                output.writeInt(data.length);
                output.write(data);
                super.close();
            }
        };
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

    private static void writeStopIdMaps(Function<String, CheckedOutputStream> outputProvider) throws IOException {
        CheckedOutputStream os = outputProvider.apply("stop_mapping");
        IdStorage.STOP.write(os);
        os.close();

        DataOutputStream s = getDataOutStream(getStopMapsFile());
        IdStorage.STOP.write(new CheckedOutputStream(s));
        s.close();
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
        return getDataRoot().resolve("storage").resolve("stop_maps").toFile();
    }


}