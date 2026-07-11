package com.studio.vuepage.api;

import java.util.List;

public record ApiResponse<T>(boolean success, T data, String code, String message, List<String> details) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, List<String> details) {
        return new ApiResponse<>(false, null, code, message, details);
    }
}
