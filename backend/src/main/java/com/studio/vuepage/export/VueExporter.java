package com.studio.vuepage.export;

import com.studio.vuepage.dsl.DslValidationException;
import com.studio.vuepage.dsl.DslValidator;
import com.studio.vuepage.dsl.model.PageDsl;

import java.util.List;

/**
 * PageDSL → Vue 3 SFC 确定性导出器（唯一导出权威）。
 */
public class VueExporter {

    private final DslValidator validator;

    public VueExporter() {
        this(new DslValidator());
    }

    public VueExporter(DslValidator validator) {
        this.validator = validator != null ? validator : new DslValidator();
    }

    /**
     * 导出单文件 Vue SFC 字符串。DSL 非法时抛出 {@link DslValidationException}。
     */
    public String export(PageDsl dsl) {
        List<String> errors = validator.validate(dsl);
        if (!errors.isEmpty()) {
            throw new DslValidationException(errors);
        }
        String style = dsl.getStyle();
        if ("plain".equals(style)) {
            return new PlainSfcBuilder().build(dsl);
        }
        // element-plus（及校验通过后的默认）
        return new ElementPlusSfcBuilder().build(dsl);
    }
}
