package com.shulie.instrument.simulator.agent.core.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

public class FileUtils {

    public static String readFileToString(final File file, final String charsetName) throws IOException {
        return readFileToString(file, toCharset(charsetName));
    }

    public static String readFileToString(final File file, final Charset charsetName) throws IOException {
        try (InputStream inputStream = openInputStream(file)) {
            return IOUtils.toString(inputStream, charsetName);
        }
    }

    public static FileInputStream openInputStream(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        return new FileInputStream(file);
    }

    public static Charset toCharset(final String charsetName) throws UnsupportedCharsetException {
        return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

}
