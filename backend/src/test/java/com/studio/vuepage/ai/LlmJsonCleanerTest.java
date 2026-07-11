package com.studio.vuepage.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmJsonCleanerTest {

    private LlmJsonCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new LlmJsonCleaner();
    }

    @Test
    void stripsJsonMarkdownFence() {
        String raw = "```json\n{\"version\":\"1\"}\n```";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"version\":\"1\"}");
    }

    @Test
    void stripsPlainMarkdownFence() {
        String raw = "```\n{\"a\":1}\n```";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsFenceWithLanguageAndTrailingText() {
        String raw = "```JSON\n{\"x\":true}\n```\nHere is an explanation.";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"x\":true}");
    }

    @Test
    void pureJsonUnchanged() {
        String raw = "{\"version\":\"1\",\"pageType\":\"list\"}";
        assertThat(cleaner.clean(raw)).isEqualTo(raw);
    }

    @Test
    void trimsWhitespace() {
        assertThat(cleaner.clean("  \n  {\"k\":1}  \n")).isEqualTo("{\"k\":1}");
    }

    @Test
    void nullAndEmpty() {
        assertThat(cleaner.clean(null)).isEqualTo("");
        assertThat(cleaner.clean("")).isEqualTo("");
        assertThat(cleaner.clean("   ")).isEqualTo("");
    }

    @Test
    void stripsBom() {
        String raw = "\uFEFF{\"version\":\"1\"}";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"version\":\"1\"}");
    }

    @Test
    void extractsJsonWhenPrefixedWithProse() {
        String raw = "Sure, here is the DSL:\n{\"version\":\"1\",\"title\":\"t\"}\nThanks!";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"version\":\"1\",\"title\":\"t\"}");
    }

    @Test
    void singleLineFence() {
        String raw = "```json{\"version\":\"1\"}```";
        assertThat(cleaner.clean(raw)).isEqualTo("{\"version\":\"1\"}");
    }

    @Test
    void arrayJson() {
        String raw = "```\n[1,2,3]\n```";
        assertThat(cleaner.clean(raw)).isEqualTo("[1,2,3]");
    }
}
