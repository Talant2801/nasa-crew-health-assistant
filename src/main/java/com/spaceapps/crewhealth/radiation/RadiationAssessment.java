package com.spaceapps.crewhealth.radiation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spaceapps.crewhealth.model.Status;

/** Whole-crew RADIATION module. actionCardId is null when GREEN. */
public record RadiationAssessment(
        String bodySystem,
        Status status,
        double currentDoseMSv,
        double projectedDoseMSv,
        double careerLimitMSv,
        double speLimitMSv,
        boolean activeSpe,
        String model,
        String summary,
        @JsonIgnore String actionCardId) {
}
