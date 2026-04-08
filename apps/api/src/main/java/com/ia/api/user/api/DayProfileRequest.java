package com.ia.api.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DayProfileRequest {
    @NotBlank
    private String dayCategory;

    @NotBlank
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String wakeUpTime;

    public String getDayCategory() {
        return dayCategory;
    }

    public void setDayCategory(String dayCategory) {
        this.dayCategory = dayCategory;
    }

    public String getWakeUpTime() {
        return wakeUpTime;
    }

    public void setWakeUpTime(String wakeUpTime) {
        this.wakeUpTime = wakeUpTime;
    }
}
