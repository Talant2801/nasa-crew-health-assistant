package com.spaceapps.crewhealth.api.dto;

import com.spaceapps.crewhealth.model.Environment;
import com.spaceapps.crewhealth.model.SpaceWeather;

public record MissionDto(
        Environment env,
        String label,
        int missionDay,
        int missionLengthDays,
        double commsDelaySeconds,
        String escalation,
        double doseRateMSvPerDay,
        String doseSource,
        SpaceWeather spaceWeather) {
}
