package com.studio.vuepage.api;

import com.studio.vuepage.ai.AiExceptions.ConfigMissingException;
import com.studio.vuepage.ai.AiExceptions.LlmBadJsonException;
import com.studio.vuepage.ai.AiExceptions.LlmTimeoutException;
import com.studio.vuepage.dsl.DslValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常映射为 {@link ApiResponse}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DslValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDslValidation(DslValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("DSL_INVALID", ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(ConfigMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleConfigMissing(ConfigMissingException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("CONFIG_MISSING", ex.getMessage(), null));
    }

    @ExceptionHandler(LlmBadJsonException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmBadJson(LlmBadJsonException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("LLM_BAD_JSON", ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmTimeout(LlmTimeoutException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("LLM_TIMEOUT", ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("BAD_REQUEST", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred", null));
    }
}
