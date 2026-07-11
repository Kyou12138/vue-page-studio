package com.studio.vuepage.api;

import com.studio.vuepage.dsl.DslValidationException;
import com.studio.vuepage.dsl.DslValidator;
import com.studio.vuepage.dsl.model.PageDsl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DSL 校验 HTTP 接口。
 */
@RestController
@RequestMapping("/api/dsl")
public class DslController {

    private final DslValidator validator;

    public DslController() {
        this(new DslValidator());
    }

    public DslController(DslValidator validator) {
        this.validator = validator != null ? validator : new DslValidator();
    }

    /**
     * 校验 PageDSL。合法返回 200；非法抛出 {@link DslValidationException}，由全局处理器映射为 400。
     */
    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validate(@RequestBody PageDsl dsl) {
        List<String> errors = validator.validate(dsl);
        if (!errors.isEmpty()) {
            throw new DslValidationException(errors);
        }
        return ApiResponse.ok(Map.of("valid", true));
    }
}
