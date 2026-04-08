package com.ia.api.task.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteTaskOccurrenceRequest(@NotNull TaskMutationScope scope, UUID taskTimeSlotId) {
}
