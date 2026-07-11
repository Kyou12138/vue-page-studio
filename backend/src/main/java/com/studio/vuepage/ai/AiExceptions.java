package com.studio.vuepage.ai;

import java.util.Collections;
import java.util.List;

/**
 * AI 相关业务异常（由 {@code GlobalExceptionHandler} 映射 HTTP 状态与错误码）。
 */
public final class AiExceptions {

    private AiExceptions() {
    }

    /** 未配置 LLM API Key 等，HTTP 503 + CONFIG_MISSING */
    public static final class ConfigMissingException extends RuntimeException {
        public ConfigMissingException(String message) {
            super(message);
        }
    }

    /** 模型输出无法解析/校验，HTTP 502 + LLM_BAD_JSON */
    public static final class LlmBadJsonException extends RuntimeException {
        private final List<String> details;

        public LlmBadJsonException(String message) {
            this(message, List.of());
        }

        public LlmBadJsonException(String message, List<String> details) {
            super(message);
            this.details = details == null
                    ? List.of()
                    : Collections.unmodifiableList(List.copyOf(details));
        }

        public List<String> getDetails() {
            return details;
        }
    }

    /** 模型调用超时，HTTP 502 + LLM_TIMEOUT */
    public static final class LlmTimeoutException extends RuntimeException {
        public LlmTimeoutException(String message) {
            super(message);
        }

        public LlmTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
