package com.ia.api.task.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

/**
 * Un créneau horaire intra-journée pour une tâche.
 * timeMode : FIXED | WAKE_UP_OFFSET | AFTER_PREVIOUS
 * Classe (non-record) pour que @ModelAttribute fasse le binding setter-based.
 */
public class TimeSlotRequest {

    @NotBlank
    private String timeMode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime fixedTime;

    @Min(0) @Max(720)
    private Integer wakeUpOffsetMinutes;

    @Min(0) @Max(720)
    private Integer afterPreviousMinutes;

    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }

    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }

    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }

    public Integer getAfterPreviousMinutes() { return afterPreviousMinutes; }
    public void setAfterPreviousMinutes(Integer afterPreviousMinutes) { this.afterPreviousMinutes = afterPreviousMinutes; }
}
