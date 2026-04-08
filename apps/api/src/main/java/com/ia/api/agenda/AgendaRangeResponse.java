package com.ia.api.agenda;

import java.time.LocalDate;
import java.util.List;

public record AgendaRangeResponse(
        String view,
        LocalDate anchorDate,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        List<AgendaDaySummary> days
) {
}
