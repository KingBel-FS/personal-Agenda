package com.ia.api.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class SignedAssetUrlService {
    private final String baseUrl;
    private final byte[] signingSecret;

    public SignedAssetUrlService(
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.asset-signing-secret:${app.jwt.secret}}") String signingSecret
    ) {
        this.baseUrl = baseUrl;
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String signProfilePhotoUrl(String storageKey) {
        long expiresAt = Instant.now().plusSeconds(900).getEpochSecond();
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/profile/photo")
                .queryParam("key", storageKey)
                .queryParam("expiresAt", expiresAt)
                .queryParam("signature", sign(storageKey, expiresAt))
                .toUriString();
    }

    public boolean isValid(String storageKey, long expiresAt, String signature) {
        return expiresAt >= Instant.now().getEpochSecond() && sign(storageKey, expiresAt).equals(signature);
    }

    private String sign(String storageKey, long expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
            byte[] signed = mac.doFinal((storageKey + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign asset URL", exception);
        }
    }
}
