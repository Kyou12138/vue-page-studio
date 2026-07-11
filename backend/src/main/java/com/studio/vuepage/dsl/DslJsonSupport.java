package com.studio.vuepage.dsl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studio.vuepage.dsl.model.PageDsl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * PageDSL 的 JSON 读写辅助。
 */
public final class DslJsonSupport {

    private static final ObjectMapper MAPPER = createMapper();

    private DslJsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * 从 classpath 读取 PageDSL，例如 {@code fixtures/dsl/list-user.json}。
     */
    public static PageDsl readClasspath(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = DslJsonSupport.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("classpath resource not found: " + path);
            }
            return MAPPER.readValue(in, PageDsl.class);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read DSL from classpath: " + path, e);
        }
    }

    public static PageDsl readJson(String json) {
        try {
            return MAPPER.readValue(json, PageDsl.class);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to parse DSL JSON", e);
        }
    }

    public static String writeJson(PageDsl dsl) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dsl);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write DSL JSON", e);
        }
    }
}
