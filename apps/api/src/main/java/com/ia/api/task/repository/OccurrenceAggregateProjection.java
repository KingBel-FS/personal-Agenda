package com.ia.api.task.repository;

public interface OccurrenceAggregateProjection {
    long getTotalCount();
    long getDoneCount();
    long getMissedCount();
    long getSkippedCount();
    long getPlannedCount();
    long getDistinctTaskCount();
}
