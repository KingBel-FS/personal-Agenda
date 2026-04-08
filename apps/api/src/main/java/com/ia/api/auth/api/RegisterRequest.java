package com.ia.api.auth.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    private String pseudo;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotBlank
    private String geographicZone;

    private MultipartFile profilePhoto;

    @NotNull
    private Boolean consentAccepted;

    @NotBlank
    private String legalVersion;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGeographicZone() {
        return geographicZone;
    }

    public void setGeographicZone(String geographicZone) {
        this.geographicZone = geographicZone;
    }

    public MultipartFile getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(MultipartFile profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public Boolean getConsentAccepted() {
        return consentAccepted;
    }

    public void setConsentAccepted(Boolean consentAccepted) {
        this.consentAccepted = consentAccepted;
    }

    public String getLegalVersion() {
        return legalVersion;
    }

    public void setLegalVersion(String legalVersion) {
        this.legalVersion = legalVersion;
    }

    @AssertTrue(message = "Consent must be accepted")
    public boolean isConsentAcceptedValid() {
        return Boolean.TRUE.equals(consentAccepted);
    }
}
