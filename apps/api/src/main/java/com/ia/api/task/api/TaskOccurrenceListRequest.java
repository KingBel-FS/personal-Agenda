package com.ia.api.task.api;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class TaskOccurrenceListRequest {

    private Integer page;
    private Integer size;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate occurrenceDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate occurrenceDateTo;

    private String search;
    private String taskKind;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate selectedDate;

    private String timeMode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime fixedTime;

    private Integer wakeUpOffsetMinutes;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public LocalDate getOccurrenceDateFrom() { return occurrenceDateFrom; }
    public void setOccurrenceDateFrom(LocalDate occurrenceDateFrom) { this.occurrenceDateFrom = occurrenceDateFrom; }

    public LocalDate getOccurrenceDateTo() { return occurrenceDateTo; }
    public void setOccurrenceDateTo(LocalDate occurrenceDateTo) { this.occurrenceDateTo = occurrenceDateTo; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public String getTaskKind() { return taskKind; }
    public void setTaskKind(String taskKind) { this.taskKind = taskKind; }

    public LocalDate getSelectedDate() { return selectedDate; }
    public void setSelectedDate(LocalDate selectedDate) { this.selectedDate = selectedDate; }

    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }

    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }

    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }
}
