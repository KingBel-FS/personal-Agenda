package com.ia.api.user.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class UpdateProfileRequest {
    @NotBlank
    @Size(max = 100)
    private String pseudo;

    @NotBlank
    private String geographicZone;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$")
    private String timezoneName;

    private Boolean zoneChangeConfirmed;

    @Valid
    @NotEmpty
    @Size(min = 3, max = 3)
    private List<DayProfileRequest> dayProfiles;

    private MultipartFile profilePhoto;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getGeographicZone() {
        return geographicZone;
    }

    public void setGeographicZone(String geographicZone) {
        this.geographicZone = geographicZone;
    }

    public String getTimezoneName() {
        return timezoneName;
    }

    public void setTimezoneName(String timezoneName) {
        this.timezoneName = timezoneName;
    }

    public Boolean getZoneChangeConfirmed() {
        return zoneChangeConfirmed;
    }

    public void setZoneChangeConfirmed(Boolean zoneChangeConfirmed) {
        this.zoneChangeConfirmed = zoneChangeConfirmed;
    }

    public List<DayProfileRequest> getDayProfiles() {
        return dayProfiles;
    }

    public void setDayProfiles(List<DayProfileRequest> dayProfiles) {
        this.dayProfiles = dayProfiles;
    }

    public MultipartFile getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(MultipartFile profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
}
