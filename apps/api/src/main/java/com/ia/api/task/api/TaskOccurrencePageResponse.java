package com.ia.api.task.api;

import java.util.List;

public record TaskOccurrencePageResponse(
        List<TaskOccurrenceResponse> items,
        int page,
        int size,
        int totalElements,
        int totalPages
) {}
