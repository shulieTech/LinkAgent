package com.shulie.instrument.simulator.agent.core.util;


import java.io.*;
import java.nio.charset.Charset;

public class IOUtils {

    public static final int EOF = -1;
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final ThreadLocal<char[]> SKIP_CHAR_BUFFER = new ThreadLocal<char[]>(){
        @Override
        protected char[] initialValue() {
            return IOUtils.charArray();
        }
    };

    public static String toString(final InputStream input) throws IOException {
        return toString(input, Charset.defaultCharset());
    }

    public static String toString(final InputStream input, final Charset charset) throws IOException {
        try (final StringBuilderWriter sw = new StringBuilderWriter()) {
            copy(input, sw, charset);
            return sw.toString();
        }
    }

    public static void copy(final InputStream input, final Writer writer, final Charset inputCharset)
            throws IOException {
        final InputStreamReader reader = new InputStreamReader(input, inputCharset);
        copy(reader, writer);
    }

    public static int copy(final Reader reader, final Writer writer) throws IOException {
        final long count = copyLarge(reader, writer);
        if (count > Integer.MAX_VALUE) {
            return EOF;
        }
        return (int) count;
    }

    public static long copyLarge(final Reader reader, final Writer writer) throws IOException {
        return copyLarge(reader, writer, getCharArray());
    }

    static char[] getCharArray() {
        return SKIP_CHAR_BUFFER.get();
    }

    public static long copyLarge(final Reader reader, final Writer writer, final char[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = reader.read(buffer))) {
            writer.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private static char[] charArray() {
        return charArray(DEFAULT_BUFFER_SIZE);
    }

    private static char[] charArray(final int size) {
        return new char[size];
    }

    public static void closeQuietly(final InputStream input) {
        closeQuietly((Closeable) input);
    }

    public static void closeQuietly(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
            }
        }
    }

}
