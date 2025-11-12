package io.github.mirancz.gtfsparser.util;

import io.github.mirancz.gtfsparser.Main;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class IdStorage {

    public static final MappingIndexer STOP;

    public static final Indexer TRIP = (original -> original-1);

    static {
        STOP = load(Main.getStopMapsFile());
    }

    private static MappingIndexer load(File file) {
        if (!file.exists()) return new MappingIndexer(new HashMap<>());

        try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {

            int size = is.readInt();

            Map<Integer, Integer> map = new HashMap<>();

            for (int i = 0; i < size; i++) {
                map.put(is.readInt(), i);
            }

            return new MappingIndexer(map);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    public interface Indexer {
        int getId(int original);
    }

    public static class MappingIndexer implements Indexer {

        private final Map<Integer, Integer> idToIndex;
        private int ID = 0;

        private MappingIndexer(Map<Integer, Integer> initial) {
            this.idToIndex = initial;
        }

        public int getId(int original) {
            return idToIndex.computeIfAbsent(original, k -> ID++);
        }

        public void write(CheckedOutputStream os) throws IOException {
            os.writeInt(idToIndex.size());

            int[] ids = new int[idToIndex.size()];

            for (Map.Entry<Integer, Integer> entry : idToIndex.entrySet()) {
                ids[entry.getValue()] = entry.getKey();
            }

            for (int id : ids) {
                os.writeInt(id);
            }
        }

    }

}
