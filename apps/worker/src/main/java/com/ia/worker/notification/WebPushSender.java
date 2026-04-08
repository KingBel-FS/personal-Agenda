package com.ia.worker.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;

/**
 * Sends Web Push notifications using VAPID authentication + RFC 8291 payload encryption.
 * Uses the web-push library for proper ECDH + AES-128-GCM encryption.
 */
@Component
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final PushService pushService;
    private final ObjectMapper objectMapper;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public WebPushSender(
            @Value("${app.vapid.public-key}") String vapidPublicKey,
            @Value("${app.vapid.private-key}") String vapidPrivateKey,
            @Value("${app.vapid.subject}") String vapidSubject,
            ObjectMapper objectMapper
    ) throws GeneralSecurityException {
        this.objectMapper = objectMapper;
        this.pushService = new PushService();
        this.pushService.setPublicKey(vapidPublicKey);
        this.pushService.setPrivateKey(vapidPrivateKey);
        this.pushService.setSubject(vapidSubject);
    }

    /**
     * Sends a push notification to a single subscription endpoint.
     *
     * @return true if delivery was accepted (HTTP 201), false otherwise
     */
    public boolean send(String endpoint, String authKey, String p256dhKey, PushPayload payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            Subscription subscription = new Subscription(endpoint,
                    new Subscription.Keys(p256dhKey, authKey));

            Notification notification = new Notification(subscription, payloadJson);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201 || statusCode == 200) {
                return true;
            }

            if (statusCode == 404 || statusCode == 410) {
                log.warn("webpush.send: subscription gone ({}), endpoint={}", statusCode, endpoint);
            } else {
                log.warn("webpush.send: unexpected status={}, reason={}",
                        statusCode, response.getStatusLine().getReasonPhrase());
            }
            return false;

        } catch (Exception e) {
            log.error("webpush.send: failed for endpoint={}", endpoint, e);
            return false;
        }
    }

    /**
     * Sends a push notification and returns the HTTP status code.
     * Returns -1 on exception.
     */
    public int sendWithStatus(String endpoint, String authKey, String p256dhKey, PushPayload payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            Subscription subscription = new Subscription(endpoint,
                    new Subscription.Keys(p256dhKey, authKey));

            Notification notification = new Notification(subscription, payloadJson);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200 && statusCode != 201) {
                log.warn("webpush.sendWithStatus: status={}, endpoint={}", statusCode, endpoint);
            }
            return statusCode;

        } catch (Exception e) {
            log.error("webpush.sendWithStatus: failed for endpoint={}", endpoint, e);
            return -1;
        }
    }

    public record PushPayload(
            String title,
            String body,
            String iconUrl,
            String badgeUrl,
            String imageUrl,
            String notificationJobId,
            String taskOccurrenceId,
            String actionUrl,
            boolean requireInteraction,
            List<PushAction> actions
    ) {}

    public record PushAction(String action, String title) {}
}
