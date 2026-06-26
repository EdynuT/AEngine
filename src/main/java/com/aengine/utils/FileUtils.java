package com.aengine.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    private FileUtils() {}

    public static String readResource(String path) {
        try (InputStream is = FileUtils.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }


    // Reads a shader resource and injects the dynamic max texture slots count into the source text.

    public static String readAndInjectResource(String path, int maxSlots) {
        String source = readResource(path);
        return source.replace("#MAX_TEXTURE_SLOTS#", String.valueOf(maxSlots));
    }
}
