package com.ia.api.task.repository;

import java.util.UUID;

public interface TaskStatsProjection {
    UUID getTaskDefinitionId();
    String getTitle();
    String getIcon();
    long getTotalCount();
    long getDoneCount();
    long getMissedCount();
    long getSkippedCount();
    long getPlannedCount();
}
