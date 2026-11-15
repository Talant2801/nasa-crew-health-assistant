package com.spaceapps.crewhealth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/** Rule-based action card loaded from data/action-cards.json. sourceUrl links the cited NASA document. */
public record ActionCard(
        String id,
        String title,
        String what,
        String whySpace,
        List<String> doNow,
        String escalateLevel,
        String source,
        String sourceUrl) {

    @JsonIgnore
    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(escalateLevel);
    }
}
