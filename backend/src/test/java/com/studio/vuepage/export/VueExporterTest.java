package com.studio.vuepage.export;

import com.studio.vuepage.dsl.DslJsonSupport;
import com.studio.vuepage.dsl.DslValidationException;
import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VueExporterTest {

    private VueExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new VueExporter();
    }

    @Test
    void exportsListUserElementPlus() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        String sfc = exporter.export(dsl);

        assertThat(sfc).contains("<script setup>");
        assertThat(sfc).contains("<template>");
        assertThat(sfc).contains("<style scoped>");
        assertThat(sfc).contains("el-table");
        assertThat(sfc).contains("el-pagination");
        assertThat(sfc).contains("姓名");
        assertThat(sfc).containsIgnoringCase("mock");
        assertThat(sfc).contains("Element Plus");
    }

    @Test
    void escapesHostileTitle() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        dsl.setTitle("<img onerror=alert(1)>");
        DslNode header = dsl.getChildren().get(0);
        Map<String, Object> props = new HashMap<>(header.getProps());
        props.put("title", "<img onerror=alert(1)>");
        header.setProps(props);

        String sfc = exporter.export(dsl);

        assertThat(sfc).doesNotContain("<img onerror");
        assertThat(sfc).contains("&lt;img");
    }

    @Test
    void exportsPlainStyle() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        dsl.setStyle("plain");
        String sfc = exporter.export(dsl);

        assertThat(sfc).doesNotContain("el-table");
        assertThat(sfc).doesNotContain("el-form");
        assertThat(sfc).contains("<table");
        assertThat(sfc).contains("<script setup>");
        assertThat(sfc).contains("姓名");
    }

    @Test
    void rejectsInvalidDsl() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        dsl.setVersion("2");

        assertThatThrownBy(() -> exporter.export(dsl))
                .isInstanceOf(DslValidationException.class)
                .satisfies(ex -> {
                    DslValidationException dve = (DslValidationException) ex;
                    assertThat(dve.getDetails()).isNotEmpty();
                    assertThat(dve.getDetails().stream().anyMatch(e -> e.toLowerCase().contains("version")))
                            .isTrue();
                });
    }

    @Test
    void exportsFormUserElementPlus() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/form-user.json");
        String sfc = exporter.export(dsl);

        assertThat(sfc).contains("el-form");
        assertThat(sfc).contains("el-input");
        assertThat(sfc).contains("el-button");
        assertThat(sfc).contains("reactive");
        assertThat(sfc).contains("console.log");
        assertThat(sfc).contains("姓名");
        assertThat(sfc).contains("邮箱");
    }

    @Test
    void exportsDashboardElementPlus() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/dashboard-min.json");
        String sfc = exporter.export(dsl);

        assertThat(sfc).contains("el-card");
        assertThat(sfc).contains("el-table");
        assertThat(sfc).contains("用户数");
        assertThat(sfc).contains("数据概览");
    }
}
