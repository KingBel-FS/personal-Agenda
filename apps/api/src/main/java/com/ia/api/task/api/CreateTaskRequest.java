package com.ia.api.task.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CreateTaskRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 50)
    private String icon;

    private String description;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotEmpty
    private List<String> dayCategories;

    @NotBlank
    private String timeMode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime fixedTime;

    private Integer wakeUpOffsetMinutes;

    private String recurrenceType; // WEEKLY or MONTHLY (null for ONE_TIME)

    private List<Integer> daysOfWeek; // 1=Mon..7=Sun for WEEKLY

    private Integer dayOfMonth; // 1-31 for MONTHLY

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    private MultipartFile photo;

    /** Créneaux intra-journée additionnels (optionnel). Si vide, comportement mono-slot existant. */
    private List<TimeSlotRequest> timeSlots;

    @AssertTrue(message = "daysOfWeek est requis quand recurrenceType est WEEKLY")
    public boolean isDaysOfWeekValid() {
        if ("WEEKLY".equals(recurrenceType)) {
            return daysOfWeek != null && !daysOfWeek.isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "dayOfMonth est requis quand recurrenceType est MONTHLY")
    public boolean isDayOfMonthValid() {
        if ("MONTHLY".equals(recurrenceType)) {
            return dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31;
        }
        return true;
    }

    @AssertTrue(message = "fixedTime est requis quand timeMode est FIXED")
    public boolean isFixedTimeValid() {
        if ("FIXED".equals(timeMode)) {
            return fixedTime != null;
        }
        return true;
    }

    @AssertTrue(message = "wakeUpOffsetMinutes est requis quand timeMode est WAKE_UP_OFFSET")
    public boolean isWakeUpOffsetValid() {
        if ("WAKE_UP_OFFSET".equals(timeMode)) {
            return wakeUpOffsetMinutes != null;
        }
        return true;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public List<String> getDayCategories() { return dayCategories; }
    public void setDayCategories(List<String> dayCategories) { this.dayCategories = dayCategories; }

    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }

    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }

    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public MultipartFile getPhoto() { return photo; }
    public void setPhoto(MultipartFile photo) { this.photo = photo; }

    public List<TimeSlotRequest> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotRequest> timeSlots) { this.timeSlots = timeSlots; }
}
