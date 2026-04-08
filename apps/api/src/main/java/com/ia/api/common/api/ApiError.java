package com.ia.api.common.api;

public record ApiError(String code, String message, Object details) {
}
