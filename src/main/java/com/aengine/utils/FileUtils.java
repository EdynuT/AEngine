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
}
