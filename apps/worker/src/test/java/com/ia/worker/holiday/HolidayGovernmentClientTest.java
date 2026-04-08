package com.ia.worker.holiday;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HolidayGovernmentClientTest {
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private HolidayGovernmentClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1, 2.0, 2)
                .retryOn(org.springframework.web.client.RestClientException.class)
                .build();
        client = new HolidayGovernmentClient(restTemplate, retryTemplate, "https://calendrier.api.gouv.fr/jours-feries");
    }

    @Test
    void retriesThenSucceeds() {
        String url = "https://calendrier.api.gouv.fr/jours-feries/metropole/2026.json";
        server.expect(times(2), requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withServerError());
        server.expect(requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("""
                {"2026-01-01":"Jour de l'an"}
                """, MediaType.APPLICATION_JSON));

        Map<LocalDate, String> holidays = client.fetchHolidays("METROPOLE", 2026);

        assertThat(holidays).containsEntry(LocalDate.of(2026, 1, 1), "Jour de l'an");
        server.verify();
    }

    @Test
    void failsAfterMaxRetries() {
        String url = "https://calendrier.api.gouv.fr/jours-feries/metropole/2026.json";
        server.expect(times(3), requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchHolidays("METROPOLE", 2026)).isInstanceOf(Exception.class);
        server.verify();
    }
}
