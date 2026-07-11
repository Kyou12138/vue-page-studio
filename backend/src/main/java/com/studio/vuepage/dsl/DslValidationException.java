package com.studio.vuepage.dsl;

import java.util.Collections;
import java.util.List;

/**
 * DSL 校验失败异常，携带明细错误列表。
 */
public class DslValidationException extends RuntimeException {

    private final List<String> details;

    public DslValidationException(List<String> details) {
        super(buildMessage(details));
        this.details = details == null
                ? List.of()
                : Collections.unmodifiableList(List.copyOf(details));
    }

    public DslValidationException(String message, List<String> details) {
        super(message);
        this.details = details == null
                ? List.of()
                : Collections.unmodifiableList(List.copyOf(details));
    }

    public List<String> getDetails() {
        return details;
    }

    private static String buildMessage(List<String> details) {
        if (details == null || details.isEmpty()) {
            return "DSL validation failed";
        }
        return "DSL validation failed: " + String.join("; ", details);
    }
}
