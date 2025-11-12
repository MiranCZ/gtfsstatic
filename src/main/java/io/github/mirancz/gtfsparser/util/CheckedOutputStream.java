package io.github.mirancz.gtfsparser.util;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CheckedOutputStream implements AutoCloseable{

    private final DataOutputStream os;

    public CheckedOutputStream(OutputStream os) {
        this(new DataOutputStream(os));
    }

    public CheckedOutputStream(DataOutputStream os) {
        this.os = os;
    }

    public void writeLong(long v) throws IOException {
        os.writeLong(v);
    }

    public void writeInt(int v) throws IOException {
        os.writeInt(v);
    }

    public void writeShort(int v) throws IOException {
        if (v > Short.MAX_VALUE || v < Short.MIN_VALUE) throw new IllegalStateException();

        os.writeShort(v);
    }

    public void writeByte(int v) throws IOException {
        if (v > Byte.MAX_VALUE || v < Byte.MIN_VALUE) throw new IllegalStateException();

        os.writeByte(v);
    }

    public void write(int v) throws IOException {
        if (v < 0 || v > 0xFF) throw new IllegalStateException();

        os.write(v);
    }

    public void writeDouble(double v) throws IOException {
        os.writeDouble(v);
    }

    public void writeBoolean(boolean v) throws IOException {
        os.writeBoolean(v);
    }

    public void write(byte[] bytes) throws IOException {
        os.write(bytes);
    }

    public void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        os.writeInt(bytes.length);
        os.write(bytes);    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
