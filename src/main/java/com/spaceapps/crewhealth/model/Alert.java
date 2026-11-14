package com.spaceapps.crewhealth.model;

/** Derived from the current assessment; never stored. crewId is null for crew-wide alerts. */
public record Alert(
        String id,
        String crewId,
        String indicator,
        Status severity,
        String summary,
        ActionCard actionCard,
        String escalationText,
        boolean acknowledged) {
}
