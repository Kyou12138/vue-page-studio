package com.studio.vuepage.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEscaperTest {

    @Test
    void escapeText_escapesScriptAndSpecialChars() {
        assertThat(HtmlEscaper.escapeText("<script>")).isEqualTo("&lt;script&gt;");
        assertThat(HtmlEscaper.escapeText("a & b")).isEqualTo("a &amp; b");
        assertThat(HtmlEscaper.escapeText("say \"hi\"")).isEqualTo("say &quot;hi&quot;");
        assertThat(HtmlEscaper.escapeText(null)).isEqualTo("");
    }

    @Test
    void escapeAttr_escapesQuotes() {
        assertThat(HtmlEscaper.escapeAttr("a\"b")).contains("&quot;");
        assertThat(HtmlEscaper.escapeAttr("<img onerror=alert(1)>"))
                .doesNotContain("<img")
                .contains("&lt;img");
        assertThat(HtmlEscaper.escapeAttr(null)).isEqualTo("");
    }

    @Test
    void escapeText_preservesSafeChinese() {
        assertThat(HtmlEscaper.escapeText("姓名")).isEqualTo("姓名");
    }
}
