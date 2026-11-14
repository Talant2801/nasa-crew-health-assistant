package com.spaceapps.crewhealth.model;

import java.time.LocalDate;

/** source: "SIMULATED_WEARABLE" or "MANUAL". */
public record Reading(
        String astronautId,
        String indicatorCode,
        double value,
        String unit,
        LocalDate timestamp,
        String source) {
}
