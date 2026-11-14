package com.spaceapps.crewhealth.model;

/** Static mission-environment profile loaded from data/missions.json. */
public record MissionContext(
        Environment env,
        String label,
        double doseRateMSvPerDay,
        int missionLengthDays,
        double commsDelaySeconds,
        String escalation,
        String doseSource) {
}
