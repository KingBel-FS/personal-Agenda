package com.ia.api.common.api;

public record ApiResponse<T>(T data, Object meta) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }
}
