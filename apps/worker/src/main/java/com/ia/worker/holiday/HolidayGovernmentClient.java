package com.ia.worker.holiday;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HolidayGovernmentClient {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String baseUrl;

    public HolidayGovernmentClient(
            RestTemplate restTemplate,
            RetryTemplate retryTemplate,
            @Value("${holiday.api.base-url:https://calendrier.api.gouv.fr/jours-feries}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<LocalDate, String> fetchHolidays(String geographicZone, int year) {
        return retryTemplate.execute(context -> {
            String normalizedZone = geographicZone.equals("ALSACE_LORRAINE") ? "alsace-moselle" : "metropole";
            String url = baseUrl + "/" + normalizedZone + "/" + year + ".json";
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new RestClientException("Holiday API returned empty payload");
            }
            Map<LocalDate, String> holidays = new LinkedHashMap<>();
            response.forEach((date, name) -> holidays.put(LocalDate.parse(date), name));
            return holidays;
        });
    }
}
