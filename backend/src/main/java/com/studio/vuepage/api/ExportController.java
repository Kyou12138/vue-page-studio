package com.studio.vuepage.api;

import com.studio.vuepage.dsl.model.PageDsl;
import com.studio.vuepage.export.VueExporter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Vue SFC 导出 HTTP 接口。
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final VueExporter vueExporter;

    public ExportController() {
        this(new VueExporter());
    }

    public ExportController(VueExporter vueExporter) {
        this.vueExporter = vueExporter != null ? vueExporter : new VueExporter();
    }

    /**
     * 将 PageDSL 导出为 Vue 3 SFC 字符串。DSL 非法时由导出器抛出校验异常 → 400。
     */
    @PostMapping("/vue")
    public ApiResponse<Map<String, String>> exportVue(@RequestBody PageDsl dsl) {
        String code = vueExporter.export(dsl);
        return ApiResponse.ok(Map.of("code", code));
    }
}
