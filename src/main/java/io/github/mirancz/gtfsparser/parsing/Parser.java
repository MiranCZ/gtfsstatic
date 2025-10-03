package io.github.mirancz.gtfsparser.parsing;

import io.github.mirancz.gtfsparser.util.Pair;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Parser {

    private final Map<String, Pair<String, Transformer>> transformers = new HashMap<>();
    private final Map<String, Consumer<InputStream>> readers = new HashMap<>();

    public final void onFile(String name, InputStream input, Function<String, DataOutputStream> outputProvider) {
        try(WrappedOutputProvider wrapped = new WrappedOutputProvider(outputProvider)) {
            onFileInternal(name, input, wrapped::generate);

            if (transformers.containsKey(name)) {
                var pair = transformers.get(name);
                DataOutputStream os = wrapped.generate(pair.left());
                pair.right().call(input, os);
            }
            if (readers.containsKey(name)) {
                readers.get(name).accept(input);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void onFinish(Function<String, DataOutputStream> outputProvider) {
        try(WrappedOutputProvider wrapped = new WrappedOutputProvider(outputProvider)) {
            onFinishInternal(wrapped::generate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void subscribeReader(String name, Consumer<InputStream> consumer) {
        readers.put(name, consumer);
    }

    protected void subscribeTransformer(String name, String outputName, Transformer transformer) {
        transformers.put(name, new Pair<>(outputName, transformer));
    }

    protected void onFinishInternal(Function<String, DataOutputStream> outputProvider) throws Exception {
    }

    protected void onFileInternal(String name, InputStream input, Function<String, DataOutputStream> outputProvider) throws Exception {
    }

    @FunctionalInterface
    protected interface Transformer {
        void call(InputStream inputStream, DataOutputStream output) throws Exception;
    }

    private static class WrappedOutputProvider implements AutoCloseable {

        private final List<DataOutputStream> streams = new ArrayList<>();
        private final Function<String, DataOutputStream> provider;

        public WrappedOutputProvider(Function<String, DataOutputStream> provider) {
            this.provider = provider;
        }

        public DataOutputStream generate(String s) {
            DataOutputStream generated = provider.apply(s);

            streams.add(generated);

            return generated;
        }


        @Override
        public void close() throws Exception {
            for (DataOutputStream stream : streams) {
                stream.close();
            }
        }
    }

}
