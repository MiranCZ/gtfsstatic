package io.github.mirancz.gtfsparser.parsing;

import java.io.*;

public abstract class Parser {

    public final void parse(InputStream input, DataOutputStream output) {
        try {
            parseAndWrite(input, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void parseAndWrite(InputStream input, DataOutputStream output) throws Exception;

}
