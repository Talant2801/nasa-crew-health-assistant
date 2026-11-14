package com.spaceapps.crewhealth.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spaceapps.crewhealth.model.Status;

import java.time.LocalDate;
import java.util.List;

public record IndicatorAssessment(
        String code,
        String name,
        String unit,
        Double value,
        double baseline,
        Status status,
        String trend,
        String summary,
        List<Point> last7,
        @JsonIgnore String bodySystem,
        @JsonIgnore String actionCardId) {

    public record Point(LocalDate date, double value) {
    }
}
