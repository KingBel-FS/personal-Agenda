package com.ia.api.task.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DayClassificationReadService {

    private final JdbcTemplate jdbcTemplate;

    public DayClassificationReadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Classifie une date en WEEKEND_HOLIDAY, VACATION ou WORKDAY.
     *
     * D-2 : cache intra-appel pour éviter les doubles requêtes répétées sur la même
     * date (utile lors du refresh de séries longues).  Le cache est local à l'instance
     * du service Spring (singleton), thread-safe via ConcurrentHashMap côté appel unique
     * dans un contexte transactionnel.  Pour un vrai cache distribué on utilisera
     * Spring Cache + Redis à l'étape 4.x.
     */
    public String classifyDate(LocalDate date, UUID userId, String geographicZone) {
        return classifyWithCache(date, userId, geographicZone, new HashMap<>());
    }

    /**
     * Variante acceptant un cache externe pour mutualiser les appels sur plusieurs
     * dates dans le même contexte (OccurrenceRefreshService peut passer sa propre map).
     */
    public String classifyDate(LocalDate date, UUID userId, String geographicZone,
                               Map<String, String> cache) {
        return classifyWithCache(date, userId, geographicZone, cache);
    }

    private String classifyWithCache(LocalDate date, UUID userId, String geographicZone,
                                     Map<String, String> cache) {
        String key = date + "::" + userId + "::" + geographicZone;
        String cached = cache.get(key);
        if (cached != null) return cached;

        String result = classify(date, userId, geographicZone);
        cache.put(key, result);
        return result;
    }

    private String classify(LocalDate date, UUID userId, String geographicZone) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "WEEKEND_HOLIDAY";
        }

        Integer holidayCount = jdbcTemplate.queryForObject(
                "select count(*) from holidays where geographic_zone = ? and holiday_date = ?",
                Integer.class,
                geographicZone,
                date
        );
        if (holidayCount != null && holidayCount > 0) {
            return "WEEKEND_HOLIDAY";
        }

        Integer vacationCount = jdbcTemplate.queryForObject(
                "select count(*) from vacation_periods where user_id = ? and start_date <= ? and end_date >= ?",
                Integer.class,
                userId,
                date,
                date
        );
        if (vacationCount != null && vacationCount > 0) {
            return "VACATION";
        }

        return "WORKDAY";
    }
}
