package com.spaceapps.crewhealth.api.dto;

import com.spaceapps.crewhealth.engine.IndicatorAssessment;

import java.util.List;

public record ReadingsDto(
        String indicator,
        String unit,
        double baseline,
        double bandLow,
        double bandHigh,
        Double absoluteLimit,
        List<IndicatorAssessment.Point> series) {
}
