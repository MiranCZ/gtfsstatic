package io.github.mirancz.gtfsparser.util;

import java.io.DataOutputStream;
import java.io.IOException;

public record StopInfo(int stopId, int postId) {

    public void write(CheckedOutputStream os) throws IOException {
        os.writeShort(stopId);
        os.writeShort(postId);
    }

}
