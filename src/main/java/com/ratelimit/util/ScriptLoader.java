package com.ratelimit.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ScriptLoader {
    public static String load(String path) {
        try (InputStream is = ScriptLoader.class.getResourceAsStream(path)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Critical Failure: Could not load Lua script at " + path, e);
        }
    }
}

